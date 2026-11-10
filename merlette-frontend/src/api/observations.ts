import { api } from './client'
import type { Feed, Observation } from '../types'
export async function getObservations(params: Record<string, string | number>) { return (await api.get<Feed>('/observations', { params })).data }
export async function getMyObservations(page: number) { return (await api.get<Feed>('/users/me/observations', { params: { page } })).data }
export async function getObservation(id: string) { return (await api.get<Observation>(`/observations/${id}`)).data }
export async function createObservation(input: FormData) { return (await api.post<Observation>('/observations', input)).data }
export async function updateObservation(id: number, input: Record<string, string | number | null>) { return (await api.patch<Observation>(`/observations/${id}`, input)).data }
export async function deleteObservation(id: number) { await api.delete(`/observations/${id}`) }
export async function addPhotos(id: number, input: FormData) { return (await api.post(`/observations/${id}/photos`, input)).data }
export async function deletePhoto(id: number, photoId: number) { await api.delete(`/observations/${id}/photos/${photoId}`) }
