# Knownet Deployment auf IONOS

Ausgerollt wird mit [`deploy.sh`](deploy.sh) direkt auf dem Server: Das Skript holt den
gewünschten Stand per git, baut das Image mit Docker Compose, sichert die Volumes, startet
den Stack, prüft die App und schaltet bei einem Fehler automatisch auf die vorige Version zurück.

## 1. Einmalige Einrichtung auf dem Server

### 1.1 Repository klonen
```bash
ssh fse@<ionos-server>
mkdir -p ~/apps/knownet && cd ~/apps/knownet
git clone https://github.com/FrankS68/knownet-vaadin.git .
```

### 1.2 `.env` anlegen (nicht einchecken)
Vorlage: [`.env.example`](.env.example).
```bash
cat > .env << 'EOF'
# Name des Compose-Projekts = Praefix der Volumes (knownet_neo4j-data, knownet_knownet-data).
# Bei einer bestehenden Installation den bisherigen Namen beibehalten, sonst startet
# der Stack mit leeren Volumes.
COMPOSE_PROJECT_NAME=knownet

NEO4J_USER=neo4j
NEO4J_PASSWORD=<sicheres Passwort>
GITHUB_CLIENT_ID=<GitHub-OAuth-App: Client ID>
GITHUB_CLIENT_SECRET=<GitHub-OAuth-App: Client Secret>
WITCH_AUTH_FIRST_ADMIN_EMAIL=<E-Mail des ersten Admins>
EOF
chmod 600 .env
```
`COMPOSE_FILE` ist nicht nötig, Compose nimmt `docker-compose.yml`.

### 1.3 GitHub-Token für den Build
Der Build lädt witch-parent, witch-ui und witch-auth aus GitHub Packages. Dafür ein
Token (classic) mit Scope **`read:packages`** unter <https://github.com/settings/tokens>
anlegen und im Projektordner ablegen:
```bash
printf '%s' 'ghp_...' > .github_token
chmod 600 .github_token
```
Das Token wird als BuildKit-Secret eingebunden (siehe `Dockerfile`, `docker-compose.yml`)
und landet weder im Image noch im Build-Cache. Es muss im Projektordner liegen, Docker
Buildx verweigert Secrets außerhalb des Build-Kontexts.

### 1.4 nginx
Konfiguration: [`deploy/nginx-knownet.segieth.cologne.conf`](deploy/nginx-knownet.segieth.cologne.conf)
(Upstream `localhost:8082`).
```bash
sudo cp deploy/nginx-knownet.segieth.cologne.conf /etc/nginx/sites-available/knownet.segieth.cologne
sudo ln -sf /etc/nginx/sites-available/knownet.segieth.cologne /etc/nginx/sites-enabled/
sudo nginx -t && sudo systemctl reload nginx
```

## 2. Ausrollen

```bash
cd ~/apps/knownet
./deploy.sh              # aktuellen Branch per git pull aktualisieren und ausrollen
./deploy.sh v1.1.0       # bestimmten Branch oder Tag ausrollen
./deploy.sh --rollback   # vorige Version (Image knownet-vaadin:prev) wieder starten
```

Lange Läufe mit `nohup` starten, sonst beendet ein SSH-Abbruch das Skript womöglich bei
gestopptem Stack:
```bash
nohup ./deploy.sh main > deploy.log 2>&1 &
tail -f deploy.log
```

Ablauf von `deploy.sh`:

1. **Code**: bricht ab, wenn das Arbeitsverzeichnis Änderungen an eingecheckten Dateien hat;
   sonst `git fetch` und Checkout/Pull des gewünschten Stands.
2. **Bauen**: das bisherige Image wird als `knownet-vaadin:prev` markiert, dann
   `docker compose build`. Die laufende App bleibt währenddessen online. Schlägt der Build
   fehl, bleibt alles wie es war.
3. **Sichern**: Stack stoppen, Volumes `neo4j-data` und `knownet-data` nach
   `~/backups/knownet/<volume>-<datum>-<commit>.tgz` sichern (die letzten 10 je Volume bleiben).
4. **Starten und prüfen**: `docker compose up -d`, dann warten bis
   „Started KnownetApplication“ im Log steht und `/` sowie `/api/graph` mit 200 antworten.
   Erfolgreiche Deploys landen in `deploy-history.tsv`.
5. **Rollback**: startet die neue Version nicht (oder meldet „Application run failed“),
   zeigt das Skript die letzten Log-Zeilen und schaltet automatisch auf `knownet-vaadin:prev` zurück.

Nur ein Deploy läuft gleichzeitig (Sperrdatei `.deploy.lock`).

Einstellbar über Umgebungsvariablen:

| Variable         | Default                 | Bedeutung                             |
|------------------|-------------------------|---------------------------------------|
| `HEALTH_URL`     | `http://localhost:8082` | Adresse für die Prüfung nach dem Start |
| `HEALTH_TIMEOUT` | `240`                   | Sekunden bis zum Abbruch der Prüfung   |
| `BACKUP_DIR`     | `~/backups/knownet`     | Ziel der Volume-Sicherungen            |
| `BACKUP_KEEP`    | `10`                    | Anzahl Sicherungen je Volume           |

## 3. Daten wiederherstellen

Hat eine neue Version das Schema verändert, reicht der Rollback des Images nicht. Dann die
Sicherung von vor dem Deploy zurückspielen (Beispiel Neo4j):
```bash
docker compose stop
docker run --rm -v knownet_neo4j-data:/v -v ~/backups/knownet:/b alpine \
  sh -c 'rm -rf /v/* && tar xzf /b/neo4j-data-<datum>-<commit>.tgz -C /v'
docker compose up -d
```
Für `knownet-data` genauso. Der Volume-Präfix entspricht `COMPOSE_PROJECT_NAME`.

## 4. Checks & Monitoring

```bash
docker compose ps
docker compose logs -f knownet
docker compose logs neo4j
docker compose exec neo4j cypher-shell -u neo4j -p <password> "RETURN 1"
curl -s -o /dev/null -w '%{http_code}\n' http://localhost:8082/
cat deploy-history.tsv
ls -lt ~/backups/knownet | head
```

## Troubleshooting

**Build schlägt fehl mit 401/403 von maven.pkg.github.com:**
`.github_token` fehlt, ist abgelaufen oder hat nicht den Scope `read:packages`.

**„Es läuft bereits ein Deploy.“:**
Ein anderer `deploy.sh` läuft noch (`ps aux | grep deploy.sh`). Die Sperre löst sich mit dem
Prozess von selbst.

**„Arbeitsverzeichnis hat Änderungen“:**
Auf dem Server wurden eingecheckte Dateien geändert (`git status`, `git diff`). Änderungen
zurücknehmen oder committen; Server-Konfiguration gehört in `.env`, nicht in eingecheckte Dateien.

**Neo4j startet nicht:**
```bash
docker compose logs neo4j
# Check: Port-Konflikte, Plattenplatz, Speicher
```

**App kann Neo4j nicht erreichen:**
```bash
docker compose exec knownet curl -v neo4j:7687
```
