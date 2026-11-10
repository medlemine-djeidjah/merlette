// GET /categories · POST /categories (admin)
// Voir docs/05-api.md
// TODO

import { api } from './client'
import type { Category } from '../types'
export async function getCategories() { return (await api.get<Category[]>('/categories')).data }
