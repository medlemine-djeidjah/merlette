import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation, useNavigate } from 'react-router'
import { login, register } from '../../api/auth'
import { errorMessage } from '../../api/client'
import { useAuth } from '../../hooks/useAuth'
export default function AuthForm({ registration = false }: { registration?: boolean }) {
  const { accept, sessionError } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setPending(true); setError('')
    try {
      const input = { email: String(form.get('email')), password: String(form.get('password')) }
      const session = registration
        ? await register({ ...input, username: String(form.get('username')) }) : await login(input)
      accept(session)
      const from = location.state?.from
      navigate(typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/', { replace: true })
    } catch (err) { setError(errorMessage(err)) }
    finally { setPending(false) }
  }
  return <section className="card mx-auto mt-8 max-w-md bg-base-100 shadow-sm">
    <div className="card-body gap-5">
      <h1 className="card-title text-2xl">{registration ? 'Créer un compte' : 'Se connecter'}</h1>
      <p>Partage tes découvertes et retrouve tes observations.</p>
      {(error || sessionError) && <p role="alert" className="alert alert-error">{error || sessionError}</p>}
      <form onSubmit={submit} className="flex flex-col gap-4">
        {registration && <label className="flex flex-col gap-1">Pseudo
          <input className="input w-full" name="username" required minLength={3} maxLength={30}
            pattern="[a-zA-Z0-9_]{3,30}" autoComplete="username" title="3 à 30 lettres, chiffres ou caractères _" />
        </label>}
        <label className="flex flex-col gap-1">Email
          <input className="input w-full" name="email" type="email" required maxLength={255} autoComplete="email" />
        </label>
        <label className="flex flex-col gap-1">Mot de passe
          <input className="input w-full" name="password" type="password" required minLength={registration ? 8 : 1}
            maxLength={72} autoComplete={registration ? 'new-password' : 'current-password'} />
          {registration && <span className="text-sm opacity-70">Au moins 8 caractères ; maximum 72 octets UTF-8.</span>}
        </label>
        <button className="btn btn-primary" disabled={pending}>{pending ? 'En cours…' : registration ? 'Créer mon compte' : 'Connexion'}</button>
      </form>
      <Link className="link" to={registration ? '/login' : '/register'} state={location.state}>
        {registration ? 'Déjà un compte ? Se connecter' : 'Créer un compte'}
      </Link>
    </div>
  </section>
}
