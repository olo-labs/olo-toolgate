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
DisableWelcomePage=yes
DisableReadyPage=yes
DisableFinishedPage=yes
PrivilegesRequired=admin
ArchitecturesAllowed={#NativeArchitecture}
ArchitecturesInstallIn64BitMode={#NativeArchitecture}
OutputDir={#OutputDirectory}
OutputBaseFilename={#OutputName}
Compression=lzma2
SolidCompression=yes
UninstallDisplayName=OLO ToolGate Client
CloseApplications=yes

[Files]
Source: "{#PayloadDirectory}\*"; DestDir: "{app}"; Flags: recursesubdirs createallsubdirs ignoreversion

[Registry]
Root: HKLM; Subkey: "Software\Google\Chrome\NativeMessagingHosts\io.ololabs.toolgate.connect"; ValueType: string; ValueData: "{app}\browser-host.json"; Flags: uninsdeletekey
Root: HKLM32; Subkey: "Software\Google\Chrome\Extensions\emmemldedebhbloibichmmdlbpjakfkf"; ValueType: string; ValueName: "update_url"; ValueData: "https://clients2.google.com/service/update2/crx"; Check: StorePublished; Flags: uninsdeletekey
Root: HKLM; Subkey: "Software\OLO\ToolGate"; ValueType: string; ValueName: "ServerUrl"; ValueData: "{code:ServerUrl}"; Flags: uninsdeletevalue
Root: HKLM; Subkey: "Software\Microsoft\Windows\CurrentVersion\Run"; ValueType: string; ValueName: "OloToolGateTray"; ValueData: """{sys}\WindowsPowerShell\v1.0\powershell.exe"" -NoProfile -STA -ExecutionPolicy RemoteSigned -WindowStyle Hidden -File ""{app}\packaging\toolgate-tray.ps1"""; Flags: uninsdeletevalue

[Run]
Filename: "{sys}\WindowsPowerShell\v1.0\powershell.exe"; Parameters: "-NoProfile -STA -ExecutionPolicy RemoteSigned -WindowStyle Hidden -File ""{app}\packaging\toolgate-tray.ps1"""; Flags: nowait runasoriginaluser

[Code]
#include "windows-context.iss"
var MaintenancePage: TInputOptionWizardPage;
    GatewayPage: TInputQueryWizardPage;
    ConfiguredServer: String; ExistingClient, MaintenanceComplete: Boolean;
function StorePublished: Boolean;
begin
  Result := '{#ChromeStoreUrl}' <> '';
end;
function ServerUrl(Param: String): String;
begin
  Result := ConfiguredServer;
end;
procedure InitializeWizard;
begin
  ExistingClient := FileExists(ExpandConstant('{commonappdata}\OLO\ToolGate\client.json'));
  ConfiguredServer := ExpandConstant('{param:SERVER|}');
  if WizardSilent and (ConfiguredServer = '') then ConfiguredServer := ServerFromDownload;
  if ExistingClient and (ConfiguredServer = '') then
    RegQueryStringValue(HKLM, 'Software\OLO\ToolGate', 'ServerUrl', ConfiguredServer);
  ConfiguredServer := SetupServer(ConfiguredServer);
  MaintenancePage := CreateInputOptionPage(wpWelcome, 'ToolGate is already installed',
    'Choose what to do with the existing client',
    'Reinstall preserves enrollment for the same gateway. Switching gateways starts a new enrollment. Uninstall removes the service and tray icon; device keys are retained.', True, False);
  MaintenancePage.Add('Repair / reinstall');
  MaintenancePage.Add('Uninstall');
  MaintenancePage.SelectedValueIndex := 0;
  GatewayPage := CreateInputQueryPage(MaintenancePage.ID, 'Gateway connection',
    'Choose the gateway for this computer',
    'Copy the Gateway URL shown on Enroll Device, or keep https://localhost:18450 for a local gateway. No credentials are required.');
  GatewayPage.Add('&Gateway URL (optional):', False);
  GatewayPage.Values[0] := ConfiguredServer;
end;
function ShouldSkipPage(PageID: Integer): Boolean;
begin
  Result := (PageID = MaintenancePage.ID) and not ExistingClient;
end;
function NextButtonClick(CurPageID: Integer): Boolean;
var ExitCode: Integer;
begin
  Result := True;
  if CurPageID = GatewayPage.ID then begin
    ConfiguredServer := SetupServer(GatewayPage.Values[0]);
    Result := ValidSetupServer(ConfiguredServer);
    if not Result then
      MsgBox('Enter a gateway URL, such as https://localhost:18450.', mbError, MB_OK);
  end;
  if ExistingClient and (CurPageID = MaintenancePage.ID) and (MaintenancePage.SelectedValueIndex = 1) then begin
    Result := False;
    if Exec(ExpandConstant('{app}\unins000.exe'), '/SILENT /SUPPRESSMSGBOXES /NORESTART', '',
      SW_HIDE, ewWaitUntilTerminated, ExitCode) and (ExitCode = 0) then begin
      MaintenanceComplete := True;
      WizardForm.Close;
    end else MsgBox('Uninstallation failed. Check the Windows service logs.', mbError, MB_OK);
  end;
end;
procedure CancelButtonClick(CurPageID: Integer; var Cancel, Confirm: Boolean);
begin
  if MaintenanceComplete then begin Cancel := True; Confirm := False; end;
end;
function PrepareToInstall(var NeedsRestart: Boolean): String;
var ExitCode: Integer; Output: TExecOutput;
begin
  Result := '';
  if not WizardSilent then ConfiguredServer := SetupServer(GatewayPage.Values[0]);
  if not ValidSetupServer(ConfiguredServer) then
    Result := 'Enter a valid gateway URL, such as https://localhost:18450.'
  else begin
    ExtractTemporaryFile('olo-toolgate-client.exe');
    if not ExecAndCaptureOutput(ExpandConstant('{tmp}\olo-toolgate-client.exe'),
      'resolve-server --server "' + ConfiguredServer + '"', '', SW_SHOWNORMAL, ewWaitUntilTerminated, ExitCode, Output) or
      (ExitCode <> 0) or Output.Error or (GetArrayLength(Output.StdOut) <> 1) then
      Result := 'Could not resolve the Gateway URL. Enter the HTTPS Gateway URL shown on Enroll Device, such as https://localhost:18450, then retry.'
    else if not ValidServer(Output.StdOut[0]) then
      Result := 'The console did not publish a valid HTTPS Gateway URL.'
    else ConfiguredServer := Output.StdOut[0];
  end;
end;
procedure CurStepChanged(CurStep: TSetupStep);
var ExitCode: Integer; HostPath, HostDocument, Operation, Parameters, Peer: String;
begin
  if CurStep = ssPostInstall then begin
    Operation := 'install';
    if ExistingClient then Operation := 'reinstall';
    Parameters := Operation + ' --server "' + ConfiguredServer + '"';
    Peer := ExpandConstant('{param:PEER|}');
    if Peer <> '' then begin
      if (Pos('S-1-', Peer) <> 1) or (Pos('"', Peer) <> 0) or (Pos(' ', Peer) <> 0) then
        RaiseException('Invalid Windows account.');
      Parameters := Parameters + ' --peer "' + Peer + '"';
    end;
    if ExistingClient then begin
      if not Exec(ExpandConstant('{app}\olo-toolgate-client.exe'),
        'configure --server "' + ConfiguredServer + '"', '', SW_HIDE, ewWaitUntilTerminated, ExitCode) or (ExitCode <> 0) then
        RaiseException('Could not update the existing gateway configuration.');
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
