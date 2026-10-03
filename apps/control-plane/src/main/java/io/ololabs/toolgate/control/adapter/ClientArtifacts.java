// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import io.ololabs.toolgate.contracts.*;
import io.ololabs.toolgate.control.application.Codec;
import io.ololabs.toolgate.control.application.Failure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.nio.file.*;
import java.nio.channels.SeekableByteChannel;
import java.util.*;

/** Public, immutable release assets. No credential, arbitrary file or user-supplied directory access. */
@ApplicationScoped
public class ClientArtifacts {
    private final Path directory;
    private final ClientDownloadManifest manifest;
    private final Map<String,ClientDownloadArtifact> artifacts;
    private final java.util.concurrent.Semaphore transfers = new java.util.concurrent.Semaphore(2);
    void start(@jakarta.enterprise.event.Observes io.quarkus.runtime.StartupEvent ignored) { }
    @Inject public ClientArtifacts(org.eclipse.microprofile.config.Config config,Codec codec){this(config.getOptionalValue("toolgate.client-downloads.directory",String.class),codec);}
    public ClientArtifacts(Optional<String> configured, Codec codec) {
        if(configured.isEmpty()){directory=null;manifest=null;artifacts=Map.of();return;}
        try {
            if(!Path.of(configured.get()).isAbsolute())throw new IllegalArgumentException();
            directory=Path.of(configured.get()).toAbsolutePath().normalize();
            for(Path path=directory;path!=null;path=path.getParent())if(Files.isSymbolicLink(path))throw new IllegalArgumentException();
            if(!Files.isDirectory(directory,LinkOption.NOFOLLOW_LINKS))throw new IllegalArgumentException();
            var document=directory.resolve("manifest.json");
            if(!Files.isRegularFile(document,LinkOption.NOFOLLOW_LINKS)||Files.size(document)>65536)throw new IllegalArgumentException();
            manifest=codec.model(Files.readString(document),ClientDownloadManifest.class);
            var index=new HashMap<String,ClientDownloadArtifact>();var targets=new HashSet<String>();var platforms=new HashSet<ClientPlatform>();
            for(var artifact:manifest.artifacts()) {
                if(index.put(artifact.filename(),artifact)!=null||!targets.add(artifact.target()))throw new IllegalArgumentException();
                var expected=artifact.target().contains("windows")?ClientPlatform.WINDOWS:artifact.target().contains("apple")?ClientPlatform.MACOS:ClientPlatform.LINUX;
                if(artifact.platform()!=expected||!artifact.filename().equals("olo-toolgate-client-"+manifest.version()+"-"+artifact.target()+(expected==ClientPlatform.WINDOWS?".zip":".tar.gz")))throw new IllegalArgumentException();
                platforms.add(artifact.platform());
                try(var file=open(artifact)){verify(file,artifact);}
            }
            if(platforms.size()!=3)throw new IllegalArgumentException();
            artifacts=Map.copyOf(index);
        }catch(Exception rejected){throw new IllegalStateException("Client release asset validation failed");}
    }
    public ClientDownloadManifest manifest(){if(manifest==null)throw unavailable();return manifest;}
    private static Failure unavailable(){return new Failure(ErrorCode.DEPENDENCY_UNAVAILABLE,503,"Client downloads are unavailable");}
    private SeekableByteChannel open(ClientDownloadArtifact artifact)throws java.io.IOException {
        if(!Files.isRegularFile(directory.resolve(artifact.filename()),LinkOption.NOFOLLOW_LINKS))throw new java.io.IOException("Invalid artifact");
        return Files.newByteChannel(directory.resolve(artifact.filename()),Set.of(StandardOpenOption.READ,LinkOption.NOFOLLOW_LINKS));
    }
    private static void verify(SeekableByteChannel file,ClientDownloadArtifact artifact)throws Exception {
        if(file.size()!=artifact.bytes())throw new IllegalArgumentException();
        var digest=java.security.MessageDigest.getInstance("SHA-256");var buffer=java.nio.ByteBuffer.allocate(65536);
        long total=0;int size;
        while((size=file.read(buffer))!=-1){total+=size;if(total>artifact.bytes())throw new IllegalArgumentException();buffer.flip();digest.update(buffer);buffer.clear();}
        if(total!=artifact.bytes()||!java.util.HexFormat.of().formatHex(digest.digest()).equals(artifact.sha256()))throw new IllegalArgumentException();
        file.position(0);
    }
    public jakarta.ws.rs.core.Response download(String filename) {
        manifest();var artifact=artifacts.get(filename);
        if(artifact==null)throw new Failure(ErrorCode.NOT_FOUND,404,"Client artifact unavailable");
        if(!transfers.tryAcquire())throw new Failure(ErrorCode.DEPENDENCY_UNAVAILABLE,429,"Client download capacity reached");
        try {
            var file=open(artifact);
            try{verify(file,artifact);}catch(Exception invalid){file.close();throw invalid;}
            jakarta.ws.rs.core.StreamingOutput output=stream->{
                try(file){var input=java.nio.channels.Channels.newInputStream(file);input.transferTo(stream);}
                finally{transfers.release();}
            };
            return jakarta.ws.rs.core.Response.ok(output,"application/octet-stream")
                .header("Content-Disposition","attachment; filename=\""+filename+"\"").header("Content-Length",artifact.bytes())
                .header("ETag","\""+artifact.sha256()+"\"").header("X-Content-Type-Options","nosniff")
                .header("Cache-Control","public, max-age=31536000, immutable").build();
        }catch(Exception rejected){transfers.release();throw unavailable();}
    }
}
