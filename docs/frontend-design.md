# Frontend Merlette — Design et intégration

## Direction visuelle

Le PRD, le contrat `05-api.pdf` et les parcours de `design.pdf` servent de référence.
L’interface adopte l’esprit d’un carnet de terrain : ivoire, vert forêt et sauge,
titres en Georgia, texte système sans dépendance à une police distante, illustrations
botaniques SVG locales et photographies des observations mises en avant.
Les noms, images, statuts et compteurs des observations proviennent de l’API.
Aucun jeu de démonstration n’est injecté dans l’application.

Le frontend reste React + TypeScript + Vite. Le design est défini dans
`merlette-frontend/src/index.css`, avec des variables de couleur et des composants
réutilisables. Tailwind fournit le socle CSS ; la nouvelle interface n’utilise plus
le thème automatique daisyUI. Les dépendances existantes sont conservées.

## Pages et interactions

| Route | Interface et comportement |
|---|---|
| `/` | Accueil, catégories, statut, tri, filtres dans l’URL, pagination, cartes |
| `/login`, `/register` | Validation HTML, affichage du mot de passe, erreurs API, session JWT et retour au parcours demandé |
| `/observations/new` | Glisser-déposer ou sélection de 1–5 images, aperçus, retrait, champs validés, publication multipart et confirmation |
| `/observations/:id` | Galerie avec agrandissement, auteur, date, lieu, description, propositions, votes, discussion et actions d’auteur |
| `/me/observations` | Espace personnel, observations de l’utilisateur, filtres et pagination |
| `/me/conversations` | Discussions, filtres « mes observations » / « mes contributions » et liens vers les observations |
| `/notifications` | Toutes/non lues, pagination, marquage individuel avant ouverture, tout marquer comme lu |
| `/users/:id` | Profil public, observations récentes, statistiques et personnalisation du propre profil |
| `/admin` | Catégories réelles, formulaires d’ajout de catégories/espèces, recherche du référentiel, consultation des dernières observations pour modération |
| `/especes` | Recherche différée de 300 ms, catégorie, noms communs/scientifiques |
| `/comment-ca-marche` | Présentation du parcours et de la règle de consensus |
| Autres routes | Page 404 avec retour vers les observations |

Toutes les pages personnelles exigent une session. L’administration exige le rôle
ADMIN côté interface ; le contrôle des autorisations doit aussi rester côté serveur.
Les discussions du PRD désignent les fils publics des observations, pas une messagerie privée.
Il n’existe pas d’API de signalements ni de modification/suppression du référentiel
dans le contrat fourni : aucune action fictive n’a été ajoutée pour ces fonctions.

Les états de chargement, vide, erreur, confirmation, accès réservé et session expirée
sont traités. Les actions destructives sur les observations et commentaires utilisent
une boîte de confirmation native `<dialog>` avec gestion du focus. Les champs
possèdent des labels, les boutons d’icônes des noms accessibles, les filtres un état
`aria-pressed`, et les animations respectent `prefers-reduced-motion`.
La navigation comporte un lien d’évitement et un menu mobile. Au changement de page,
le focus revient au contenu principal. Les listes et formulaires sont adaptés aux
écrans de 390 px et aux écrans de bureau ; les très petits écrans passent en une colonne.

## Modules disponibles et contrats préparés

Le backend actuel fournit les comptes, catégories, publication, fil, détail et suppression des
observations. La suppression est disponible pour l’auteur ou un administrateur avec
confirmation ; l’édition reste un module distinct. Le frontend prépare également les autres parcours du contrat, mais
leurs fonctions sont désactivées par défaut pour éviter les requêtes vers des endpoints
non implémentés. Les pages concernées affichent des messages adaptés, sans faux
compteurs ni fausse confirmation. La validation avec les fixtures navigateur ne
constitue pas un test de leur future implémentation Spring.

| Option Vite | Endpoints nécessaires avant activation |
|---|---|
| Aucun drapeau | POST `/auth/register`, POST `/auth/login`, GET `/auth/me`, GET `/categories`, GET/POST `/observations`, GET `/observations/{id}`, GET `/uploads/{fichier}` |
| `VITE_IDENTIFICATION_ENABLED=true` | GET/POST `/species`, GET/POST `/observations/{id}/proposals`, DELETE `/proposals/{id}`, PUT/DELETE `/observations/{id}/vote` |
| `VITE_COMMENTS_ENABLED=true` | GET/POST `/observations/{id}/comments`, PATCH/DELETE `/comments/{id}` |
| `VITE_NOTIFICATIONS_ENABLED=true` | GET `/notifications`, GET `/notifications/unread-count`, PATCH `/notifications/{id}/read`, PATCH `/notifications/read-all` |
| `VITE_PROFILES_ENABLED=true` | GET `/users/{id}`, PATCH `/users/me` |
| `VITE_CONVERSATIONS_ENABLED=true` | GET `/users/me/conversations` |
| `VITE_MANAGEMENT_ENABLED=true` | PATCH/DELETE `/observations/{id}`, POST `/observations/{id}/photos`, DELETE `/observations/{id}/photos/{photoId}` |
| `VITE_DELETION_ENABLED=true` (défaut) | DELETE `/observations/{id}` ; indépendant de l’édition, confirmation, contrôle auteur/admin côté serveur |
| `VITE_ADMINISTRATION_ENABLED=true` | POST `/categories`, POST `/species` avec contrôle du rôle ADMIN |

Les fonctions HTTP se trouvent dans `src/api/`, les types JSON dans `src/types/` et
les options dans `src/config/features.ts`. L’exemple de configuration contient toutes
les options à `false`. Pour activer un module après implémentation de son API,
modifier `.env`, puis redémarrer Vite. Ces options ne sont ni un mécanisme de sécurité
ni une autorisation de l’utilisateur ; elles ne contrôlent que la disponibilité de l’interface.

Le profil propre à l’utilisateur affiche déjà les informations de session et les
observations réelles. Sans API de profil public, le nom d’un autre auteur peut être
déduit de ses observations publiques ; les champs inconnus et statistiques non
disponibles restent absents ou affichés « — ».

## Points d’attention pour le backend

- Port de développement : **8080**, origine frontend : **http://localhost:5173**.
  La base d’API se configure par `VITE_API_URL`. Par défaut `/api` utilise le proxy
  Vite ; `MERLETTE_API_TARGET` choisit le backend, en local ou sur le réseau partagé.
  `/uploads` est transmis au même serveur. Les exemples historiques en 3000
  dans les PDF ne correspondent pas au backend Spring actuel. Vite réserve le port
  5173 et ne bascule pas silencieusement sur un autre port.
- Avant d’activer les modules d’édition et de vote, étendre les méthodes CORS du
  backend à PATCH et PUT en plus de GET, POST, DELETE et OPTIONS. Les tests navigateur
  avec fixtures ne valident pas la configuration CORS du futur backend.
- Les routes personnelles réutilisent l’identité de session. L’auteur d’une publication
  doit toujours être déduit du JWT côté serveur, jamais d’un champ envoyé par React.
- Le fil conserve les paramètres `category`, `status`, `sort`, `page`, `limit` et
  éventuellement `userId`. La page personnelle utilise le même fil filtré par `userId`,
  ce qui permet la combinaison des filtres avec le backend déjà disponible.
- `photos`, `proposalCount`, `commentCount` et `identifiedSpecies` sont compatibles
  avec le format du PDF. Les champs non encore fournis sont optionnels dans les types.
  Le détail accepte la liste de photos ; une couverture seule reste affichable.
- Les images sont résolues à partir de l’origine de l’API. Avec `/api`, elles passent
  par le proxy `/uploads` de Vite ; avec une URL absolue, elles utilisent l’origine
  du backend. Les URL non HTTP(S) ne sont pas affichées.
- Les requêtes ont un délai maximal de 20 secondes et les listes conservent leur
  état pendant le changement de filtre. Les réponses invalidantes rafraîchissent
  les caches correspondants (liste, détail, commentaires, propositions, notifications).
- Le JWT n’est pas envoyé aux endpoints de connexion/inscription : un ancien token
  expiré ne doit pas empêcher de se reconnecter. Une réponse 401 sur les autres
  endpoints efface la session. Les réponses tardives d’une ancienne restauration
  de session sont annulées ou ignorées après déconnexion/connexion.
- Aucun changement de rôle ou d’autorisation n’est effectué par le frontend.
- Pour les votes, le frontend affiche les valeurs de consensus du serveur et ne
  remplace pas son calcul métier. Le texte explicatif reprend les seuils du PRD.

## Lancer et vérifier

Depuis `merlette-frontend/` :

```powershell
npm.cmd run dev
npm.cmd run lint
npm.cmd run build
```

Le serveur Spring et PostgreSQL restent nécessaires pour l’utilisation réelle.
Les fonctionnalités additionnelles ne nécessitent aucune donnée fictive dans la base.

### Tests d’interface dans Chrome

`scripts/frontend-smoke.mjs` utilise le protocole de développement de Chrome et
les API standard de Node 24. Il ne nécessite pas Playwright. Les fixtures HTTP
sont interceptées **uniquement dans l’onglet de test**, et les illustrations et
comptes de test restent dans ce script. Aucune écriture n’est envoyée à PostgreSQL.
Le profil Chrome isolé, les captures et les rapports ne sont pas suivis par Git.

Premier terminal, depuis `merlette-frontend/`, pour les parcours complets :

```powershell
$env:VITE_IDENTIFICATION_ENABLED='true'
$env:VITE_COMMENTS_ENABLED='true'
$env:VITE_NOTIFICATIONS_ENABLED='true'
$env:VITE_PROFILES_ENABLED='true'
$env:VITE_CONVERSATIONS_ENABLED='true'
$env:VITE_MANAGEMENT_ENABLED='true'
$env:VITE_ADMINISTRATION_ENABLED='true'
npm.cmd run dev -- --host 127.0.0.1 --port 5178 --strictPort
```

Deuxième terminal, depuis le même dossier :

```powershell
$browserProfile = Join-Path (Get-Location) '.preview-browser'
Start-Process -FilePath 'C:\Program Files\Google\Chrome\Application\chrome.exe' -ArgumentList '--headless=new','--remote-debugging-port=9223',"--user-data-dir=$browserProfile",'--no-first-run','--no-default-browser-check','about:blank' -WindowStyle Hidden
node scripts/frontend-smoke.mjs
```

Le serveur de test peut utiliser `VITE_API_URL=/api` ou `http://localhost:8080/api` :
le script intercepte les deux configurations. Les actions simulées ne doivent pas être essayées dans
un autre navigateur sans ces interceptions si le backend est incomplet.

Pour vérifier aussi la configuration livrée (toutes les options à `false`), compiler
depuis un autre terminal sans les variables d’activation, puis servir la compilation :

```powershell
npm.cmd run build
npm.cmd run preview -- --host 127.0.0.1 --port 5179 --strictPort
```

Dans le terminal de test :

```powershell
$env:PREVIEW_URL='http://127.0.0.1:5179'
$env:COMMUNITY_ENABLED='false'
node scripts/frontend-smoke.mjs
```

Les rapports sont dans `test-artifacts/complete/results.json` et
`test-artifacts/core/results.json`. Les captures d’écran voisines permettent
de revoir l’accueil, le détail, la publication et les autres pages. La suite vérifie
les filtres, états vide/erreur, inscription, routes protégées, galerie, publication
multipart et retrait d’aperçu, votes, recherche d’espèce, commentaire, édition,
profil, administration, notifications, déconnexion et absence de débordement horizontal.
Le mode `core` vérifie aussi qu’aucun endpoint communautaire désactivé n’est appelé.

Après les vérifications, arrêter les serveurs de test avec Ctrl+C et fermer le
Chrome de test, en conservant les serveurs habituels de développement si nécessaire.

## Swagger comme référence de contrat

La documentation fournie est `http://10.86.216.74:8080/docs`, pour une API dont la
base est `http://10.86.216.74:8080/api`. Ces adresses servent de référence pour
préparer les fonctions HTTP. Elles ne constituent pas une demande de connexion
au serveur partagé. Le schéma JSON OpenAPI n’a pas pu être récupéré : la concordance
exacte avec ce Swagger reste à vérifier, sans assimiler le PDF historique à ce schéma.

La configuration locale reste :

```dotenv
VITE_API_URL=/api
MERLETTE_API_TARGET=http://localhost:8080
```

Redémarrer Vite après modification. Si une connexion au serveur partagé est souhaitée
plus tard, remplacer explicitement `MERLETTE_API_TARGET` par son origine.
Le proxy simplifie les requêtes navigateur entre les deux origines ; il ne rend pas
accessible un serveur arrêté ou un réseau injoignable, et ne remplace pas les
autorisations du backend. Le poste qui exécute Vite doit pouvoir joindre le serveur.
En déploiement statique, le proxy Vite n’existe pas : configurer un reverse proxy
`/api` et `/uploads`, ou une base d’API absolue avec CORS et HTTPS adaptés.

Vérification indépendante du proxy (backend jetable local, aucune écriture distante) :

```powershell
node scripts/proxy-smoke.mjs
```

Ce test démarre son propre Vite sur 5180, vérifie le transfert des chemins, paramètres,
corps POST et images, puis ferme ses serveurs. Il ne valide pas les contrats du
serveur partagé. À l’intégration du serveur partagé, relire son schéma OpenAPI avant
d’activer les modules et confronter les réponses réelles aux types JSON du frontend.
