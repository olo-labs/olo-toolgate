<!-- Copyright 2026 OLO Labs -->
<!-- SPDX-License-Identifier: Apache-2.0 -->
# ToolGate in 36 seconds

[Watch the video](toolgate-overview.mp4). This is a captioned, silent overview,
not a recording of a live installation. The README uses an animated GIF preview
linked to the H.264 MP4. [WebVTT captions](toolgate-overview.vtt) are also available.

## Transcript

| Time | On-screen explanation |
| --- | --- |
| 00:00–00:06 | **Control what AI can do.** Build, distribute, authorize and govern tools across users, agents and devices. |
| 00:06–00:12 | **One request. Clear boundaries.** An agent calls `hotfolder.write_text`. The Gateway checks access before a protected effect. |
| 00:12–00:18 | **Access starts with groups.** Groups hold access mappings. Default groups grant no access; new verified users start disabled. |
| 00:18–00:24 | **Allow. Ask. Block.** Allow permits an authorized action. Ask requires independent approval. Block prevents execution. |
| 00:24–00:30 | **Manage it. See what happened.** Manage groups and devices in the console. The Windows tray shows command progress and activity. |
| 00:30–00:36 | **Your first PR can be small.** Visit `github.com/olo-labs/olo-toolgate`. Choose a good first issue and help improve the docs. |

See the [initial configuration guide](../../config/initial/README.md) for group
semantics and [Windows status documentation](../client/installers.md#windows-status-activity-and-licensing)
for the device UI. Approvals do not substitute for required grants or device eligibility.

## Rebuild the media

From the repository root, with Python 3 and either Windows Segoe UI or Linux
DejaVu Sans fonts installed:

```bash
python -m pip install Pillow imageio-ffmpeg
python tools/docs/render_overview.py
```

The script renders six diagram scenes, the MP4, GIF, poster and WebVTT captions.
It uses the existing OLO logo, contains no live credentials or device data, and
does not contact a running ToolGate instance. `imageio-ffmpeg` supplies the encoder.
The media and source are distributed under this repository's Apache-2.0 license.

## Real console screenshots

Captured from the running local `0.10.0-dev` console on October 10, 2026:

- [Tool directory](console-tools.jpg): registered tool names, enabled/disabled state and revision.
- [Tool Groups](console-tool-groups.jpg): the standard presets and default group.
- [Configuration reviews](console-configuration.jpg): complete export and import controls.

These are browser captures of the actual application, with no fabricated UI or
replacement data. The configuration capture is cropped above change-history rows.
The selected screens contain no passwords, tokens, private user details or device
identifiers. The local demo's tool states are preserved; capturing the screens did
not change access configuration. The video renderer does not regenerate screenshots.
