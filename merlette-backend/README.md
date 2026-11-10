# Merlette — back-end

API REST Spring Boot 4 (Java 21) + PostgreSQL. Spécification : `../docs/05-api.pdf`.

## Démarrer

```bash
cp .env.example .env          # puis mettre un JWT_SECRET aléatoire (≥ 32 caractères)
docker compose up -d          # PostgreSQL de dev sur le port 5432
./mvnw spring-boot:run        # API sur http://localhost:8080/api
```

Flyway crée le schéma et le seed au démarrage (`src/main/resources/db/migration`).
Compte admin : `admin@merlette.local` / `Admin1234`.

## Documentation de l'API

Swagger UI : **http://localhost:8080/docs** (spécification OpenAPI brute : `/v3/api-docs`).
Bouton **Authorize** : coller le `token` renvoyé par `/api/auth/login`.

## Accès depuis le réseau local (équipe front)

L'API écoute sur toutes les interfaces. Depuis une autre machine : `http://<IP-de-cette-machine>:8080/api`
(IP : `ip -4 addr show`). Dans le front : `VITE_API_URL=http://<IP>:8080/api`.

- CORS accepte localhost et les réseaux privés (`10.*`, `172.*`, `192.168.*`) ; voir `CORS_ORIGINS` dans `.env`.
- Si le pare-feu bloque : `sudo ufw allow from <sous-réseau>/24 to any port 8080 proto tcp`.

## Tests

```bash
./mvnw test                   # 88 tests : unitaires + intégration (Docker requis, base lancée par Testcontainers)
npx newman run ../postman/merlette.postman_collection.json --working-dir ../postman \
  --env-var baseUrl=http://localhost:8080/api --env-var serverUrl=http://localhost:8080
```

## Structure

```
src/main/java/com/merlette/backend/
├── controller/   routes HTTP (/api/...)
├── service/      règles métier (ConsensusService = PRD §6.1, NotificationService = §6.2)
├── repository/   accès base (Spring Data JPA)
├── entity/       tables de la base
├── dto/          objets JSON des requêtes / réponses
├── security/     JWT, utilisateur courant, erreurs 401/403
├── exception/    format d'erreur uniforme { error: { code, message, details } }
└── config/       service des images /uploads
```
