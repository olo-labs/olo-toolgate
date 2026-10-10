# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
# Render the existing OLO mark in connection colors; cache icons for the tray lifetime.
function Initialize-ToolGateIconRenderer {
    Add-Type -AssemblyName System.Drawing
    if (-not ('Olo.ToolGate.StatusIcon' -as [type])) {
        Add-Type -ReferencedAssemblies System.Drawing -TypeDefinition @'
using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.Runtime.InteropServices;
namespace Olo.ToolGate {
    public static class StatusIcon {
        [DllImport("user32.dll")] private static extern bool DestroyIcon(IntPtr icon);
        public static Icon Create(string path, bool connected) {
            Color color = connected ? Color.FromArgb(22, 163, 74) : Color.FromArgb(220, 38, 38);
            return Render(path, color);
        }
        public static Icon CreateWindow(string path) {
            return Render(path, Color.FromArgb(15, 23, 42));
        }
        private static Icon Render(string path, Color color) {
            using (Image logo = Image.FromFile(path))
            using (Bitmap bitmap = new Bitmap(32, 32, PixelFormat.Format32bppArgb))
            using (Graphics graphics = Graphics.FromImage(bitmap))
            using (ImageAttributes attributes = new ImageAttributes()) {
                // Match the console favicon: preserve the source geometry and use inverse luminance as alpha.
                attributes.SetColorMatrix(new ColorMatrix(new float[][] {
                    new float[] {0, 0, 0, -0.2126f, 0},
                    new float[] {0, 0, 0, -0.7152f, 0},
                    new float[] {0, 0, 0, -0.0722f, 0},
                    new float[] {0, 0, 0, 0, 0},
                    new float[] {color.R / 255f, color.G / 255f, color.B / 255f, 1, 1}
                }));
                graphics.Clear(Color.Transparent);
                graphics.InterpolationMode = InterpolationMode.HighQualityBicubic;
                graphics.DrawImage(logo, new Rectangle(1, 1, 30, 30), 144, 12, 750, 750, GraphicsUnit.Pixel, attributes);
                IntPtr handle = bitmap.GetHicon();
                try { return (Icon)Icon.FromHandle(handle).Clone(); }
                finally { DestroyIcon(handle); }
            }
        }
    }
}
'@
    }
}
function New-ToolGateStatusIcon([string]$LogoPath, [bool]$Connected) {
    Initialize-ToolGateIconRenderer
    return [Olo.ToolGate.StatusIcon]::Create($LogoPath, $Connected)
}
function New-ToolGateBrandIcon([string]$LogoPath) {
    Initialize-ToolGateIconRenderer
    return [Olo.ToolGate.StatusIcon]::CreateWindow($LogoPath)
}
