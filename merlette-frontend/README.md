# Merlette — Frontend

Interface React + TypeScript + Vite : un carnet de terrain pour partager et identifier
les espèces avec la communauté.

Voir [le guide design et intégration API](../docs/frontend-design.md) pour les pages,
les options de disponibilité et les tests navigateur. Les contrats métier sont dans
`../docs/05-api.pdf` et le périmètre dans `../docs/02-prd.pdf`.

## Démarrer dans PowerShell

```powershell
if (!(Test-Path .env)) { Copy-Item .env.example .env }
npm.cmd ci
npm.cmd run dev
```

Ouvrir http://localhost:5173, avec le backend Spring démarré sur 8080. L’origine
autorisée côté backend doit correspondre à l’URL du navigateur (`FRONTEND_URL`).
En développement, `VITE_API_URL=/api` utilise le proxy Vite. `MERLETTE_API_TARGET`
définit le serveur Spring (par exemple `http://10.86.216.74:8080` pour le backend
partagé). `/api` et `/uploads` sont transmis au même serveur. Après un changement
de `.env`, redémarrer Vite. En production, prévoir le même routage sur le serveur
web ou définir une URL absolue `VITE_API_URL` avec le CORS correspondant.

## Vérifier

```powershell
npm.cmd run lint
npm.cmd run build
```

Les tests Chrome utilisent `scripts/frontend-smoke.mjs` ; les commandes de préparation
du navigateur et du serveur de test figurent dans le guide. Ils interceptent les
réponses API dans un profil isolé, sans écriture dans la base de développement.

## Organisation

- `src/api/` : requêtes HTTP conformes aux contrats du projet.
- `src/types/` : types JSON partagés.
- `src/components/` : navigation, cartes, formulaires, galerie, votes et commentaires.
- `src/pages/` : pages publiques et espace personnel.
- `src/context/`, `src/hooks/` : session et notifications.
- `src/config/features.ts` : disponibilité des modules backend.
- `src/index.css` : palette, typographies, composants et adaptations mobile.

Le design utilise des polices système et des illustrations SVG locales. Aucune
nouvelle dépendance n’est requise. Les modules encore absents du backend sont
préparés dans le frontend et désactivés par défaut dans `.env.example` ; ils affichent
un état explicite au lieu de contenus ou confirmations fictifs.
