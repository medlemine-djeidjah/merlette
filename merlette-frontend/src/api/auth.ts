// POST /auth/register · POST /auth/login · GET /auth/me
// Voir docs/05-api.md
// TODO

import { api } from './client'
import type { Session, User } from '../types'
export async function register(input: { username: string; email: string; password: string }) {
  return (await api.post<Session>('/auth/register', input)).data
}
export async function login(input: { email: string; password: string }) {
  return (await api.post<Session>('/auth/login', input)).data
}
export async function currentUser() { return (await api.get<User>('/auth/me')).data }
