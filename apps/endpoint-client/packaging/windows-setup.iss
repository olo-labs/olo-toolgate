; Copyright 2026 OLO Labs
; SPDX-License-Identifier: Apache-2.0
[Setup]
AppId=OLO.ToolGate.Client
AppName=OLO ToolGate Client
AppVersion={#ProductVersion}
AppPublisher=OLO Labs
DefaultDirName={autopf}\OLO\ToolGateSetup
DisableDirPage=yes
DisableProgramGroupPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed={#NativeArchitecture}
ArchitecturesInstallIn64BitMode={#NativeArchitecture}
OutputDir={#OutputDirectory}
OutputBaseFilename={#OutputName}
Compression=lzma2
SolidCompression=yes
LicenseFile={#PayloadDirectory}\LICENSE
InfoBeforeFile={#PayloadDirectory}\RELEASE-NOTES.md
UninstallDisplayName=OLO ToolGate Client

[Files]
Source: "{#PayloadDirectory}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Registry]
Root: HKLM; Subkey: "Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect"; ValueType: string; ValueData: "{app}\browser-host.json"; Flags: uninsdeletekey

[Code]
var ServerPage: TInputQueryWizardPage;
procedure InitializeWizard;
begin
  ServerPage := CreateInputQueryPage(wpWelcome, 'Control server',
    'Enter your organization''s HTTPS server address',
    'Installation creates a background service. Enroll the device afterward.');
  ServerPage.Add('Server URL:', False);
  ServerPage.Values[0] := ExpandConstant('{param:SERVER|}');
end;
function NextButtonClick(CurPageID: Integer): Boolean;
var URL: String;
begin
  Result := True;
  if CurPageID = ServerPage.ID then begin
    URL := ServerPage.Values[0];
    Result := (Pos('https://', URL) = 1) and (Length(URL) <= 2048)
      and (Pos('"', URL) = 0) and (Pos(#13, URL) = 0) and (Pos(#10, URL) = 0);
    if not Result then MsgBox('Enter an HTTPS address without quotes or line breaks.', mbError, MB_OK);
  end;
end;
function PrepareToInstall(var NeedsRestart: Boolean): String;
begin
  Result := '';
  if FileExists(ExpandConstant('{commonappdata}\OLO\ToolGate\client.json')) then
    Result := 'A client is already installed. Uninstall it first; enrollment data is retained.';
end;
procedure CurStepChanged(CurStep: TSetupStep);
var ExitCode: Integer; HostPath, HostDocument: String;
begin
  if CurStep = ssPostInstall then begin
    if not Exec(ExpandConstant('{app}\olo-toolgate-client.exe'),
      'install --server "' + ServerPage.Values[0] + '"', '', SW_HIDE, ewWaitUntilTerminated, ExitCode) then
      RaiseException('Could not start service installation.');
    if ExitCode <> 0 then RaiseException('Service installation failed. Check the client configuration and Windows service logs.');
    HostPath := ExpandConstant('{app}\olo-toolgate-browser-host.exe');
    StringChangeEx(HostPath, '\', '\\', True);
    HostDocument := '{"name":"io.ololabs.toolgate.connect","description":"ToolGate protected client bridge","path":"' + HostPath + '","type":"stdio","allowed_origins":["chrome-extension://emmemldedebhbloibichmmdlbpjakfkf/"]}';
    if not SaveStringToFile(ExpandConstant('{app}\browser-host.json'), HostDocument, False) then
      RaiseException('Could not register the Chrome client bridge.');
  end;
end;
function InitializeUninstall: Boolean;
var ExitCode: Integer;
begin
  Result := True;
  if FileExists(ExpandConstant('{commonappdata}\OLO\ToolGate\client.json')) then
    Result := Exec(ExpandConstant('{app}\olo-toolgate-client.exe'), 'uninstall', '',
      SW_HIDE, ewWaitUntilTerminated, ExitCode) and (ExitCode = 0);
end;
