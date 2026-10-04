-- Copyright 2026 OLO Labs
-- SPDX-License-Identifier: Apache-2.0
on run
  set response to display dialog "Enter your organization's HTTPS Control server. The system service keeps running after logout. Enroll the device after installation." default answer "" buttons {"Cancel", "Install"} default button "Install"
  set serverURL to text returned of response
  if serverURL does not start with "https://" or (length of serverURL) > 2048 then
    display alert "An HTTPS server address is required."
    return
  end if
  set installerPath to POSIX path of (path to me)
  set clientPath to installerPath & "Contents/Resources/payload/olo-toolgate-client"
  set callerUID to do shell script "/usr/bin/id -u"
  try
    do shell script "SUDO_UID=" & quoted form of callerUID & " " & quoted form of clientPath & " install --server " & quoted form of serverURL with administrator privileges
    display dialog "The background service is installed. Use the client enrollment guide included in this disk image to enroll your device." buttons {"OK"} default button "OK"
  on error messageText number errorNumber
    if errorNumber is not -128 then display alert "Installation failed" message messageText
  end try
end run
