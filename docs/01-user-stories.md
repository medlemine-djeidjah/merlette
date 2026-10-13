# Merlette — User Stories

Plateforme communautaire d'observation naturaliste (type *iNaturalist*, *Observation.org*, *Pl@ntNet*).
Dans le métier, on parle de **plateforme de science participative** (*citizen science*) avec **identification communautaire** (*crowdsourced species identification*).

## Rôles

| Rôle | Description |
|---|---|
| **Visiteur** | Personne non connectée. Peut consulter les observations. |
| **Utilisateur** | Personne connectée. Chaque utilisateur est à la fois **observateur** et **identifieur**. |
| **Observateur** | Utilisateur qui publie une observation (photos + infos) pour la faire identifier. |
| **Identifieur** | Utilisateur qui propose des espèces, vote et commente pour aider à identifier. |
| **Administrateur** | Modère le contenu et gère le référentiel (catégories, espèces). |

## Priorités (MoSCoW) et estimation

- **Must** : indispensable au prototype.
- **Should** : important, à faire si le temps le permet.
- **Could** : bonus.
- **Won't** : hors périmètre du prototype.

Estimation en points (suite de Fibonacci : 1, 2, 3, 5, 8).

> Les numéros #2 et #7 n'apparaissent pas dans les captures du tableau. Les nouvelles stories commencent à #11.

---

## Epic A — Compte et authentification

### #11 — Créer un compte · Must · 2 pts
**En tant que** visiteur, **je veux** créer un compte **afin de** pouvoir publier des observations et participer aux identifications.

Critères d'acceptation :
- Le formulaire demande : nom d'utilisateur, email, mot de passe (8 caractères minimum).
- Si l'email ou le nom d'utilisateur est déjà utilisé, un message d'erreur clair s'affiche.
- Le mot de passe est stocké haché (bcrypt), jamais en clair.
- Après inscription, l'utilisateur est connecté automatiquement.

### #10 — Se connecter et retrouver mes conversations · Must · 3 pts *(existante, complétée)*
**En tant qu'**utilisateur, **je veux** pouvoir m'identifier **afin de** retrouver mes conversations.

Critères d'acceptation :
- Je me connecte avec email + mot de passe.
- Si les identifiants sont faux, un message « Email ou mot de passe incorrect » s'affiche (sans préciser lequel).
- Une fois connecté, une page **« Mes discussions »** liste les observations où j'ai commenté, proposé une espèce ou que j'ai publiées, triées par dernière activité.
- La session reste active (jeton JWT valide 24 h).
- Je peux me déconnecter.

### #12 — Consulter et modifier mon profil · Should · 2 pts
**En tant qu'**utilisateur, **je veux** consulter et modifier mon profil **afin de** présenter mon expérience aux autres.

Critères d'acceptation :
- Le profil affiche : nom d'utilisateur, bio, date d'inscription, nombre d'observations, nombre d'identifications proposées.
- Je peux modifier ma bio et mon avatar.
- Le profil d'un autre utilisateur est consultable en lecture seule.

---

## Epic B — Observations

### #1 — Poster des images · Must · 5 pts *(existante, complétée)*
**En tant qu'**observateur, **je veux** pouvoir poster des images **afin de** pouvoir recevoir une identification.

Critères d'acceptation :
- Je dois être connecté.
- Je peux ajouter de 1 à 5 photos (JPEG/PNG/WEBP, 5 Mo max chacune).
- Une observation sans photo est refusée.
- Après publication, l'observation apparaît en tête de la liste « Récentes » avec le statut **« À identifier »**.

### #13 — Décrire mon observation · Must · 2 pts
**En tant qu'**observateur, **je veux** renseigner la catégorie, la date, le lieu et une description **afin d'**aider les identifieurs.

Critères d'acceptation :
- Champs obligatoires : catégorie, date d'observation.
- Champs facultatifs : titre, description, nom du lieu, latitude/longitude.
- La date d'observation ne peut pas être dans le futur.

### #14 — Gérer mes observations · Should · 3 pts
**En tant qu'**observateur, **je veux** modifier ou supprimer mes observations **afin de** corriger une erreur.

Critères d'acceptation :
- Je ne peux modifier/supprimer que **mes** observations (sinon erreur 403).
- La suppression demande une confirmation et supprime aussi photos, propositions, votes et commentaires.
- Une page « Mes observations » liste mes publications avec leur statut.

### #15 — Voir le détail d'une observation · Must · 3 pts
**En tant qu'**utilisateur, **je veux** voir le détail d'une observation **afin de** l'analyser avant d'identifier.

Critères d'acceptation :
- La page affiche : photos (galerie), auteur, catégorie, date, lieu, description, statut, espèce retenue (si identifiée), propositions avec leurs votes, commentaires.
- La page est accessible aux visiteurs (lecture seule).

---

## Epic C — Découverte des observations

### #8 — Voir les demandes d'identification récentes · Must · 2 pts *(existante, complétée)*
**En tant qu'**identifieur, **je veux** voir les requêtes d'identification les plus récentes **afin d'**aider à identifier.

Critères d'acceptation :
- La page d'accueil liste les observations triées de la plus récente à la plus ancienne.
- Chaque carte montre : 1re photo, catégorie, date, auteur, statut, nombre de propositions.
- Pagination de 20 observations par page.
- Un filtre « À identifier uniquement » est disponible.

### #9 — Filtrer par catégorie · Must · 2 pts *(existante, complétée)*
**En tant qu'**identifieur, **je veux** filtrer les observations par catégorie **afin de** trouver des observations que je peux identifier.

Critères d'acceptation :
- Les catégories disponibles : Plantes, Oiseaux, Insectes, Mammifères, Reptiles & amphibiens, Champignons, Poissons, Autres.
- Le filtre se combine avec le filtre de statut et le tri.
- Le filtre choisi est visible dans l'URL (ex. `?category=oiseaux`) pour pouvoir partager le lien.

### #16 — Rechercher une espèce · Should · 2 pts
**En tant qu'**identifieur, **je veux** rechercher une espèce par nom commun ou scientifique **afin de** la proposer rapidement.

Critères d'acceptation :
- L'autocomplétion se déclenche à partir de 2 caractères.
- Les résultats peuvent être restreints à la catégorie de l'observation.

---

## Epic D — Identification communautaire

### #17 — Proposer une espèce · Must · 3 pts
**En tant qu'**identifieur, **je veux** proposer une espèce pour une observation **afin de** contribuer à son identification.

Critères d'acceptation :
- Je choisis l'espèce dans le référentiel (#16).
- Une même espèce ne peut être proposée qu'une seule fois par observation (si elle existe déjà, je vote pour elle).
- Proposer une espèce compte automatiquement comme mon vote pour cette espèce.
- L'auteur de l'observation reçoit une notification.

### #4 — Voter sur l'espèce · Must · 3 pts *(existante, complétée)*
**En tant qu'**identifieur, **je veux** voter sur l'espèce **pour** déterminer une identification.

Critères d'acceptation :
- Je dois être connecté.
- J'ai **un seul vote par observation** : voter pour une autre proposition déplace mon vote.
- Je peux retirer mon vote.
- Après chaque vote, la règle de consensus est recalculée (voir PRD §6.1) :
  une proposition avec **≥ 3 votes** et **≥ 2/3 des votes** devient l'identification retenue, et l'observation passe au statut **« Identifiée »**.
- Si le consensus est perdu (votes retirés/déplacés), l'observation repasse « À identifier ».

### #5 — Consulter les votes · Must · 2 pts *(existante, complétée)*
**En tant qu'**identifieur, **je veux** consulter les votes sur les espèces **pour** guider l'identification.

Critères d'acceptation :
- Pour chaque proposition : nom de l'espèce, nombre de votes, pourcentage, liste des votants.
- Les propositions sont triées par nombre de votes décroissant.
- Mon vote actuel est mis en évidence.

### #3 — Commenter les posts · Must · 2 pts *(existante, complétée)*
**En tant qu'**identifieur, **je veux** commenter sur des posts **pour** discuter des idées d'identification.

Critères d'acceptation :
- Je dois être connecté ; un commentaire vide ou > 1000 caractères est refusé.
- Les commentaires sont affichés par ordre chronologique avec auteur et date.
- L'auteur de l'observation et les autres participants à la discussion sont notifiés.

### #18 — Modifier / supprimer mes commentaires · Could · 1 pt
**En tant qu'**utilisateur, **je veux** modifier ou supprimer mes commentaires **afin de** corriger une erreur.

Critères d'acceptation :
- Seul l'auteur (ou un admin) peut modifier/supprimer un commentaire.
- Un commentaire modifié affiche la mention « modifié ».

---

## Epic E — Notifications

### #6 — Être notifié du déroulement de l'identification · Must · 3 pts *(existante, complétée)*
**En tant qu'**observateur, **je veux** être notifié du déroulement de l'identification.

Critères d'acceptation :
- Je reçois une notification **in-app** quand, sur mon observation :
  - une espèce est proposée ;
  - un commentaire est ajouté ;
  - l'observation devient « Identifiée » (avec le nom de l'espèce).
- Une icône cloche affiche le nombre de notifications non lues (rafraîchi toutes les 30 s).
- Cliquer sur une notification ouvre l'observation et la marque comme lue.
- Je peux tout marquer comme lu.
- Je ne suis pas notifié de mes propres actions.

---

## Epic F — Administration

### #19 — Modérer le contenu · Could · 2 pts
**En tant qu'**administrateur, **je veux** supprimer une observation ou un commentaire inapproprié **afin de** garder la plateforme saine.

### #20 — Gérer le référentiel · Could · 3 pts
**En tant qu'**administrateur, **je veux** ajouter des catégories et des espèces **afin de** compléter le référentiel.

---

## Hors périmètre (Won't pour le prototype)

- #21 Carte interactive des observations.
- #22 Identification automatique par IA.
- #23 Notifications par email / push mobile.
- #24 Application mobile native.

---

## Récapitulatif

| # | Story | Epic | Priorité | Pts |
|---|---|---|---|---|
| 11 | Créer un compte | A | Must | 2 |
| 10 | Se connecter / mes discussions | A | Must | 3 |
| 12 | Profil | A | Should | 2 |
| 1 | Poster des images | B | Must | 5 |
| 13 | Décrire l'observation | B | Must | 2 |
| 14 | Gérer mes observations | B | Should | 3 |
| 15 | Détail d'une observation | B | Must | 3 |
| 8 | Observations récentes | C | Must | 2 |
| 9 | Filtrer par catégorie | C | Must | 2 |
| 16 | Rechercher une espèce | C | Should | 2 |
| 17 | Proposer une espèce | D | Must | 3 |
| 4 | Voter | D | Must | 3 |
| 5 | Consulter les votes | D | Must | 2 |
| 3 | Commenter | D | Must | 2 |
| 18 | Modifier mes commentaires | D | Could | 1 |
| 6 | Notifications | E | Must | 3 |
| 19 | Modération | F | Could | 2 |
| 20 | Référentiel | F | Could | 3 |
| | **Total Must** | | | **35** |
| | **Total** | | | **45** |

## Definition of Done

- Code relu par un autre membre de l'équipe.
- Endpoint(s) testé(s) dans la collection Postman (voir `06-postman-scenarios.md`).
- Critères d'acceptation vérifiés manuellement dans l'interface.
- Aucune erreur dans la console navigateur ni dans les logs serveur.
