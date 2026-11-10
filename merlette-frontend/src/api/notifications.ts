import { api } from './client'
import type { Notification, Paginated } from '../types'
export async function getNotifications(page: number, unread: boolean) { return (await api.get<Paginated<Notification>>('/notifications', { params: { page, ...(unread && { unread: true }) } })).data }
export async function unreadCount() { return (await api.get<{ count: number }>('/notifications/unread-count')).data }
export async function markRead(id: number) { return (await api.patch<Notification>(`/notifications/${id}/read`)).data }
export async function markAllRead() { return (await api.patch<{ updated: number }>('/notifications/read-all')).data }
