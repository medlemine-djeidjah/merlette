# Lot 1 — Comptes et publication d'observations

## Périmètre

Implémentation Spring Boot / React des stories #11 (inscription), de la partie
authentification de #10, #1/#13 (publication avec photos), #8/#9 (fil et catégories),
de la consultation de #15 et de la liste personnelle de #14.
Les votes, propositions, commentaires, notifications, profils publics, édition,
suppression et administration restent à développer. Les tables correspondantes
sont déjà présentes pour conserver le schéma métier existant.

Les PDF historiques décrivent parfois Express et le port 3000. Pour ce lot,
le backend est **Spring Boot sur le port 8080**, le frontend React sur 5173.
Cette note décrit le comportement réellement implémenté. La collection complète
reste une cible pour les lots suivants ; utiliser `lot1.postman_collection.json`
pour la démonstration actuelle.

## Démarrage PowerShell

Prérequis : JDK 21, Node.js/npm, Docker Desktop démarré. Depuis la racine du projet,
si le conteneur de développement a déjà été créé :

```powershell
docker start merlette-postgres
```

Pour une première installation seulement :

```powershell
docker run --name merlette-postgres -e POSTGRES_DB=merlette -e POSTGRES_USER=merlette -e POSTGRES_PASSWORD=merlette_dev -p 127.0.0.1:5432:5432 -v merlette-pgdata:/var/lib/postgresql/data -d postgres:17
```

Dans le terminal backend :

```powershell
cd C:\Users\PRET-ELEVES\merlette\merlette-backend
$env:JAVA_HOME = 'C:\Users\PRET-ELEVES\.jdk\jdk-25'
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://localhost:5432/merlette'
$env:SPRING_DATASOURCE_USERNAME = 'merlette'
$env:SPRING_DATASOURCE_PASSWORD = 'merlette_dev'
.\mvnw.cmd spring-boot:run
```

Ces identifiants sont exclusivement destinés au développement local. Flyway crée
les tables, les huit catégories et les espèces au premier démarrage sur une base vide.
La migration ne contient ni `DROP`, ni compte administrateur avec mot de passe connu.
Ne pas exécuter `docs/schema.sql` : ce script historique supprime les tables.
Si une base contient déjà ce schéma sans historique Flyway, utiliser une nouvelle
base vide ou préparer une migration d'adoption après vérification de sa structure.
Ne pas activer automatiquement `baseline-on-migrate` pour masquer une divergence.

Dans un deuxième terminal :

```powershell
cd C:\Users\PRET-ELEVES\merlette\merlette-frontend
if (!(Test-Path .env)) { Copy-Item .env.example .env }
npm.cmd ci
npm.cmd run dev
```

Ouvrir http://localhost:5173. La configuration CORS autorise cette origine exacte.
Pour un autre hôte/port, définir `FRONTEND_URL` dans le terminal backend.
`JWT_SECRET` accepte une clé d'au moins 32 octets UTF-8. Sans cette variable,
le backend génère une clé temporaire sûre ; les sessions expirent au redémarrage.
Les JWT expirent après 24 heures. La déconnexion efface le token du navigateur ;
elle ne révoque pas les JWT déjà émis côté serveur. Le token est conservé dans
localStorage pour ce prototype ; ce choix exige d'éviter les injections de scripts.

## Vérification automatisée

Les tests HTTP utilisent une **base distincte** sur le port 55432 et ne ciblent pas
la base de développement. Première création :

```powershell
docker run --name merlette-dev-check -e POSTGRES_DB=merlette_test -e POSTGRES_USER=merlette -e POSTGRES_PASSWORD=merlette_test -p 127.0.0.1:55432:5432 -d postgres:17
```

Si elle existe déjà : `docker start merlette-dev-check`. Attendre que PostgreSQL
soit prêt avec `docker logs merlette-dev-check`, puis :

```powershell
cd C:\Users\PRET-ELEVES\merlette\merlette-backend
$env:JAVA_HOME = 'C:\Users\PRET-ELEVES\.jdk\jdk-25'
.\mvnw.cmd test

cd ..\merlette-frontend
npm.cmd run lint
npm.cmd run build
```

Les tests couvrent le parcours HTTP réel avec PostgreSQL : inscription, doublon,
validation, connexion, JWT invalide, protection des routes, catégories, publication
multipart, récupération des octets de la photo, consultation, filtre, liste personnelle,
date future, faux fichier image et pagination invalide. Un test séparé vérifie que
les fichiers sont nettoyés après rollback. Chaque exécution crée un compte unique
dans la base de test ; elle conserve ses données. Photos de test : `target/test-uploads`.
Variables de remplacement : `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME`,
`TEST_DATABASE_PASSWORD` (toujours une base de test dédiée).

## Scénario Postman et démonstration

Avec l'application démarrée sur 8080, depuis `postman/` :

```powershell
npx.cmd newman run .\lot1.postman_collection.json
```

Ou importer la collection dans Postman, choisir `postman/` comme répertoire de
travail et lancer les requêtes dans l'ordre. Pour relancer le Runner avec ses
variables persistées, vider `runId` pour générer un nouveau compte.

Parcours navigateur : créer un compte, publier une photo avec catégorie et date,
consulter son détail, revenir au fil, filtrer par catégorie, ouvrir « Mes observations »,
se déconnecter et vérifier la redirection vers la connexion lors d'une publication.

Deux diapositives possibles :

1. Contrat HTTP et flux React → contrôleur → service transactionnel → repositories → PostgreSQL.
2. Preuves : 201 valide, 400 invalide, 401 sans token, 409 doublon et nettoyage sur rollback.

Avantages démontrables : contraintes vérifiées avant insertion, mots de passe hachés
BCrypt coût 10, auteur déduit du JWT plutôt que choisi par le client, pagination
bornée, nom de photo généré côté serveur et compensation des fichiers si la transaction échoue.
Les types d'images sont reconnus par leur signature binaire plutôt que le seul nom/MIME
annoncé. Ce contrôle n'est pas une analyse antivirus ou une validation intégrale du décodage.

## API et exemples JSON

| Méthode | URL | Accès | Réponse |
|---|---|---|---|
| GET | `/api/health` | Public | 200 `{ "status": "ok" }` |
| POST | `/api/auth/register` | Public | 201 `{ token, user }` |
| POST | `/api/auth/login` | Public | 200 `{ token, user }` |
| GET | `/api/auth/me` | JWT | 200 utilisateur privé |
| GET | `/api/categories` | Public | 200 tableau de catégories |
| POST | `/api/observations` | JWT | 201 observation et photos |
| GET | `/api/observations` | Public | 200 `{ data, pagination }` |
| GET | `/api/observations/{id}` | Public | 200 observation ou 404 |
| GET | `/api/users/me/observations` | JWT | 200 fil personnel paginé |
| GET | `/uploads/{fichier}` | Public | Image |

Inscription :

```json
{ "username": "lea", "email": "lea@example.org", "password": "Secret123!" }
```

Connexion : mêmes champs email/password, sans username. Le profil privé contient
id, username, email, role, bio, avatarUrl et createdAt. Aucun hash n'est exposé.

Publication multipart : `categoryId`, `observedAt` (YYYY-MM-DD) et 1 à 5 fichiers
`photos` obligatoires ; `title` (120 caractères), `description` (10000),
`locationName` (200), latitude [-90,90], longitude [-180,180] optionnels.
Chaque image JPEG/PNG/WEBP est limitée à 5 Mo. Date future interdite.

Le fil accepte `category` (slug), `status` (NEEDS_ID/IDENTIFIED), `userId`,
`sort` (recent/oldest), `page` (≥1), `limit` (1–50, défaut 20).

```json
{
  "data": [],
  "pagination": { "page": 1, "limit": 20, "total": 0, "totalPages": 0 }
}
```

Une observation contient id, title, description, status, category {id,name,slug},
author {id,username}, observedAt, locationName, latitude, longitude, coverUrl,
photos [{id,url,position}], createdAt et updatedAt. Les champs d'identification et
les compteurs ne sont pas encore implémentés ; ils seront ajoutés avec les stories correspondantes.

Erreur uniforme :

```json
{ "error": { "code": "VALIDATION_ERROR", "message": "Les données envoyées sont invalides.", "details": [{ "field": "email", "message": "Email invalide" }] } }
```

## Découpage et estimations initiales

Estimations indicatives en heures-personne pour l'équipe, à confronter au temps
réel de vos séances. Elles ne représentent pas le temps effectivement passé.

| Stories | Tâche | Estimation | État du code |
|---|---|---:|---|
| Socle | Migration, configuration et erreurs | 3 h | Implémenté |
| #11/#10 | API d'authentification et sécurité JWT | 4 h | Implémenté |
| #11/#10 | Formulaires et session React | 3 h | Implémenté |
| #1/#13 | Publication transactionnelle et stockage photo | 5 h | Implémenté |
| #8/#9/#15 | Fil, catégories, pagination, détail | 4 h | Implémenté pour ce lot |
| #14 | Liste personnelle | 1 h | Implémenté ; édition/suppression à faire |
| Transverse | Tests, scénario Postman et documentation | 3 h | Ajoutés |
| Transverse | Relecture collective et démonstration navigateur | 2 h | À faire en séance |

Faire tourner les rôles frontend/backend/tests entre les trois étudiants.
Avant le commit de séance : chacun doit pouvoir expliquer le chemin d'une requête,
les règles de validation et le rôle des tests. Compléter les diagrammes UML
existants selon les classes effectivement implémentées et enregistrer les temps réels.
