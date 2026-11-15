# Knownet Deployment auf IONOS

## 1. Vorbereitung (lokal)

### 1.1 Build mit GitHub Token
```bash
# GitHub Token erstellen: https://github.com/settings/tokens (repo scope)
export GITHUB_TOKEN=ghp_xxxxx

# Production Build
mvn clean package -Pproduction -DskipTests \
  -Denv.GITHUB_TOKEN=$GITHUB_TOKEN \
  --build-arg GITHUB_TOKEN=$GITHUB_TOKEN
```

### 1.2 Docker Image bauen
```bash
docker build --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  -t knownet-vaadin:latest .
```

## 2. Auf IONOS Server deployen

### 2.1 SSH auf Server
```bash
ssh fse@<ionos-server-ip>
```

### 2.2 Verzeichnis vorbereiten
```bash
mkdir -p ~/apps/knownet
cd ~/apps/knownet

# Repository clonen (oder Dateien manuell hochladen)
git clone https://github.com/FrankS68/knownet-vaadin.git .
```

### 2.3 Environment Variablen
```bash
# .env Datei erstellen (nicht committen!)
cat > .env << EOF
NEO4J_USER=neo4j
NEO4J_PASSWORD=generate_secure_password_here
GITHUB_CLIENT_ID=your_github_oauth_client_id
GITHUB_CLIENT_SECRET=your_github_oauth_client_secret
WITCH_AUTH_FIRST_ADMIN_EMAIL=frank.segieth@gmail.com
EOF

chmod 600 .env
```

### 2.4 Docker Image bauen auf Server
```bash
export GITHUB_TOKEN=ghp_xxxxx
docker build --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  -t knownet-vaadin:latest .
```

Oder von lokalem Registry laden (wenn vorhanden):
```bash
# Local: docker save knownet-vaadin:latest | gzip > knownet.tar.gz
# Upload: scp knownet.tar.gz fse@ionos:~/apps/knownet/
# On Server:
docker load < knownet.tar.gz
```

### 2.5 Container starten
```bash
# Mit .env Datei
docker compose up -d

# Logs checken
docker compose logs -f
```

## 3. nginx konfigurieren

### 3.1 Upstream hinzufügen
Datei: `/etc/nginx/sites-available/default` oder separate Config-Datei

```nginx
upstream knownet {
    server localhost:8082;
    keepalive 32;
}

server {
    listen 80;
    server_name knownet.example.com;
    
    location / {
        proxy_pass http://knownet;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}
```

### 3.2 nginx neu laden
```bash
sudo systemctl reload nginx
```

## 4. Checks & Monitoring

```bash
# Container Status
docker compose ps

# Logs
docker compose logs knownet
docker compose logs neo4j

# Neo4j Health
docker compose exec neo4j cypher-shell -u neo4j -p <password> "RETURN 1"

# App Health (via Browser oder curl)
curl http://localhost:8082/

# nginx Status
sudo systemctl status nginx
```

## 5. Updates & Redeployment

```bash
cd ~/apps/knownet

# Code aktualisieren
git pull

# Neu bauen
export GITHUB_TOKEN=ghp_xxxxx
docker build --build-arg GITHUB_TOKEN=$GITHUB_TOKEN \
  -t knownet-vaadin:latest .

# Container neu starten
docker compose up -d
```

## 6. Backup Neo4j Daten

```bash
# Volume sichern
docker compose exec neo4j bash -c 'tar czf /tmp/neo4j-backup.tar.gz /data'
docker cp knownet-neo4j:/tmp/neo4j-backup.tar.gz ~/backups/

# Oder: Volume mounten und local sichern
docker run --rm -v knownet_neo4j-data:/data \
  -v ~/backups:/backup alpine tar czf /backup/neo4j-backup.tar.gz /data
```

## Troubleshooting

**Neo4j startet nicht:**
```bash
docker compose logs neo4j
# Check: Port Konflikte, Disk Space, Memory
```

**App kann Neo4j nicht erreichen:**
```bash
docker compose exec knownet ping neo4j
docker compose exec knownet curl -v neo4j:7687
```

**GitHub Token Fehler beim Build:**
```bash
# Token überprüfen: https://github.com/settings/tokens
# Muss scope "repo" haben
# Oder in settings.xml eintragen
```
