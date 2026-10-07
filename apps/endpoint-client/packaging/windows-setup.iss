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
CloseApplications=yes

[Files]
Source: "{#PayloadDirectory}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Registry]
Root: HKLM; Subkey: "Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect"; ValueType: string; ValueData: "{app}\browser-host.json"; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\OLO\ToolGate"; ValueType: string; ValueName: "ServerUrl"; ValueData: "{code:ServerUrl}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "OloToolGateTray"; ValueData: """{sys}\WindowsPowerShell\v1.0\powershell.exe"" -NoProfile -WindowStyle Hidden -File ""{app}\packaging\toolgate-tray.ps1"""; Flags: uninsdeletevalue

[Run]
Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -WindowStyle Hidden -File ""{app}\packaging\toolgate-tray.ps1"""; Flags: nowait runasoriginaluser

[Code]
#include "windows-context.iss"
var ServerPage: TInputQueryWizardPage; MaintenancePage: TInputOptionWizardPage;
    ConfiguredServer: String; ExistingClient: Boolean;
function ServerUrl(Param: String): String;
begin
  Result := ServerPage.Values[0];
end;
procedure InitializeWizard;
begin
  ExistingClient := FileExists(ExpandConstant('{commonappdata}\OLO\ToolGate\client.json'));
  ConfiguredServer := ExpandConstant('{param:SERVER|}');
  if ConfiguredServer = '' then ConfiguredServer := ServerFromDownload;
  if ExistingClient and (ConfiguredServer = '') then
    RegQueryStringValue(HKLM, 'Software\OLO\ToolGate', 'ServerUrl', ConfiguredServer);
  MaintenancePage := CreateInputOptionPage(wpWelcome, 'ToolGate is already installed',
    'Choose what to do with the existing client',
    'Reinstall preserves gateway configuration and enrollment. Uninstall removes the service and tray icon; device credentials are retained.', True, False);
  MaintenancePage.Add('Repair / reinstall');
  MaintenancePage.Add('Uninstall');
  MaintenancePage.SelectedValueIndex := 0;
  ServerPage := CreateInputQueryPage(wpWelcome, 'Control server',
    'Enter your organization''s HTTPS server address',
    'Installation creates a background service. Enroll the device afterward.');
  ServerPage.Add('Server URL:', False);
  ServerPage.Values[0] := ConfiguredServer;
end;
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := ((PageID = MaintenancePage.ID) and not ExistingClient)
    or ((PageID = ServerPage.ID) and (ValidServer(ConfiguredServer)
      or (ExistingClient and (MaintenancePage.SelectedValueIndex = 1))));
end;
function NextButtonClick(CurPageID: Integer): Boolean;
var URL: String;
begin
  Result := True;
  if CurPageID = ServerPage.ID then begin
    URL := ServerPage.Values[0];
    Result := ValidServer(URL);
    if not Result then MsgBox('Enter an HTTPS address without quotes or line breaks.', mbError, MB_OK);
  end;
end;
function PrepareToInstall(var NeedsRestart: Boolean): String;
var ExitCode: Integer;
begin
  Result := '';
  if ExistingClient and (MaintenancePage.SelectedValueIndex = 1) then begin
    if not Exec(ExpandConstant('{app}\unins000.exe'), '/SILENT /NORESTART', '',
      SW_HIDE, ewWaitUntilTerminated, ExitCode) or (ExitCode <> 0) then
      Result := 'Uninstallation failed. Check the Windows service logs.'
    else Result := 'ToolGate was uninstalled. Close this setup window.';
    Exit;
  end;
  if not ValidServer(ServerPage.Values[0]) then
    Result := 'A valid HTTPS gateway address is required.';
end;
procedure CurStepChanged(CurStep: TSetupStep);
var ExitCode: Integer; HostPath, HostDocument, Operation, Parameters, Peer: String;
begin
  if CurStep = ssPostInstall then begin
    Operation := 'install';
    if ExistingClient then Operation := 'reinstall';
    Parameters := Operation + ' --server "' + ServerPage.Values[0] + '"';
    Peer := ExpandConstant('{param:PEER|}');
    if Peer <> '' then begin
      if (Pos('S-1-', Peer) <> 1) or (Pos('"', Peer) <> 0) or (Pos(' ', Peer) <> 0) then
        RaiseException('Invalid Windows account.');
      Parameters := Parameters + ' --peer "' + Peer + '"';
    end;
    if not Exec(ExpandConstant('{app}\olo-toolgate-client.exe'),
      Parameters, '', SW_HIDE, ewWaitUntilTerminated, ExitCode) then
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
