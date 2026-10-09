// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;

/** Bounded mounted material, owned and immutable to untrusted local writers. */
final class ProtectedCustody {
    private ProtectedCustody(){}
    static byte[] read(Path file,int limit)throws java.io.IOException{
        if(!file.isAbsolute()||!Files.isRegularFile(file))throw new IllegalArgumentException();
        var parent=file.getParent().toRealPath();var resolved=file.toRealPath();
        if(!resolved.startsWith(parent))throw new IllegalArgumentException();
        check(resolved);for(var directory=parent;directory!=null;directory=directory.getParent())check(directory);
        var before=Files.readAttributes(resolved,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        byte[] bytes;try(var in=Files.newInputStream(resolved,LinkOption.NOFOLLOW_LINKS)){bytes=in.readNBytes(limit+1);}
        var after=Files.readAttributes(resolved,BasicFileAttributes.class,LinkOption.NOFOLLOW_LINKS);
        if(bytes.length>limit||!Objects.equals(before.fileKey(),after.fileKey())||before.size()!=after.size()||!before.lastModifiedTime().equals(after.lastModifiedTime()))throw new IllegalArgumentException();
        return bytes;
    }
    static byte[] privateKey(Path file,int limit)throws java.io.IOException{
        byte[] bytes=read(file,limit);var resolved=file.toRealPath();var posix=Files.getFileAttributeView(resolved,PosixFileAttributeView.class);
        if(posix!=null){if(posix.readAttributes().permissions().contains(PosixFilePermission.OTHERS_READ))throw new IllegalArgumentException();}
        else {var acl=Files.getFileAttributeView(resolved,AclFileAttributeView.class);String owner=acl.getOwner().getName().toLowerCase(Locale.ROOT);String user=System.getProperty("user.name").toLowerCase(Locale.ROOT);for(var entry:acl.getAcl())if(entry.type()==AclEntryType.ALLOW&&entry.permissions().contains(AclEntryPermission.READ_DATA)){String principal=entry.principal().getName().toLowerCase(Locale.ROOT);if(!principal.equals(owner)&&!principal.equals("nt authority\\system")&&!principal.equals("builtin\\administrators")&&!principal.endsWith("\\"+user))throw new IllegalArgumentException();}}
        return bytes;
    }
    static String text(Path file,int limit)throws java.io.IOException{return new String(read(file,limit),java.nio.charset.StandardCharsets.UTF_8);}
    static String privateText(Path file,int limit)throws java.io.IOException{return new String(privateKey(file,limit),java.nio.charset.StandardCharsets.US_ASCII);}
    private static void check(Path path)throws java.io.IOException{
        if(Files.getFileAttributeView(path,PosixFileAttributeView.class)!=null){
            var attributes=Files.readAttributes(path,PosixFileAttributes.class);
            if(attributes.permissions().contains(PosixFilePermission.GROUP_WRITE)||attributes.permissions().contains(PosixFilePermission.OTHERS_WRITE))throw new IllegalArgumentException();
            // Numeric container UIDs may have no passwd entry, so Java's user.name is not an identity proof.
            if(Files.exists(Path.of("/proc/self"))&&Files.getFileStore(path).supportsFileAttributeView("unix")){
                int owner=(Integer)Files.getAttribute(path,"unix:uid");int current=(Integer)Files.getAttribute(Path.of("/proc/self"),"unix:uid");if(owner!=0&&owner!=current)throw new IllegalArgumentException();
            }else if(!attributes.owner().getName().equals("root")&&!attributes.owner().getName().equals(System.getProperty("user.name")))throw new IllegalArgumentException();
        }else{
            var view=Files.getFileAttributeView(path,AclFileAttributeView.class);if(view==null)throw new IllegalArgumentException();
            String owner=view.getOwner().getName().toLowerCase(Locale.ROOT);
            if(!trustedWindowsPrincipal(owner))throw new IllegalArgumentException();
            for(var entry:view.getAcl())if(entry.type()==AclEntryType.ALLOW){String principal=entry.principal().getName().toLowerCase(Locale.ROOT);boolean trusted=trustedWindowsPrincipal(principal);
                if(!trusted&&entry.permissions().stream().anyMatch(p->Set.of(AclEntryPermission.WRITE_DATA,AclEntryPermission.APPEND_DATA,AclEntryPermission.DELETE,AclEntryPermission.DELETE_CHILD,AclEntryPermission.WRITE_ACL,AclEntryPermission.WRITE_OWNER).contains(p)))throw new IllegalArgumentException();}
        }
    }
    private static boolean trustedWindowsPrincipal(String principal){
        String user=System.getProperty("user.name").toLowerCase(Locale.ROOT);
        return principal.equals("nt authority\\system")||principal.equals("builtin\\administrators")||principal.equals(user)||principal.endsWith("\\"+user);
    }
}
