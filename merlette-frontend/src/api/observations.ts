// GET /observations · POST /observations · GET|PATCH|DELETE /observations/:id · POST|DELETE photos
// Voir docs/05-api.md
// TODO

import { api } from './client'
import type { Feed, Observation } from '../types'
export async function getObservations(params: Record<string, string | number>) {
  return (await api.get<Feed>('/observations', { params })).data
}
export async function getMyObservations(page: number) {
  return (await api.get<Feed>('/users/me/observations', { params: { page } })).data
}
export async function getObservation(id: string) { return (await api.get<Observation>(`/observations/${id}`)).data }
export async function createObservation(input: FormData) { return (await api.post<Observation>('/observations', input)).data }
