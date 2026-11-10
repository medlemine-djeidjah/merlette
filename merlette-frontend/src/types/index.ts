// Types partagés (User, Observation, Proposal, Comment, Notification…)
// Voir docs/05-api.md pour la forme des réponses
// TODO

export interface User {
  id: number; username: string; email: string; role: 'USER' | 'ADMIN'
  bio: string | null; avatarUrl: string | null; createdAt: string
}
export interface Session { token: string; user: User }
export interface Category { id: number; name: string; slug: string }
export interface Observation {
  id: number; title: string | null; description: string | null
  status: 'NEEDS_ID' | 'IDENTIFIED'; category: Category
  author: { id: number; username: string }; observedAt: string
  locationName: string | null; latitude: number | null; longitude: number | null
  coverUrl: string | null; photos: { id: number; url: string; position: number }[]
  createdAt: string; updatedAt: string
}
export interface Feed {
  data: Observation[]
  pagination: { page: number; limit: number; total: number; totalPages: number }
}
