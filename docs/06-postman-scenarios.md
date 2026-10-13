# Merlette — Scénarios de test Postman

Collection prête à importer : [`postman/merlette.postman_collection.json`](../postman/merlette.postman_collection.json) — **85 requêtes, 9 dossiers**, toutes avec assertions automatiques.

## Lancer les tests

**Prérequis**
1. Base initialisée : `psql -d merlette -f docs/schema.sql` (crée le compte `admin@merlette.local` / `Admin1234`).
2. API démarrée sur `http://localhost:3000` (modifiable via les variables `baseUrl` et `serverUrl`).

**Dans Postman**
1. *Import* → `postman/merlette.postman_collection.json`.
2. *Settings → General → Working directory* : choisir le dossier `postman/` (pour les fichiers de `fixtures/`).
3. Clic droit sur la collection → *Run collection* → garder l'ordre → *Run*.

**En ligne de commande (Newman)**
```bash
npx newman run postman/merlette.postman_collection.json --working-dir postman
```

La collection est **rejouable** : chaque exécution crée des utilisateurs uniques (`lea_<runId>`, …) et supprime ses observations à la fin.

## Tests communs (appliqués à chaque requête)

- Temps de réponse < 500 ms (NFR-05).
- Si statut ≥ 400 : le corps respecte le format `{ "error": { "code", "message" } }` (NFR-03).

## Acteurs

| Variable | Utilisateur | Rôle dans les scénarios |
|---|---|---|
| `tokenA` | Lea | Observatrice (auteure de l'observation) |
| `tokenB` | Karim | Identifieur, propose la Mésange bleue, commente |
| `tokenC` | Sam | Identifieur, change d'avis, commente |
| `tokenD` | Tom | Identifieur, propose la Mésange charbonnière |
| `tokenAdmin` | admin | Modération et référentiel |

---

## 00 · Préparation

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T00 | L'API répond | `GET /health` | 200 ; génère `runId` |

## 01 · Authentification (#10, #11)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T01–T04 | Inscription de Lea, Karim, Sam, Tom | `POST /auth/register` | 201, token présent, rôle USER, pas de mot de passe dans la réponse |
| T05 | Email déjà utilisé | `POST /auth/register` | 409 `CONFLICT` |
| T06 | Mot de passe < 8 caractères | `POST /auth/register` | 400 `VALIDATION_ERROR` |
| T07 | Email invalide | `POST /auth/register` | 400 `VALIDATION_ERROR` |
| T08 | Connexion valide | `POST /auth/login` | 200, token |
| T09 | Mauvais mot de passe | `POST /auth/login` | 401 `INVALID_CREDENTIALS` |
| T10 | Email inconnu | `POST /auth/login` | 401 `INVALID_CREDENTIALS` (même message que T09) |
| T11 | Utilisateur courant | `GET /auth/me` | 200, id = Lea |
| T12 | Sans token | `GET /auth/me` | 401 `UNAUTHORIZED` |
| T13 | Token invalide | `GET /auth/me` | 401 `UNAUTHORIZED` |
| T14 | Connexion admin | `POST /auth/login` | 200, rôle ADMIN |

## 02 · Référentiel (#9, #16, #20)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T15 | Lister les catégories | `GET /categories` | 200, ≥ 8 catégories dont `oiseaux` |
| T16 | Recherche sans accent | `GET /species?q=mesange&category=oiseaux` | 200, contient *Cyanistes caeruleus* et *Parus major* |
| T17 | Recherche par nom scientifique | `GET /species?q=Parus` | 200, contient *Parus major* |
| T18 | Recherche trop courte | `GET /species?q=m` | 400 |
| T19 | Utilisateur crée une espèce | `POST /species` | 403 `FORBIDDEN` |
| T20 | Admin crée une espèce | `POST /species` | 201 |

## 03 · Observations (#1, #8, #13, #14, #15)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T21 | Publier sans être connecté | `POST /observations` | 401 |
| T22 | Publier sans photo | `POST /observations` | 400 |
| T23 | Publier un fichier `.txt` | `POST /observations` | 400 |
| T24 | Date d'observation dans le futur | `POST /observations` | 400 |
| T25 | Sans catégorie | `POST /observations` | 400 |
| T26 | **Lea publie une observation** | `POST /observations` (multipart) | 201, statut `NEEDS_ID`, 1 photo, auteur = Lea |
| T27 | Lea publie une 2e observation | `POST /observations` | 201 |
| T28 | La photo est servie | `GET /uploads/...` | 200, `Content-Type: image/*` |
| T29 | Fil récent paginé | `GET /observations?page=1&limit=20` | 200, ≤ 20 éléments, trié par date décroissante, pagination présente |
| T30 | Filtre catégorie + statut | `GET /observations?category=oiseaux&status=NEEDS_ID` | Uniquement oiseaux `NEEDS_ID`, contient l'observation |
| T31 | Filtre autre catégorie | `GET /observations?category=plantes` | L'observation est absente |
| T32 | Détail | `GET /observations/:id` | 200, photos, propositions, catégorie, auteur, statut |
| T33 | Observation inexistante | `GET /observations/999999` | 404 |
| T34 | Karim modifie l'observation de Lea | `PATCH /observations/:id` | 403 |
| T35 | Lea modifie son observation | `PATCH /observations/:id` | 200, titre mis à jour |
| T36 | Karim ajoute une photo chez Lea | `POST /observations/:id/photos` | 403 |

## 04 · Identification et consensus (#17, #4, #5)

Scénario central : il vérifie la règle **≥ 3 votes ET ≥ 2/3 des votes**.

| ID | Scénario | Votes après l'action | Résultat attendu |
|---|---|---|---|
| T37 | Proposer sans token | — | 401 |
| T38 | Proposer sans `speciesId` | — | 400 |
| T39 | Espèce inexistante | — | 404 |
| T40 | **Karim propose Mésange bleue** | Bleue 1 | 201, la proposition a 1 vote, `NEEDS_ID` |
| T41 | Sam propose la même espèce | Bleue 1 | 409 `CONFLICT` |
| T42 | **Sam vote Mésange bleue** | Bleue 2 | 200, `NEEDS_ID` (< 3 votes) |
| T43 | Sam revote pareil | Bleue 2 | 200, inchangé (idempotent) |
| T44 | **Lea vote Mésange bleue** | Bleue 3 | 200, **`IDENTIFIED`**, espèce retenue = Mésange bleue |
| T45 | Consulter les votes | Bleue 3 | 3 votes, 100 %, 3 votants, consensus atteint |
| T46 | **Tom propose Charbonnière** | Bleue 3 · Charbo 1 | 201, reste `IDENTIFIED` (3/4 = 75 %) |
| T47 | Tri des propositions | Bleue 3 · Charbo 1 | Votes `[3, 1]`, pourcentages `[75, 25]` |
| T48 | **Sam déplace son vote** vers Charbonnière | Bleue 2 · Charbo 2 | 200, repasse **`NEEDS_ID`**, espèce retenue = null |
| T49 | Un seul vote par utilisateur | Bleue 2 · Charbo 2 | Total 4, Sam absent des votants de Bleue |
| T50 | **Sam retire son vote** | Bleue 2 · Charbo 1 | 200, `NEEDS_ID` |
| T51 | Sam retire encore | — | 404 |
| T52 | Voter sans token | — | 401 |
| T53 | Voter pour une proposition d'une autre observation | — | 404 |
| T54 | **Sam revote Mésange bleue** | Bleue 3 · Charbo 1 | 200, **`IDENTIFIED`** |
| T55 | Le fil reflète le statut | — | `?status=IDENTIFIED` contient l'observation |
| T56 | Tom supprime sa proposition (seul votant) | Bleue 3 | 204 |

## 05 · Commentaires (#3, #18)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T57 | Sans token | `POST /observations/:id/comments` | 401 |
| T58 | Contenu vide (espaces) | idem | 400 |
| T59 | Contenu > 1000 caractères | idem | 400 |
| T60 | Karim commente | idem | 201, auteur = Karim |
| T61 | Sam commente | idem | 201 |
| T62 | Lister | `GET /observations/:id/comments` | 2 commentaires, ordre chronologique |
| T63 | Sam modifie le commentaire de Karim | `PATCH /comments/:id` | 403 |
| T64 | Karim modifie son commentaire | `PATCH /comments/:id` | 200, `edited: true` |

## 06 · Notifications (#6)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T65 | Compteur de Lea | `GET /notifications/unread-count` | ≥ 4 |
| T66 | Notifications de Lea | `GET /notifications?unread=true` | Contient `NEW_PROPOSAL`, `NEW_COMMENT`, `OBSERVATION_IDENTIFIED` ; aucune causée par Lea elle-même |
| T67 | Notifications de Karim | `GET /notifications` | Contient `OBSERVATION_IDENTIFIED` (son espèce retenue) et `NEW_COMMENT` (Sam a répondu) |
| T68 | Sans token | `GET /notifications` | 401 |
| T69 | Sam lit la notification de Lea | `PATCH /notifications/:id/read` | 404 |
| T70 | Lea marque une notification lue | idem | 200, `isRead: true` |
| T71 | Lea marque tout comme lu | `PATCH /notifications/read-all` | 200, `updated ≥ 3` |
| T72 | Compteur de Lea | `GET /notifications/unread-count` | 0 |

## 07 · Profil et discussions (#10, #12, #14)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T73 | Discussions de Karim | `GET /users/me/conversations` | Contient l'observation |
| T74 | Discussions de Tom | idem | 200 |
| T75 | Observations de Lea | `GET /users/me/observations` | Contient ses 2 observations |
| T76 | Profil public de Lea | `GET /users/:id` | `stats.observations = 2`, email non exposé |
| T77 | Modifier sa bio | `PATCH /users/me` | 200, bio mise à jour |

## 08 · Suppression et modération (#14, #18, #19)

| ID | Scénario | Requête | Résultat attendu |
|---|---|---|---|
| T78 | Sam supprime le commentaire de Karim | `DELETE /comments/:id` | 403 |
| T79 | Karim supprime son commentaire | idem | 204 |
| T80 | Karim supprime l'observation de Lea | `DELETE /observations/:id` | 403 |
| T81 | Admin supprime la 2e observation | idem | 204 |
| T82 | Lea supprime son observation | idem | 204 |
| T83 | Observation supprimée | `GET /observations/:id` | 404 |
| T84 | Suppression en cascade | `GET /observations/:id/proposals` | 404 |

## Couverture

| Story | Tests |
|---|---|
| #1 Poster des images | T21–T28, T36 |
| #3 Commenter | T57–T62 |
| #4 Voter | T42–T44, T48–T54 |
| #5 Consulter les votes | T45, T47, T49 |
| #6 Notifications | T65–T72 |
| #8 Récentes | T29 |
| #9 Filtrer par catégorie | T15, T30, T31, T55 |
| #10 Connexion / discussions | T08–T14, T73, T74 |
| #11 Inscription | T01–T07 |
| #12 Profil | T76, T77 |
| #13 Décrire l'observation | T24, T25, T26 |
| #14 Gérer mes observations | T34, T35, T75, T80, T82 |
| #15 Détail | T32, T33 |
| #16 Rechercher une espèce | T16–T18 |
| #17 Proposer une espèce | T37–T41, T46, T56 |
| #18 Modifier mes commentaires | T63, T64, T78, T79 |
| #19 Modération | T81 |
| #20 Référentiel | T19, T20 |
