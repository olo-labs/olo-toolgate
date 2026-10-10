// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.application;

import io.ololabs.toolgate.control.domain.Ids;
import io.ololabs.toolgate.contracts.ControlServerSettings;

/** Tenant control-plane settings. Auto-approval is off until an administrator or a startup import enables it. */
public final class ServerSettingsService {
    public static final long DEFAULT_AUTO_APPROVE_DAYS=30;
    private static final String STARTUP_ACTOR=DirectoryService.digest("startup-configuration-import");
    private final Store store;
    private final Codec codec;
    public ServerSettingsService(Store store,Codec codec){this.store=store;this.codec=codec;}
    public static ControlServerSettings defaults(){return new ControlServerSettings(1L,0L,false,DEFAULT_AUTO_APPROVE_DAYS,null);}
    public ControlServerSettings current(Store.Session tx){var document=tx.serverSettings();return document==null?defaults():codec.model(document,ControlServerSettings.class);}
    /** The enabled user that owns devices approved without a human decision, or null when auto-approval is unavailable. */
    public static String autoApprovalOwner(Store.Session tx,ControlServerSettings settings){
        if(!settings.autoApproveDevices()||settings.autoApproveOwnerUserId()==null)return null;
        var owner=tx.load().entries().get(Ids.Kind.USER.id(settings.autoApproveOwnerUserId()));
        return owner!=null&&owner.enabled()?settings.autoApproveOwnerUserId():null;
    }
    private Store.Reply reply(ControlServerSettings settings){return new Store.Reply(200,codec.json(settings),settings.revision());}
    private static void admin(DirectoryService.Actor actor){actor.requireAdmin();}
    public Store.Reply get(DirectoryService.Actor actor){admin(actor);return store.transaction(actor.tenant(),false,tx->reply(current(tx)));}
    public Store.Reply update(DirectoryService.Actor actor,String body,long expected,String key,String requestId){
        admin(actor);Ids.valid(key);Ids.valid(requestId);var requested=codec.model(body,ControlServerSettings.class);var digest=DirectoryService.digest("settings\n"+expected+"\n"+body);
        return store.transaction(actor.tenant(),true,tx->{var replay=tx.replay(actor.id(),key,digest);if(replay!=null)return replay;
            var current=current(tx);if(current.revision()!=expected)throw Failure.conflict();
            var saved=save(tx,requested,current,true);tx.audit(actor.id(),"SERVER_SETTINGS_UPDATE","settings",saved.revision(),requestId,digest);
            var result=reply(saved);tx.remember(actor.id(),key,digest,result);return result;});
    }
    /** Startup import seeds settings once; with overwrite it replaces whatever an administrator saved before the restart.
     * Directory users may be imported after the control plane starts, so the owner is checked again at each enrollment. */
    public boolean importAtStartup(Ids.TenantId tenant,ControlServerSettings requested,boolean overwrite,String requestId){
        return store.transaction(tenant,true,tx->{if(tx.serverSettings()!=null&&!overwrite)return false;
            var saved=save(tx,requested,current(tx),false);tx.audit(STARTUP_ACTOR,"SERVER_SETTINGS_IMPORT","settings",saved.revision(),requestId,DirectoryService.digest(codec.json(saved)));return true;});
    }
    private ControlServerSettings save(Store.Session tx,ControlServerSettings requested,ControlServerSettings current,boolean requireOwner){
        if(requested.formatVersion()!=1||requested.autoApproveDurationDays()<1||requested.autoApproveDurationDays()>3650)throw Failure.validation();
        var owner=requested.autoApproveOwnerUserId();
        if(owner!=null){Ids.valid(owner);var entry=requireOwner?tx.load().entries().get(Ids.Kind.USER.id(owner)):null;if(requireOwner&&(entry==null||!entry.enabled()))throw Failure.validation();}
        // Auto-approved identities need an accountable owner, exactly like a human approval.
        if(requested.autoApproveDevices()&&owner==null)throw Failure.validation();
        var saved=new ControlServerSettings(1L,current.revision()+1,requested.autoApproveDevices(),requested.autoApproveDurationDays(),owner);
        tx.saveServerSettings(saved.revision(),codec.json(saved));return saved;
    }
}
