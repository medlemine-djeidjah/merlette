# Vérification des tâches frontend assignées

Références : les 13 numéros figurant sur les captures de tâches fournies.
`#16` et `#54` concernent la même page d’inscription ; ils sont conservés tous les
deux dans les références de commit, sans les compter comme deux pages distinctes.

## Bilan

Les 13 tâches ont une implémentation frontend. Les parcours correspondants sont
vérifiés dans Chrome, sur ordinateur et à 390 px, avec des réponses HTTP isolées.
Cela valide l’interface et les appels produits, pas une intégration réelle au
serveur partagé. Aucun compte de test n’est créé sur ce serveur et aucune tâche
n’est fermée automatiquement dans l’outil de suivi.

Les contrôleurs et DTO de `origin/feature/backend` ont également été lus au commit
`edea24b` pour confronter les routes, les champs et les réponses. Cette vérification
des sources remplace les suppositions fondées seulement sur le PDF historique ;
elle ne certifie pas que le serveur Swagger déployé utilise exactement ce commit.

| Tâche | Résultat côté frontend | Fichiers principaux | Vérification |
|---|---|---|---|
| #16 — Création de la page d’inscription | Implémenté | `src/pages/RegisterPage.tsx`, `src/components/auth/AuthForm.tsx` | Route `/register`, formulaire et soumission |
| #54 — Page `/register` avec erreurs | Implémenté | `AuthForm.tsx`, `src/api/auth.ts`, `src/api/client.ts` | Champs invalides refusés, erreur API 409 affichée, nouvelle tentative possible |
| #55 — Connexion automatique après inscription | Implémenté | `src/context/AuthContext.tsx`, `AuthForm.tsx` | Token conservé, utilisateur courant mis à jour, retour au parcours demandé |
| #48 — Login, token, déconnexion | Implémenté | `src/pages/LoginPage.tsx`, `AuthForm.tsx`, `AuthContext.tsx`, `src/components/layout/Navbar.tsx` | Erreur d’identifiants, connexion explicite, stockage, déconnexion et redirection protégée |
| #21 — Sélection et aperçu des photos | Implémenté | `src/pages/NewObservationPage.tsx`, `src/components/observations/ObservationForm.tsx` | Sélection, aperçu, retrait, nouvelle sélection et envoi multipart ; glisser-déposer disponible |
| #64 — Catégorie, date, lieu, description, titre | Implémenté | `ObservationForm.tsx`, `src/api/observations.ts` | Champs présents dans la requête ; date/catégorie obligatoires, limites de saisie |
| #40 — Grille photo, catégorie, statut, auteur, compteurs | Implémenté | `src/pages/HomePage.tsx`, `ObservationFeed.tsx`, `ObservationCard.tsx`, `StatusBadge.tsx` | Cartes réelles de la réponse API, états vide/erreur, affichage responsive ; compteurs absents indiqués « — », pas zéro inventé |
| #41 — Pagination et filtre « À identifier » | Implémenté | `ObservationFeed.tsx`, `ObservationFilters.tsx`, `src/components/common/Pagination.tsx` | Suivant/précédent sur plusieurs pages, filtre conservé, rechargement de l’URL, état sans résultat |
| #45 — Catégorie synchronisée avec l’URL | Implémenté | `ObservationFilters.tsx`, `ObservationFeed.tsx` | `category=<slug>` conservé dans l’URL et transmis à l’API, réinitialisation de la page et effacement des filtres |
| #70 — Détail, galerie, informations, statut, espèce retenue | Implémenté | `src/pages/ObservationDetailPage.tsx`, `PhotoGallery.tsx` | Galerie et agrandissement, dates/auteur/lieu, nom commun et scientifique de l’espèce retenue même si un titre existe |
| #49 — Page `/me/conversations` | Implémenté, activation conditionnée au backend choisi | `src/pages/MyConversationsPage.tsx`, `src/api/users.ts` | Liste, liens vers les observations, rôles et filtres de participation, état vide |
| #60 — Profil et édition de son propre profil | Implémenté, activation conditionnée au backend choisi | `src/pages/ProfilePage.tsx`, `src/api/users.ts` | Chargement du profil, observations/statistiques, PATCH du propre profil ; aucun formulaire d’édition sur le profil d’autrui |
| #74 — Autocomplétion à partir de 2 caractères | Implémenté, activation conditionnée au backend choisi | `src/components/identification/SpeciesSearch.tsx`, `src/api/species.ts` | Aucune requête à 1 caractère, recherche à 2 caractères après 300 ms, suggestions, sélection avec flèches et Entrée |

Les chemins de composants abrégés dans le tableau sont relatifs à
`merlette-frontend/src/components/observations/`, sauf indication contraire.

## Dépendances backend à ne pas confondre avec le travail frontend

La branche `main` contient le premier socle Spring, alors que `feature/backend`
contient des contrôleurs supplémentaires. Le frontend laisse ces modules
désactivés par défaut tant que le backend réellement exécuté est le socle initial :

| Tâche | Option | Contrat vérifié dans `feature/backend` |
|---|---|---|
| #49 | `VITE_CONVERSATIONS_ENABLED=true` | GET `/api/users/me/conversations` → `{ data: ConversationDto[] }` |
| #60 | `VITE_PROFILES_ENABLED=true` | GET `/api/users/{id}` → profil public et stats ; PATCH `/api/users/me` → `MeDto` |
| #74 | `VITE_IDENTIFICATION_ENABLED=true` | GET `/api/species?q=&category=&limit=10` → `SpeciesDto[]` |

Avec le backend complet exécuté, définir ces options dans `.env`, puis redémarrer
Vite. Il n’est pas nécessaire de changer d’adresse réseau pour vérifier les
interfaces : les tests utilisent des interceptions HTTP dans un profil Chrome isolé.
Le `.env` local reste dirigé vers localhost ; le serveur partagé n’est pas connecté.

Points de contrat contrôlés et ajustés :

- Le fil reçoit `ObservationSummaryDto` avec `coverUrl` et les compteurs ; le détail
  reçoit `ObservationDetailDto` avec `photos`, sans `coverUrl` obligatoire.
- Le profil public n’expose pas l’email. La session et le PATCH du propre profil
  reçoivent `MeDto` avec id, username, email, role, bio, avatarUrl et createdAt.
- L’avatar vide est envoyé comme chaîne vide pour l’effacer ; `null` signifie
  « ne pas modifier » dans le service backend de référence.
- Les dates du formulaire utilisent `YYYY-MM-DD`, et les fichiers utilisent le
  champ multipart `photos`.
- Les discussions sont des contributions publiques aux observations, pas des messages privés.

## Vérifications reproductibles

Depuis `merlette-frontend/` :

```powershell
npm.cmd run build
npm.cmd run lint
```

Le guide [frontend-design.md](frontend-design.md) décrit les serveurs de test et le
Chrome isolé, puis les commandes pour exécuter `scripts/frontend-smoke.mjs` :

- mode complet : tous les modules frontend activés, backend simulé ;
- mode core : configuration compilée par défaut, vérification de l’absence d’appels
  aux endpoints désactivés.

Les rapports et captures sont dans `test-artifacts/complete/` et
`test-artifacts/core/`, ignorés par Git. Les sources des tests sont versionnées.

## Périmètre de la branche frontend

La branche transporte les sources frontend, ses tests et sa documentation.
Les corrections backend locales (suppression, Java 21 et tests Spring), le document
du lot backend et les changements de collection Postman restent hors de ces commits.
La suppression depuis l’interface est préparée ; son fonctionnement réel nécessite
un backend exposant DELETE `/api/observations/{id}` avec contrôle auteur/admin.
Cet endpoint existe dans `feature/backend` et dans les corrections backend locales.

Les commits regroupent les composants partagés, puis l’authentification, la
publication, les profils/discussions et enfin les preuves de vérification. Chaque
commit contient les numéros des tâches concernées, sans réécrire l’historique de main
et sans fusionner automatiquement les branches.
