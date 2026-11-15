#!/usr/bin/env bash
# Knownet ausrollen: Stand auschecken, bauen, Volumes sichern, starten, pruefen,
# bei Fehlschlag auf das vorige Image zurueckschalten.
#
#   ./deploy.sh              aktuellen Branch per git pull aktualisieren und ausrollen
#   ./deploy.sh <ref>        Branch oder Tag (z.B. v1.1.0) ausrollen
#   ./deploy.sh --rollback   vorige Version (Image knownet-vaadin:prev) wieder starten
#
# Erwartet im Verzeichnis des Skripts: .env (COMPOSE_PROJECT_NAME, COMPOSE_FILE, NEO4J_PASSWORD)
# und .github_token (read:packages) fuer den Build.
# Umgebungsvariablen: HEALTH_URL (Default http://localhost:8082), HEALTH_TIMEOUT (Sekunden, 240),
# BACKUP_DIR (~/backups/knownet), BACKUP_KEEP (Anzahl Sicherungen je Volume, 10).
set -euo pipefail

cd "$(dirname "$(readlink -f "$0")")"

IMAGE=knownet-vaadin
APP=knownet-app
HEALTH_URL=${HEALTH_URL:-http://localhost:8082}
HEALTH_TIMEOUT=${HEALTH_TIMEOUT:-240}
BACKUP_DIR=${BACKUP_DIR:-$HOME/backups/knownet}
BACKUP_KEEP=${BACKUP_KEEP:-10}
PROJECT=$(grep -oP '^COMPOSE_PROJECT_NAME=\K.*' .env 2>/dev/null || basename "$PWD")

log() { printf '%s  %s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$*"; }
die() { log "FEHLER: $*"; exit 1; }

# Nur ein Deploy gleichzeitig
exec 9>".deploy.lock"
flock -n 9 || die "Es laeuft bereits ein Deploy."

backup_volumes() {
    local stamp=$1
    mkdir -p "$BACKUP_DIR"
    log "Stoppe Stack fuer konsistente Sicherung"
    docker compose stop >/dev/null
    for v in neo4j-data knownet-data; do
        docker run --rm -v "${PROJECT}_${v}:/v:ro" -v "$BACKUP_DIR:/b" alpine \
            tar czf "/b/${v}-${stamp}.tgz" -C /v .
        log "Gesichert: $BACKUP_DIR/${v}-${stamp}.tgz"
        # alte Sicherungen aufraeumen
        ls -1t "$BACKUP_DIR/${v}-"*.tgz 2>/dev/null | tail -n +$((BACKUP_KEEP + 1)) | xargs -r rm -f
    done
}

wait_healthy() {
    local since=$1 deadline=$((SECONDS + HEALTH_TIMEOUT))
    log "Warte auf Start (max. ${HEALTH_TIMEOUT}s)"
    while (( SECONDS < deadline )); do
        if docker logs --since "$since" "$APP" 2>&1 | grep -q "Application run failed"; then
            log "App meldet 'Application run failed'"
            return 1
        fi
        if docker logs --since "$since" "$APP" 2>&1 | grep -q "Started KnownetApplication"; then
            local root graph
            root=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL/" || true)
            graph=$(curl -s -o /dev/null -w '%{http_code}' "$HEALTH_URL/api/graph" || true)
            if [[ $root == 200 && $graph == 200 ]]; then
                log "App laeuft: / -> $root, /api/graph -> $graph"
                return 0
            fi
        fi
        sleep 5
    done
    log "Zeitlimit erreicht"
    return 1
}

start_and_check() {
    local since
    since=$(date -u +%Y-%m-%dT%H:%M:%SZ)
    docker compose up -d >/dev/null
    wait_healthy "$since"
}

rollback() {
    docker image inspect "$IMAGE:prev" >/dev/null 2>&1 || die "Kein Image $IMAGE:prev vorhanden."
    log "Rollback: $IMAGE:prev -> $IMAGE:latest"
    docker tag "$IMAGE:prev" "$IMAGE:latest"
    if start_and_check; then
        log "Rollback erfolgreich. Daten: Falls die neue Version das Schema geaendert hat,"
        log "liegt die Sicherung von vor dem Deploy in $BACKUP_DIR."
    else
        die "Auch die vorige Version startet nicht. Logs: docker logs $APP"
    fi
}

if [[ ${1:-} == "--rollback" ]]; then
    rollback
    exit 0
fi

REF=${1:-}

# 1. Code
[[ -z $(git status --porcelain --untracked-files=no) ]] || die "Arbeitsverzeichnis hat Aenderungen (git status)."
git fetch -q --tags origin
if [[ -z $REF ]]; then
    git pull -q --ff-only
    REF=$(git rev-parse --abbrev-ref HEAD)
elif git show-ref -q --verify "refs/remotes/origin/$REF"; then
    git checkout -q "$REF"
    git pull -q --ff-only
else
    git checkout -q "$REF"   # Tag oder Commit (detached)
fi
SHA=$(git rev-parse --short HEAD)
log "Stand: $REF @ $SHA ($(git log -1 --format=%s))"

# 2. Bauen (die laufende App bleibt waehrenddessen online)
log "Baue Image"
if docker image inspect "$IMAGE:latest" >/dev/null 2>&1; then
    docker tag "$IMAGE:latest" "$IMAGE:prev"
fi
docker compose build -q knownet || die "Build fehlgeschlagen; laufende Version unveraendert."
docker tag "$IMAGE:latest" "$IMAGE:$SHA"

# 3. Sichern
STAMP="$(date +%Y%m%d-%H%M)-$SHA"
backup_volumes "$STAMP"

# 4. Starten und pruefen
if start_and_check; then
    log "Deploy erfolgreich: $REF @ $SHA"
    printf '%s\t%s\t%s\n' "$(date '+%Y-%m-%d %H:%M:%S')" "$REF" "$SHA" >> deploy-history.tsv
else
    log "Neue Version startet nicht – letzte Log-Zeilen:"
    docker logs --tail 15 "$APP" 2>&1 | sed 's/^/    /'
    rollback
    exit 1
fi
