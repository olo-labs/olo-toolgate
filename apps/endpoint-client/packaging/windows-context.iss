// Copyright 2026 OLO Labs
// SPDX-License-Identifier: Apache-2.0
// Public downloads carry the server origin in the filename, never credentials.
function ValidServer(const URL: String): Boolean;
var I: Integer;
begin
  Result := (Pos('https://', URL) = 1) and (Length(URL) <= 2048);
  for I := 1 to Length(URL) do
    if Pos(URL[I], 'abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789:/.[]-') = 0 then
      Result := False;
end;
function SetupServer(const Value: String): String;
begin
  Result := Trim(Value);
  if Result = '' then Result := 'https://localhost:18450';
  if (Pos('localhost:', Result) = 1) or (Pos('127.0.0.1:', Result) = 1) then
    Result := 'http://' + Result;
end;
function ValidSetupServer(const URL: String): Boolean;
var Authority: String; I, PortStart: Integer;
begin
  Result := ValidServer(URL);
  if Result then Exit;
  Result := False;
  if Pos('http://', URL) <> 1 then Exit;
  Authority := Copy(URL, 8, Length(URL));
  if (Length(Authority) > 0) and (Authority[Length(Authority)] = '/') then
    Delete(Authority, Length(Authority), 1);
  PortStart := Pos(':', Authority);
  if PortStart > 0 then begin
    if (PortStart = Length(Authority)) or (Length(Authority) - PortStart > 5) then Exit;
    for I := PortStart + 1 to Length(Authority) do
      if Pos(Authority[I], '0123456789') = 0 then Exit;
    if (StrToIntDef(Copy(Authority, PortStart + 1, Length(Authority)), 0) < 1) or
      (StrToIntDef(Copy(Authority, PortStart + 1, Length(Authority)), 0) > 65535) then Exit;
    Authority := Copy(Authority, 1, PortStart - 1);
  end;
  Result := (Authority = 'localhost') or (Authority = '127.0.0.1');
end;
function ServerFromDownload: String;
var Name, Encoded: String; Start, Finish, I, N: Integer;
begin
  Result := '';
  Name := ExtractFileName(ExpandConstant('{srcexe}'));
  Start := Pos('--', Name);
  Finish := Pos('.setup', Name);
  if (Start = 0) or (Finish <= Start + 2) then Exit;
  Encoded := Copy(Name, Start + 2, Finish - Start - 2);
  if (Length(Encoded) mod 2 <> 0) or (Length(Encoded) > 180) then Exit;
  I := 1;
  while I < Length(Encoded) do begin
    N := StrToIntDef('$' + Copy(Encoded, I, 2), -1);
    if (N < 32) or (N > 126) then begin Result := ''; Exit; end;
    Result := Result + Chr(N);
    I := I + 2;
  end;
  if not ValidServer(Result) then Result := '';
end;
