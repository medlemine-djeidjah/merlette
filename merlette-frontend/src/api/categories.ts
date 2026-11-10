import { api } from './client'
import type { Category } from '../types'
export async function getCategories() { return (await api.get<Category[]>('/categories')).data }
export async function createCategory(input: { name: string; slug: string }) { return (await api.post<Category>('/categories', input)).data }
