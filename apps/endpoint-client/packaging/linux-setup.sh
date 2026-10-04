#!/bin/sh
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
set -eu
if [ "${1:-}" = --verify ]; then
  checksum=$(tail -c +@OFFSET@ "$0" | sha256sum)
  test "${checksum%% *}" = '@SHA256@'
  exit
fi
if [ "$(id -u)" != 0 ]; then
  if [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] && command -v zenity >/dev/null 2>&1; then
    server=$(zenity --entry --title='Install OLO ToolGate' --text='Organization HTTPS Control server:')
  else
    printf 'Organization HTTPS Control server: '
    IFS= read -r server
  fi
  case "$server" in https://*) ;; *) echo 'HTTPS address required' >&2; exit 1 ;; esac
  if command -v pkexec >/dev/null 2>&1; then
    pkexec /bin/sh "$0" --elevated "$server" "$(id -u)"
  else
    sudo /bin/sh "$0" --elevated "$server" "$(id -u)"
  fi
  if [ -n "${DISPLAY:-}${WAYLAND_DISPLAY:-}" ] && command -v zenity >/dev/null 2>&1; then
    zenity --info --title='OLO ToolGate installed' --text='System service installed. Enroll the device to use protected tools.'
  fi
  exit 0
fi
if [ "${1:-}" != --elevated ] || [ "$#" != 3 ]; then
  echo 'Launch the installer as your normal user; it requests administrator permission.' >&2
  exit 1
fi
server=$2
case "$server" in https://*) ;; *) exit 1 ;; esac
case "$3" in ''|*[!0-9]*) exit 1 ;; esac
SUDO_UID=$3; export SUDO_UID
if [ -e /etc/olo-toolgate/client.json ]; then
  echo 'Client already installed; uninstall first. Enrollment state is retained.' >&2
  exit 1
fi
directory=$(mktemp -d /tmp/olo-toolgate-setup.XXXXXXXX)
trap 'rm -f "$directory/payload.tar.gz"; rm -rf "$directory/payload"; rmdir "$directory"' EXIT HUP INT TERM
tail -c +@OFFSET@ "$0" > "$directory/payload.tar.gz"
printf '@SHA256@  %s\n' "$directory/payload.tar.gz" | sha256sum --check --status
mkdir "$directory/payload"
tar -xzf "$directory/payload.tar.gz" -C "$directory/payload" --no-same-owner
"$directory/payload/olo-toolgate-client" install --server "$server"
echo 'System service installed. Enroll the device using the included client guide.'
exit 0
