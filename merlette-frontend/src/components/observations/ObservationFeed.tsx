import { useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { getCategories } from '../../api/categories'
import { getMyObservations, getObservations } from '../../api/observations'
import { errorMessage } from '../../api/client'
import { useAuth } from '../../hooks/useAuth'
import ObservationCard from './ObservationCard'

export default function ObservationFeed({ mine = false }: { mine?: boolean }) {
  const [params, setParams] = useSearchParams()
  const { user } = useAuth()
  const rawPage = Number(params.get('page') || 1)
  const page = Number.isInteger(rawPage) && rawPage > 0 ? rawPage : 1
  const category = params.get('category') || ''
  const status = params.get('status') || ''
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const feed = useQuery({
    queryKey: ['observations', mine ? user?.id : 'all', page, category, status],
    queryFn: () => mine ? getMyObservations(page) : getObservations({ page, ...(category && { category }), ...(status && { status }) }),
  })
  function filter(key: string, value: string) {
    setParams(previous => {
      const next = new URLSearchParams(previous)
      if (value) next.set(key, value); else next.delete(key)
      next.delete('page'); return next
    })
  }
  function go(nextPage: number) {
    setParams(previous => { const next = new URLSearchParams(previous); next.set('page', String(nextPage)); return next })
  }
  return <section className="space-y-6 py-6">
    <header className="space-y-2">
      <h1 className="text-3xl font-bold">{mine ? 'Mes observations' : 'La nature, observée ensemble'}</h1>
      <p>{mine ? 'Retrouve les découvertes que tu as partagées.' : 'Explore les découvertes de la communauté et partage les tiennes.'}</p>
    </header>
    {!mine && <div className="flex flex-wrap gap-4">
      <label className="flex flex-col gap-1">Catégorie
        <select className="select" value={category} onChange={e => filter('category', e.target.value)}>
          <option value="">Toutes les catégories</option>
          {categories.data?.map(c => <option key={c.id} value={c.slug}>{c.name}</option>)}
        </select>
      </label>
      <label className="flex flex-col gap-1">Statut
        <select className="select" value={status} onChange={e => filter('status', e.target.value)}>
          <option value="">Tous les statuts</option><option value="NEEDS_ID">À identifier</option><option value="IDENTIFIED">Identifiées</option>
        </select>
      </label>
    </div>}
    {categories.isError && <p role="alert">{errorMessage(categories.error)}</p>}
    {feed.isPending && <p role="status">Chargement des observations…</p>}
    {feed.isError && <div role="alert" className="alert alert-error">{errorMessage(feed.error)} <button className="btn btn-sm" onClick={() => feed.refetch()}>Réessayer</button></div>}
    {feed.data && <>
      <p className="text-sm opacity-70">{feed.data.pagination.total} observation(s)</p>
      {feed.data.data.length ? <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
        {feed.data.data.map(o => <ObservationCard key={o.id} observation={o} />)}
      </div> : <div className="rounded-box bg-base-100 p-8 text-center">
        <p>Aucune observation pour le moment.</p><Link className="btn btn-primary mt-4" to="/observations/new">Partager une découverte</Link>
      </div>}
      {feed.data.pagination.totalPages > 1 && <nav aria-label="Pagination" className="flex items-center justify-center gap-4">
        <button className="btn" disabled={page <= 1} onClick={() => go(page - 1)}>Précédent</button>
        <span>Page {page} sur {feed.data.pagination.totalPages}</span>
        <button className="btn" disabled={page >= feed.data.pagination.totalPages} onClick={() => go(page + 1)}>Suivant</button>
      </nav>}
    </>}
  </section>
}
