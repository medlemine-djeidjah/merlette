export interface User {
  id: number; username: string; email: string; role: 'USER' | 'ADMIN'
  bio: string | null; avatarUrl: string | null; createdAt: string
}
export interface Session { token: string; user: User }
export interface Author { id: number; username: string }
export interface Category { id: number; name: string; slug: string }
export interface Species {
  id: number; commonName: string; scientificName: string
  category?: { id: number; slug: string; name?: string }
}
export interface Observation {
  id: number; title: string | null; description?: string | null
  status: 'NEEDS_ID' | 'IDENTIFIED'; category: Category; author: Author
  observedAt: string; locationName?: string | null; latitude?: number | null; longitude?: number | null
  coverUrl?: string | null; photos?: { id: number; url: string; position: number }[]
  createdAt: string; updatedAt?: string
  proposalCount?: number; commentCount?: number; identifiedSpecies?: Species | null
  myVoteProposalId?: number | null
}
export interface PaginationData { page: number; limit: number; total: number; totalPages: number }
export interface Paginated<T> { data: T[]; pagination: PaginationData }
export type Feed = Paginated<Observation>
export interface Proposal {
  id: number; species: Species; proposedBy: Author; votes: number; percent: number
  voters: Author[]; createdAt: string
}
export interface Proposals {
  totalVotes: number
  consensus: { reached: boolean; proposalId: number | null; speciesId: number | null }
  proposals: Proposal[]
}
export interface Comment {
  id: number; content: string; author: Author; createdAt: string; updatedAt: string; edited: boolean
}
export interface Notification {
  id: number; type: 'NEW_PROPOSAL' | 'NEW_COMMENT' | 'OBSERVATION_IDENTIFIED'
  message: string; observationId: number | null; actor: Author | null; isRead: boolean; createdAt: string
}
export interface Conversation {
  observationId: number; title: string | null; status: Observation['status']; coverUrl: string | null
  lastActivity: string; myRole: ('AUTHOR' | 'COMMENTER' | 'PROPOSER')[]
}
export interface Profile {
  id: number; username: string; bio: string | null; avatarUrl: string | null; createdAt: string
  stats: { observations: number; proposals: number; identifiedObservations: number }
}
