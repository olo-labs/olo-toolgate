; Copyright 2026 OLO Labs
; SPDX-License-Identifier: Apache-2.0
[Setup]
AppId=OLO.ToolGate.Connect
AppName=OLO ToolGate Chrome Connect
AppVersion={#ProductVersion}
AppPublisher=OLO Labs
DefaultDirName={autopf}\OLO\ToolGateConnect
PrivilegesRequired=admin
ArchitecturesAllowed={#NativeArchitecture}
ArchitecturesInstallIn64BitMode={#NativeArchitecture}
OutputDir={#OutputDirectory}
OutputBaseFilename={#OutputName}
Compression=lzma2
SolidCompression=yes
DisableWelcomePage=yes
DisableDirPage=yes
DisableProgramGroupPage=yes
DisableReadyPage=yes
DisableFinishedPage=yes
UninstallDisplayName=OLO ToolGate Chrome Connect

[Files]
Source: "{#PayloadDirectory}\olo-toolgate-browser-host.exe"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#PayloadDirectory}\bootstrap.json"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#PayloadDirectory}\chrome-extension\*"; DestDir: "{app}\chrome-extension"; Flags: recursesubdirs createallsubdirs ignoreversion
Source: "{#PayloadDirectory}\LICENSE"; DestDir: "{app}"; Flags: ignoreversion
Source: "{#PayloadDirectory}\third-party\*"; DestDir: "{app}\third-party"; Flags: recursesubdirs createallsubdirs ignoreversion

[Registry]
Root: HKLM; Subkey: "Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect"; ValueType: string; ValueData: "{app}\browser-host.json"
; Chrome prompts to enable published extensions. No force-install policy is changed.
Root: HKLM; Subkey: "Software\Google\Chrome\Extensions\emmemldedebhbloibichmmdlbpjakfkf"; ValueType: string; ValueName: "update_url"; ValueData: "https://clients2.google.com/service/update2/crx"; Check: StorePublished; Flags: uninsdeletekey

[Code]
function StorePublished: Boolean;
begin
  Result := '{#ChromeStoreUrl}' <> '';
end;
function InitializeSetup: Boolean;
var ExitCode: Integer;
begin
  Result := True;
  if not WizardSilent then begin
    if not Exec(ExpandConstant('{srcexe}'), '/VERYSILENT /SUPPRESSMSGBOXES /NORESTART', '', SW_HIDE, ewNoWait, ExitCode) then
      RaiseException('Could not start Chrome Connect setup.');
    Result := False;
  end;
end;
procedure CurStepChanged(CurStep: TSetupStep);
var HostPath, HostDocument: String;
begin
  if CurStep = ssPostInstall then begin
    HostPath := ExpandConstant('{app}\olo-toolgate-browser-host.exe');
    StringChangeEx(HostPath, '\', '\\', True);
    HostDocument := '{"name":"io.ololabs.toolgate.connect","description":"ToolGate setup and protected client bridge","path":"' + HostPath + '","type":"stdio","allowed_origins":["chrome-extension://emmemldedebhbloibichmmdlbpjakfkf/"]}';
    if not SaveStringToFile(ExpandConstant('{app}\browser-host.json'), HostDocument, False) then
      RaiseException('Could not register the Chrome bridge.');
  end;
end;
procedure CurUninstallStepChanged(CurUninstallStep: TUninstallStep);
var Registered: String;
begin
  if CurUninstallStep = usPostUninstall then
    if RegQueryStringValue(HKLM, 'Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect', '', Registered)
      and (Registered = ExpandConstant('{app}\browser-host.json')) then
      RegDeleteKeyIncludingSubkeys(HKLM, 'Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect');
end;
