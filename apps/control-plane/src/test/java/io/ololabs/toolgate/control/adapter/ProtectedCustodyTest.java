// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
package io.ololabs.toolgate.control.adapter;

import java.nio.file.*;
import java.nio.file.attribute.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class ProtectedCustodyTest {
    @TempDir Path temp;
    @Test void custodyRejectsRelativePathsAndNonRegularMaterial(){
        assertThrows(IllegalArgumentException.class,()->ProtectedCustody.read(Path.of("key.pem"),10));
        assertThrows(IllegalArgumentException.class,()->ProtectedCustody.read(temp,10));
    }
    @Test void projectedKeysStayWithinMountAndCannotBeWorldReadableOrWritable()throws Exception{
        // POSIX projection semantics belong to the Linux image; Windows has its own ACL verifier.
        Assumptions.assumeTrue(Files.getFileAttributeView(temp,PosixFileAttributeView.class)!=null);
        var root=Path.of(System.getProperty("user.home")).resolve("custody-test-"+java.util.UUID.randomUUID());
        Files.createDirectory(root,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
        try{
            var data=Files.createDirectory(root.resolve("..data"));var key=data.resolve("key");
            Files.writeString(key,"private");Files.setPosixFilePermissions(key,PosixFilePermissions.fromString("rw-------"));
            var projected=Files.createSymbolicLink(root.resolve("key"),Path.of("..data/key"));
            assertEquals("private",ProtectedCustody.privateText(projected,10));
            assertThrows(IllegalArgumentException.class,()->ProtectedCustody.read(projected,3));
            Files.setPosixFilePermissions(key,PosixFilePermissions.fromString("rw-r--r--"));
            assertThrows(IllegalArgumentException.class,()->ProtectedCustody.privateKey(projected,10));
            Files.setPosixFilePermissions(key,PosixFilePermissions.fromString("rw-rw----"));
            assertThrows(IllegalArgumentException.class,()->ProtectedCustody.read(projected,10));
            var outside=Files.createTempFile(root.getParent(),"custody-outside-",".key");
            try{Files.createSymbolicLink(root.resolve("escape"),outside);assertThrows(IllegalArgumentException.class,()->ProtectedCustody.read(root.resolve("escape"),10));}
            finally{Files.deleteIfExists(root.resolve("escape"));Files.delete(outside);}
        }finally{Files.deleteIfExists(root.resolve("key"));Files.deleteIfExists(root.resolve("..data/key"));Files.deleteIfExists(root.resolve("..data"));Files.delete(root);}
    }
}
