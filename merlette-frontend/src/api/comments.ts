import { api } from './client'
import type { Comment } from '../types'
export async function getComments(id: number) { return (await api.get<Comment[]>(`/observations/${id}/comments`)).data }
export async function createComment(id: number, content: string) { return (await api.post<Comment>(`/observations/${id}/comments`, { content })).data }
export async function updateComment(id: number, content: string) { return (await api.patch<Comment>(`/comments/${id}`, { content })).data }
export async function deleteComment(id: number) { await api.delete(`/comments/${id}`) }
