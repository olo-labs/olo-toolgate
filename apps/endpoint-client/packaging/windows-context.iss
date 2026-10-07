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
