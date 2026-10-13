# Merlette — Frontend

Interface web de Merlette (React + Vite + Tailwind CSS + daisyUI). Documentation du projet : `../docs/`.

## Stack

| Outil | Rôle |
|---|---|
| React 19 + TypeScript | Interface |
| Vite 8 | Serveur de dev et build |
| Tailwind CSS 4 + daisyUI 5 | Styles et composants (thèmes `emerald` / `forest`) |
| React Router 8 | Navigation |
| TanStack Query 5 | Appels API, cache, polling |
| Axios | Client HTTP |

## Démarrer

```bash
cp .env.example .env   # URL de l'API Spring Boot
npm install
npm run dev            # http://localhost:5173
```

## Structure

```
src/
├── api/            # un fichier par ressource de l'API (docs/05-api.md)
├── components/     # composants réutilisables, rangés par domaine
├── context/        # AuthContext
├── hooks/          # useAuth, useNotifications
├── pages/          # une page par route (commentaire en tête = route + stories)
├── types/          # types partagés
├── utils/
├── router.tsx      # routes
├── main.tsx        # point d'entrée
└── index.css       # Tailwind + daisyUI
```

## Routes

| Route | Page | Stories |
|---|---|---|
| `/` | `HomePage` | #8, #9 |
| `/login` | `LoginPage` | #10 |
| `/register` | `RegisterPage` | #11 |
| `/observations/new` | `NewObservationPage` | #1, #13 |
| `/observations/:id` | `ObservationDetailPage` | #15, #17, #4, #5, #3 |
| `/me/observations` | `MyObservationsPage` | #14 |
| `/me/conversations` | `MyConversationsPage` | #10 |
| `/notifications` | `NotificationsPage` | #6 |
| `/users/:id` | `ProfilePage` | #12 |
| `/admin` | `AdminPage` | #19, #20 |
