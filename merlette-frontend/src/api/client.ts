// Instance axios : baseURL = VITE_API_URL, ajout du header Authorization: Bearer <token>
// Voir docs/05-api.md

import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080/api',
})

export const TOKEN_KEY = 'merlette.token'
api.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})
api.interceptors.response.use(response => response, error => {
  if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/login')) {
    localStorage.removeItem(TOKEN_KEY)
    window.dispatchEvent(new Event('merlette:session-expired'))
  }
  return Promise.reject(error)
})
export function errorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const detail = error.response?.data?.error
    const fields = detail?.details?.map((field: { field: string; message: string }) => `${field.field} : ${field.message}`).join(' · ')
    return fields || detail?.message || 'Impossible de joindre le serveur. Vérifie que le backend est démarré.'
  }
  return 'Une erreur est survenue.'
}
export function imageUrl(path: string): string {
  return new URL(path, new URL(api.defaults.baseURL!, window.location.origin)).toString()
}
