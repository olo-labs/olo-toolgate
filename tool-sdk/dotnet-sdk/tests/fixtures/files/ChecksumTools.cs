// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0

using OloLabs.ToolGate.Sdk;

[assembly: ToolPackage("acme.files", Name = "Acme files", Publisher = "Acme")]

namespace Acme.Files;

public class ChecksumTools
{
    public record Checksum([Param(Description = "", Pattern = "^[0-9a-f]{64}$")] string Sha256);

    [ClientTool("files.checksum", Title = "Checksum a file",
        Description = "SHA-256 of a file in the protected HotFolder.", Effect = EffectSafety.ReadOnly)]
    public Checksum Compute(
        [Param(Description = "Path relative to the HotFolder", MinLength = 1, MaxLength = 1024)] string path,
        ToolContext context) => new("");
}
