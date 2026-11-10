import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL || '/api',
  timeout: 20000,
})
export const TOKEN_KEY = 'merlette.token'
api.interceptors.request.use(config => {
  const token = localStorage.getItem(TOKEN_KEY)
  if (token && !['/auth/register', '/auth/login'].includes(config.url || '')) config.headers.Authorization = `Bearer ${token}`
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
    if (fields || detail?.message) return fields || detail.message
    if (error.response?.status === 404) return 'Ce contenu est introuvable ou a été supprimé.'
    if (error.response?.status === 403) return 'Vous n’avez pas accès à cette action.'
    if (error.response?.status === 413) return 'Ces fichiers sont trop volumineux. Limitez chaque photo à 5 Mo.'
    return 'La connexion a été interrompue. Vérifiez votre connexion et réessayez.'
  }
  return error instanceof Error ? error.message : 'Une erreur est survenue. Veuillez réessayer.'
}
export function imageUrl(path?: string | null): string | undefined {
  if (!path) return undefined
  try {
    const url = new URL(path, new URL(api.defaults.baseURL!, window.location.origin))
    return ['http:', 'https:'].includes(url.protocol) ? url.toString() : undefined
  } catch { return undefined }
}
