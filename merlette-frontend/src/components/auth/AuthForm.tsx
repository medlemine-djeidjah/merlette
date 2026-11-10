import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router'
import { login, register } from '../../api/auth'
import { errorMessage } from '../../api/client'
import { useAuth } from '../../hooks/useAuth'
import Icon from '../common/Icon'
import Botanical from '../common/Botanical'
export default function AuthForm({ registration = false }: { registration?: boolean }) {
  const { user, loading, accept, sessionError } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [error, setError] = useState('')
  const [pending, setPending] = useState(false)
  const [visible, setVisible] = useState(false)
  const from = location.state?.from
  const destination = typeof from === 'string' && from.startsWith('/') && !from.startsWith('//') ? from : '/'
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = new FormData(event.currentTarget)
    setPending(true); setError('')
    const password = String(form.get('password'))
    if (new TextEncoder().encode(password).length > 72) { setError('Ce mot de passe est trop long. Essayez avec moins de caractères.'); setPending(false); return }
    try {
      const input = { email: String(form.get('email')).trim(), password }
      const session = registration ? await register({ ...input, username: String(form.get('username')).trim() }) : await login(input)
      accept(session); navigate(destination, { replace: true })
    } catch (err) { setError(errorMessage(err)) }
    finally { setPending(false) }
  }
  if (user && !loading) return <Navigate to={destination} replace />
  return <section className="auth-layout"><aside className="auth-story"><span className="eyebrow">Le carnet de la communauté</span><h2>Le vivant se reconnaît<br /><em>mieux ensemble.</em></h2><p>Des observations partagées, des regards qui se croisent et le plaisir d’apprendre ce qui nous entoure.</p><Botanical className="auth-art" /><span className="auth-caption">Observer · Proposer · Comprendre</span></aside>
    <div className="auth-form-panel"><span className="eyebrow">{registration ? 'Chaque regard compte' : 'Votre carnet vous attend'}</span><h1>{registration ? 'Cultivons la curiosité.' : 'Ravi de vous revoir.'}</h1><p>{registration ? 'Créez votre compte et partagez votre première découverte.' : 'Connectez-vous pour retrouver vos observations et vos discussions.'}</p>
      {(error || sessionError) && <p id="auth-error" role="alert" className="feedback feedback-error">{error || sessionError}</p>}
      <form onSubmit={submit} aria-describedby={error || sessionError ? 'auth-error' : undefined}>
        {registration && <label className="field">Nom d’utilisateur<input name="username" placeholder="Ex. lea_martin" required minLength={3} maxLength={30} pattern="[a-zA-Z0-9_]{3,30}" autoComplete="username" title="3 à 30 lettres sans accents, chiffres ou tirets bas" /><small>3 à 30 lettres, chiffres ou tirets bas.</small></label>}
        <label className="field">Adresse e-mail<input name="email" placeholder="vous@exemple.fr" type="email" required maxLength={255} autoComplete="email" /></label>
        <label className="field">Mot de passe<div className="password-input"><input name="password" type={visible ? 'text' : 'password'} placeholder={registration ? 'Au moins 8 caractères' : 'Votre mot de passe'} required minLength={registration ? 8 : 1} maxLength={72} autoComplete={registration ? 'new-password' : 'current-password'} /><button type="button" aria-label={visible ? 'Masquer le mot de passe' : 'Afficher le mot de passe'} aria-pressed={visible} onClick={() => setVisible(!visible)}><Icon name="eye" size={18} /></button></div>{registration && <small>Choisissez un mot de passe personnel d’au moins 8 caractères.</small>}</label>
        <button className="button button-full" disabled={pending || loading}>{pending ? 'Un instant…' : registration ? 'Créer mon compte' : 'Se connecter'}{!pending && <Icon name="arrow" size={16} />}</button>
      </form><div className="auth-switch">{registration ? 'Déjà un compte ?' : 'Première visite ?'}<Link to={registration ? '/login' : '/register'} state={location.state}>{registration ? 'Se connecter' : 'Rejoindre Merlette'}</Link></div>
    </div>
  </section>
}
