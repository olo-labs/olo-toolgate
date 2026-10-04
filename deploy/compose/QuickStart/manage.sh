#!/bin/sh
# Copyright 2026 OLO Labs
# SPDX-License-Identifier: Apache-2.0
set -eu
cd "$(dirname "$0")"
operation=${1:-deploy}
skip_pull=false
case "${2:-}" in --no-pull) skip_pull=true;; '') ;; *) echo 'Optional second argument: --no-pull' >&2; exit 2;; esac
case "$operation" in deploy|undeploy|update|status|logs|configure|token|policy) ;; *) echo 'Usage: manage.sh deploy|undeploy|update|status|logs|configure|token|policy' >&2; exit 2;; esac
command -v docker >/dev/null || { echo 'Install Docker with Compose v2.' >&2; exit 1; }
docker info >/dev/null
docker compose version
[ -f .env ] || cp .env.example .env
mkdir -p env
chmod 711 env
stack=false
set -- compose -f compose.yaml
if [ -f external-db.yaml ]; then
    stack=true
    umask 077
    mkdir -p env
    chmod 711 env
    if [ "$operation" = configure ] || { [ ! -f env/mode.txt ] && [ "$operation" = deploy ]; }; then
        printf 'Deploy bundled database and dependency containers? [Y/n] '
        read -r choice
        bundled=bundled
        proxy=proxy
        case "$choice" in n|N|no|NO)
            bundled=external
            ask() { printf '%s [%s]: ' "$1" "$2" >&2; read -r value; printf '%s' "${value:-$2}"; }
            db_host=$(ask 'Existing PostgreSQL host reachable from container' host.docker.internal)
            case "$db_host" in ''|*[!a-zA-Z0-9.-]*) echo 'Invalid hostname.' >&2; exit 2;; esac
            db_port=$(ask 'Database port' 5432)
            case "$db_port" in ''|*[!0-9]*) exit 2;; esac
            [ "$db_port" -ge 1 ] && [ "$db_port" -le 65535 ] || exit 2
            db_name=$(ask 'Database name' control)
            app_user=$(ask 'Restricted runtime user' control_app)
            migration_user=$(ask 'Migration user (must differ)' control_migrator)
            for identifier in "$db_name" "$app_user" "$migration_user"; do
                case "$identifier" in ''|[0-9]*|*[!a-zA-Z0-9_]*) echo 'Simple SQL identifiers required.' >&2; exit 2;; esac
            done
            [ "$app_user" != "$migration_user" ] || { echo 'Separate users required.' >&2; exit 2; }
            password() {
                printf '%s: ' "$1" >&2
                if [ -t 0 ]; then
                    saved_tty=$(stty -g)
                    trap 'stty "$saved_tty"' EXIT HUP INT TERM
                    stty -echo
                    read -r secret
                    stty "$saved_tty"
                    trap - EXIT HUP INT TERM
                    printf '\n' >&2
                else read -r secret; fi
                [ -n "$secret" ] || { echo 'Password required.' >&2; exit 2; }
                printf '%s' "$secret"
            }
            app_password=$(password 'Runtime password')
            migration_password=$(password 'Migration password')
            ssl=$(ask 'TLS mode: verify-full / require / disable' verify-full)
            case "$ssl" in verify-full|require|disable) ;; *) exit 2;; esac
            jdbc="jdbc:postgresql://$db_host:$db_port/$db_name?sslmode=$ssl"
            development=true
            if [ "$ssl" = verify-full ]; then
                ca=$(ask 'Path to PostgreSQL CA certificate (PEM)' '')
                [ -f "$ca" ] || { echo 'CA certificate required.' >&2; exit 2; }
                cp "$ca" env/db-ca.crt
                chmod 644 env/db-ca.crt
                jdbc="$jdbc&sslrootcert=/external/db-ca.crt"
                development=false
            fi
            {
                printf 'QUARKUS_DATASOURCE_JDBC_URL=%s\n' "$jdbc"
                printf 'QUARKUS_DATASOURCE_USERNAME=%s\nQUARKUS_DATASOURCE_PASSWORD=%s\n' "$app_user" "$app_password"
                printf 'QUARKUS_FLYWAY_USERNAME=%s\nQUARKUS_FLYWAY_PASSWORD=%s\n' "$migration_user" "$migration_password"
                printf 'TOOLGATE_CONTROL_DEVELOPMENT_MODE=%s\n' "$development"
            } > env/.env.db
            redis_url=$(ask 'Existing Redis URL' redis://host.docker.internal:6379/0)
            case "$redis_url" in redis://*|rediss://*) ;; *) echo 'Redis URL required.' >&2; exit 2;; esac
            printf 'Redis password (empty if none): ' >&2
            if [ -t 0 ]; then
                saved_tty=$(stty -g); trap 'stty "$saved_tty"' EXIT HUP INT TERM; stty -echo
                read -r redis_password
                stty "$saved_tty"; trap - EXIT HUP INT TERM; printf '\n' >&2
            else read -r redis_password; fi
            printf 'TOOLGATE_CACHE_MODE=redis\nTOOLGATE_REDIS_URL=%s\nTOOLGATE_REDIS_PASSWORD=%s\n' "$redis_url" "$redis_password" > env/.env.cache
            printf 'Deploy bundled HTTPS proxy? [Y/n] '
            read -r choice
            case "$choice" in n|N|no|NO)
                proxy=external-proxy
                public_url=$(ask 'Existing HTTPS proxy origin' https://localhost:8444)
                case "$public_url" in https://*) ;; *) echo 'HTTPS origin required.' >&2; exit 2;; esac
                printf '%s\n' "$public_url" > env/proxy-url.txt
            ;; esac
        ;; esac
        printf '%s\n%s\n' "$bundled" "$proxy" > env/mode.txt
    fi
    [ -f env/mode.txt ] || { echo 'Run deploy first.' >&2; exit 2; }
    bundled=$(sed -n '1p' env/mode.txt)
    proxy=$(sed -n '2p' env/mode.txt)
    case "$bundled" in bundled) set -- "$@" --profile database;; external) set -- "$@" -f external-db.yaml;; *) exit 2;; esac
    case "$proxy" in proxy) set -- "$@" --profile proxy;; external-proxy) set -- "$@" -f external-proxy.yaml;; *) exit 2;; esac
fi
if [ "$stack" = false ] && [ "$skip_pull" = false ]; then
    case "$operation" in deploy|update)
        image=$(docker "$@" config --images)
        case "$image" in */*) ;; *)
            docker image inspect "$image" >/dev/null 2>&1 || {
                echo "Local-only image '$image' is missing. Build it first, or set QUICKSTART_IMAGE=ololab/olo-toolgate-quickstart:dev in .env." >&2
                exit 1
            }
            echo "Using local image $image; skipping registry pull. Set QUICKSTART_IMAGE in .env to a published image for registry updates."
            skip_pull=true
            ;;
        esac
        ;;
    esac
fi
case "$operation" in
    configure) echo 'Configuration saved; run deploy to apply.'; exit;;
    undeploy) docker "$@" down; exit;;
    status) docker "$@" ps; exit;;
    logs) docker "$@" logs --tail 100; exit;;
    token) "$stack" || { echo 'Token command is for GatewayControl.' >&2; exit 2; }; docker "$@" run --rm --no-deps token; exit;;
    policy)
        "$stack" || { echo 'Policy command is for GatewayControl.' >&2; exit 2; }
        docker "$@" run --rm --no-deps setup apply-policy
        docker "$@" restart gateway
        docker "$@" run --rm --no-deps probe
        exit;;
esac
if "$stack"; then
    if ! "$skip_pull"; then docker "$@" pull setup; fi
    docker "$@" run --rm --no-deps setup init
    [ "$bundled" != bundled ] || docker "$@" run --rm --no-deps -e "HOST_UID=$(id -u)" -e "HOST_GID=$(id -g)" setup export-db
fi
docker "$@" config --quiet
if ! "$skip_pull"; then docker "$@" pull; fi
docker "$@" up -d --wait --wait-timeout 180
if "$stack"; then docker "$@" run --rm --no-deps probe; fi
if ! "$stack"; then docker "$@" exec -T quickstart /opt/quickstart-python/bin/python /opt/quickstart/check-options.py; fi
echo 'Deployment ready. See README.md for URLs, credentials and operations.'
