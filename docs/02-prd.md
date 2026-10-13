# Merlette — Product Requirements Document (PRD)

| | |
|---|---|
| **Produit** | Merlette — plateforme communautaire d'identification d'espèces |
| **Type** | Prototype fonctionnel (projet scolaire) |
| **Version** | 1.0 |
| **Date** | 13/10/2026 |
| **Statut** | Validé pour développement |

---

## 1. Contexte

Les promeneurs, jardiniers et naturalistes amateurs photographient souvent des plantes, insectes ou oiseaux sans savoir les nommer. Des plateformes comme iNaturalist ou Observation.org montrent qu'une communauté peut identifier ces espèces collectivement : chacun propose une espèce, les autres votent, et un consensus se dégage.

Merlette reproduit ce principe dans une version simple : **un réseau social où l'on publie des observations et où la communauté vote pour déterminer l'espèce**.

## 2. Problème

- Un observateur n'a pas toujours les connaissances pour identifier ce qu'il a vu.
- Une identification par une seule personne peut être fausse.
- Les identifieurs compétents n'ont pas d'outil simple pour trouver les observations de leur domaine.

## 3. Objectifs

| Objectif | Indicateur (mesuré en démo) |
|---|---|
| Publier une observation rapidement | < 1 minute de la page d'accueil à la publication |
| Obtenir une identification fiable | Consensus automatique dès 3 votes concordants à ≥ 2/3 |
| Faciliter le travail des identifieurs | Filtre par catégorie + statut en 1 clic |
| Tenir l'observateur informé | Notification in-app à chaque étape |

**Non-objectifs** : passage à l'échelle, application mobile, IA de reconnaissance, carte interactive.

## 4. Personas

**Léa, 24 ans — observatrice débutante.** Prend des photos en randonnée. Veut savoir ce qu'elle a vu sans connaître la botanique. Utilise surtout son téléphone.

**Karim, 45 ans — identifieur passionné.** Ornithologue amateur. Veut voir uniquement les oiseaux non identifiés et donner son avis rapidement.

**Admin (enseignant / équipe).** Veut supprimer un contenu inapproprié et ajouter des espèces au référentiel.

## 5. Périmètre du prototype

### Inclus (MVP)
- Inscription, connexion, déconnexion (JWT).
- Publication d'une observation : 1 à 5 photos, catégorie, date, lieu, description.
- Liste des observations récentes, filtres par catégorie et statut, pagination.
- Page détail d'une observation.
- Proposition d'espèce (depuis un référentiel pré-rempli).
- Vote (1 vote par utilisateur et par observation) et calcul du consensus.
- Affichage des votes par proposition.
- Commentaires.
- Notifications in-app (polling).
- Page « Mes discussions » et « Mes observations ».

### Exclus
Carte, IA, emails, application mobile, messagerie privée, système de réputation.

## 6. Exigences fonctionnelles

| ID | Exigence | Stories |
|---|---|---|
| FR-01 | Le système permet l'inscription avec nom d'utilisateur, email et mot de passe uniques. | #11 |
| FR-02 | Le système authentifie par email + mot de passe et renvoie un jeton JWT (24 h). | #10 |
| FR-03 | Le système liste les observations où l'utilisateur a participé (« Mes discussions »). | #10 |
| FR-04 | Un utilisateur connecté publie une observation avec 1 à 5 photos. | #1, #13 |
| FR-05 | L'auteur peut modifier ou supprimer son observation. | #14 |
| FR-06 | La liste des observations est triée par date de publication décroissante et paginée. | #8 |
| FR-07 | La liste est filtrable par catégorie, statut et auteur. | #8, #9 |
| FR-08 | Le référentiel d'espèces est recherchable par nom commun ou scientifique. | #16 |
| FR-09 | Un utilisateur connecté propose une espèce ; cela vaut vote. | #17 |
| FR-10 | Un utilisateur a au plus un vote par observation ; il peut le déplacer ou le retirer. | #4 |
| FR-11 | Le système recalcule le consensus après chaque changement de vote (§6.1). | #4 |
| FR-12 | Les votes sont visibles par proposition (nombre, %, votants). | #5 |
| FR-13 | Un utilisateur connecté commente une observation. | #3, #18 |
| FR-14 | Le système crée des notifications pour l'auteur et les participants (§6.2). | #6 |
| FR-15 | Un admin peut supprimer tout contenu et gérer catégories/espèces. | #19, #20 |

### 6.1 Règle de consensus

Après chaque création, déplacement ou retrait de vote sur une observation :

```
total     = nombre de votes sur l'observation
meilleure = proposition ayant le plus de votes (en cas d'égalité : la plus ancienne)

SI meilleure.votes >= 3 ET meilleure.votes / total >= 2/3 ALORS
    observation.status = IDENTIFIED
    observation.identified_species_id = meilleure.species_id
SINON
    observation.status = NEEDS_ID
    observation.identified_species_id = NULL
```

Si le statut passe à `IDENTIFIED`, une notification `OBSERVATION_IDENTIFIED` est envoyée à l'auteur.
Le statut peut revenir à `NEEDS_ID` si de nouveaux votes cassent le consensus.

### 6.2 Notifications

| Événement | Destinataires | Type |
|---|---|---|
| Nouvelle proposition d'espèce | Auteur de l'observation | `NEW_PROPOSAL` |
| Nouveau commentaire | Auteur + utilisateurs ayant déjà commenté | `NEW_COMMENT` |
| Consensus atteint | Auteur + proposeur de l'espèce retenue | `OBSERVATION_IDENTIFIED` |

Jamais de notification à l'auteur de l'action lui-même.

### 6.3 Droits

| Action | Visiteur | Utilisateur | Auteur du contenu | Admin |
|---|---|---|---|---|
| Lire observations, votes, commentaires | ✅ | ✅ | ✅ | ✅ |
| Publier observation, proposer, voter, commenter | ❌ | ✅ | ✅ | ✅ |
| Modifier / supprimer une observation ou un commentaire | ❌ | ❌ | ✅ | ✅ (suppression) |
| Gérer catégories / espèces | ❌ | ❌ | ❌ | ✅ |

## 7. Exigences non fonctionnelles

| ID | Exigence |
|---|---|
| NFR-01 | Mots de passe hachés avec bcrypt (coût 10). |
| NFR-02 | Toutes les routes d'écriture exigent un JWT valide (`Authorization: Bearer <token>`). |
| NFR-03 | Validation des entrées côté serveur ; erreurs au format JSON uniforme (voir API). |
| NFR-04 | Upload limité aux images JPEG/PNG/WEBP de 5 Mo max. |
| NFR-05 | Temps de réponse < 500 ms sur la liste paginée avec 1 000 observations (en local). |
| NFR-06 | Interface responsive (utilisable sur mobile, largeur ≥ 360 px). |
| NFR-07 | Les secrets (clé JWT, URL BDD) sont lus depuis un fichier `.env` non versionné. |

## 8. Architecture technique proposée

| Couche | Choix | Raison |
|---|---|---|
| Front-end | React + Vite | Simple, rapide à démarrer |
| Back-end | Node.js + Express | API REST facile à tester avec Postman |
| Base de données | PostgreSQL | Relations fortes (votes, propositions), contraintes d'unicité |
| ORM / requêtes | Prisma (ou `pg` en SQL brut) | Migrations et typage |
| Auth | JWT + bcrypt | Sans état, simple |
| Upload | Multer, stockage disque `/uploads` | Pas de service externe nécessaire |
| Notifications | Polling toutes les 30 s | Évite WebSocket pour un prototype |

Voir `03-uml.md` (diagramme de composants) et `04-database.md`.

## 9. Écrans

| Écran | Route front | Stories |
|---|---|---|
| Accueil / fil des observations + filtres | `/` | #8, #9 |
| Inscription | `/register` | #11 |
| Connexion | `/login` | #10 |
| Nouvelle observation | `/observations/new` | #1, #13 |
| Détail observation (photos, propositions, votes, commentaires) | `/observations/:id` | #15, #17, #4, #5, #3 |
| Mes observations | `/me/observations` | #14 |
| Mes discussions | `/me/conversations` | #10 |
| Notifications (menu déroulant + page) | `/notifications` | #6 |
| Profil | `/users/:id` | #12 |
| Admin référentiel | `/admin` | #19, #20 |

Maquette fil d'accueil :

```
+--------------------------------------------------------------+
| Merlette        [Rechercher]        🔔3   [+ Observation] 👤 |
+--------------------------------------------------------------+
| Catégorie: [Toutes ▾]  Statut: [À identifier ▾]  Tri: Récent |
+--------------------------------------------------------------+
| +-----------+  +-----------+  +-----------+  +-----------+   |
| |  [photo]  |  |  [photo]  |  |  [photo]  |  |  [photo]  |   |
| | Oiseaux   |  | Plantes   |  | Insectes  |  | Oiseaux   |   |
| | À identif.|  | Identifiée|  | À identif.|  | À identif.|   |
| | @lea · 2h |  | @karim·1j |  | @sam · 1j |  | @lea · 2j |   |
| | 💬3  🗳2  |  | 💬5  🗳4  |  | 💬0  🗳0  |  | 💬1  🗳1  |   |
| +-----------+  +-----------+  +-----------+  +-----------+   |
|                    [< Précédent]  1 2 3  [Suivant >]         |
+--------------------------------------------------------------+
```

Maquette détail :

```
+--------------------------------------------------------------+
| [   photo principale   ]   Mésange bleue ✅ Identifiée        |
| [mini][mini][mini]         Oiseaux · vu le 10/10/2026         |
|                            Parc de la Tête d'Or · par @lea    |
|                            "Petit oiseau sur la mangeoire"    |
+--------------------------------------------------------------+
| Propositions                               [+ Proposer]      |
|  Cyanistes caeruleus (Mésange bleue)  ███████░  3 (75%) [✓]  |
|  Parus major (Mésange charbonnière)   ██░░░░░░  1 (25%) [Voter]|
+--------------------------------------------------------------+
| Commentaires                                                 |
|  @karim : Calotte bleue et joues blanches → mésange bleue.   |
|  [ Écrire un commentaire...                    ] [Envoyer]   |
+--------------------------------------------------------------+
```

## 10. Planning (3 sprints de 2 semaines)

| Sprint | Contenu | Stories |
|---|---|---|
| **S1 — Socle** | Setup projet, BDD, seed, auth, publication et liste des observations | #11, #10 (login), #1, #13, #8 |
| **S2 — Identification** | Détail, filtres, recherche espèce, propositions, votes, consensus, commentaires | #15, #9, #16, #17, #4, #5, #3 |
| **S3 — Engagement & finitions** | Notifications, mes discussions, mes observations, profil, admin, tests Postman complets, démo | #6, #10 (discussions), #14, #12, #18, #19, #20 |

## 11. Risques

| Risque | Impact | Mitigation |
|---|---|---|
| Upload d'images mal géré | Fonction principale bloquée | Le faire dès le S1, limiter tailles/types |
| Règle de consensus buggée | Statuts incohérents | Fonction isolée + scénario Postman dédié |
| Référentiel d'espèces trop gros | Perte de temps | Seed de ~30 espèces suffit pour la démo |
| Retard d'équipe | Démo incomplète | Prioriser les « Must », les « Could » sont optionnels |

## 12. Critères de réussite de la démo

1. Léa s'inscrit, publie une photo d'oiseau.
2. Karim filtre « Oiseaux / À identifier », trouve l'observation, propose « Mésange bleue ».
3. Léa reçoit une notification, répond en commentaire.
4. Deux autres utilisateurs votent pour « Mésange bleue » → l'observation passe « Identifiée ».
5. Léa reçoit la notification d'identification.
6. La collection Postman passe entièrement au vert.

## 13. Glossaire

| Terme | Définition |
|---|---|
| Observation | Publication d'un utilisateur : photos + infos sur un être vivant observé. |
| Proposition | Espèce suggérée pour une observation. |
| Vote | Soutien d'un utilisateur à une proposition (1 par observation). |
| Consensus | Proposition retenue selon la règle §6.1. |
| Référentiel | Liste des catégories et espèces connues par la plateforme. |
