// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

// Descriptor vector tool-01; mirrors the Java processor fixtures acme/files/{FilesPackage,ChecksumTools}.java.
import { clientTool, toolPackage, z } from "@olo-labs/toolgate-sdk";

export default toolPackage({ id: "acme.files", name: "Acme files", publisher: "Acme" });

export const checksum = clientTool({
  name: "files.checksum",
  title: "Checksum a file",
  description: "SHA-256 of a file in the protected HotFolder.",
  effect: "READ_ONLY",
  input: z.object({ path: z.string().min(1).max(1024).describe("Path relative to the HotFolder") }),
  output: z.object({ sha256: z.string().regex(/^[0-9a-f]{64}$/) }),
  run: () => ({ sha256: "" }),
});
