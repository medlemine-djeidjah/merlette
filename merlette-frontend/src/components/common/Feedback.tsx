import type { ReactNode } from 'react'
import { errorMessage } from '../../api/client'
import Icon from './Icon'
import type { IconName } from './Icon'
export function ErrorState({ error, retry }: { error: unknown; retry?: () => void }) {
  return <div className="feedback feedback-error" role="alert"><Icon name="info" /><div><strong>Un petit contretemps</strong><p>{errorMessage(error)}</p></div>{retry && <button className="button button-small button-secondary" onClick={retry}>Réessayer</button>}</div>
}
export function EmptyState({ title, children, icon = 'leaf', action }: { title: string; children: ReactNode; icon?: IconName; action?: ReactNode }) {
  return <div className="empty-state"><span className="empty-icon"><Icon name={icon} size={30} /></span><h2>{title}</h2><p>{children}</p>{action}</div>
}
export function ComingSoon({ title, children, icon = 'leaf' }: { title: string; children: ReactNode; icon?: IconName }) {
  return <EmptyState title={title} icon={icon} action={<span className="pill">Bientôt disponible</span>}>{children}</EmptyState>
}
export function Loading({ cards = false }: { cards?: boolean }) {
  return <div role="status" aria-label="Chargement en cours" className={cards ? 'observation-grid' : 'loading-block'}>{Array.from({ length: cards ? 6 : 1 }, (_, i) => <div key={i} className="skeleton-card"><div className="skeleton-image" /><div className="skeleton-line" /><div className="skeleton-line short" /></div>)}<span className="sr-only">Chargement en cours…</span></div>
}
