import { api } from './client'
import type { Conversation, Profile, User } from '../types'
export async function getProfile(id: number) { return (await api.get<Profile>(`/users/${id}`)).data }
export async function updateProfile(input: { bio: string; avatarUrl: string | null }) {
  return (await api.patch<User>('/users/me', input)).data
}
export async function getConversations() { return (await api.get<{ data: Conversation[] }>('/users/me/conversations')).data }
