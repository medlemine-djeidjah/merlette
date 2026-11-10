import { api } from './client'
import type { Session, User } from '../types'
export async function register(input: { username: string; email: string; password: string }) { return (await api.post<Session>('/auth/register', input)).data }
export async function login(input: { email: string; password: string }) { return (await api.post<Session>('/auth/login', input)).data }
export async function currentUser(signal?: AbortSignal) { return (await api.get<User>('/auth/me', { signal })).data }
