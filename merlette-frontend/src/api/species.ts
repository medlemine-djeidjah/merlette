import { api } from './client'
import type { Species } from '../types'
export async function searchSpecies(q: string, category?: string) {
  return (await api.get<Species[]>('/species', { params: { q, category, limit: 10 } })).data
}
export async function createSpecies(input: { commonName: string; scientificName: string; categoryId: number }) {
  return (await api.post<Species>('/species', input)).data
}
