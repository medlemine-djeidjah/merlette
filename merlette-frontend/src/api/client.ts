// Instance axios : baseURL = VITE_API_URL, ajout du header Authorization: Bearer <token>
// Voir docs/05-api.md

import axios from 'axios'

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL,
})

// TODO : intercepteur qui ajoute le token JWT
