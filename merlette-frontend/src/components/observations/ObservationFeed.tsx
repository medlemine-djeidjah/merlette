import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router'
import { getObservations } from '../../api/observations'
import { useAuth } from '../../hooks/useAuth'
import ObservationCard from './ObservationCard'
import ObservationFilters from './ObservationFilters'
import Pagination from '../common/Pagination'
import { EmptyState, ErrorState, Loading } from '../common/Feedback'
import Icon from '../common/Icon'

export default function ObservationFeed({ mine = false }: { mine?: boolean }) {
  const [params, setParams] = useSearchParams()
  const { user } = useAuth()
  const rawPage = Number(params.get('page') || 1)
  const page = Number.isSafeInteger(rawPage) && rawPage > 0 ? rawPage : 1
  const category = params.get('category') || ''
  const status = ['NEEDS_ID', 'IDENTIFIED'].includes(params.get('status') || '') ? params.get('status')! : ''
  const sort = params.get('sort') === 'oldest' ? 'oldest' : 'recent'
  const feed = useQuery({
    queryKey: ['observations', mine ? user?.id : 'all', page, category, status, sort],
    queryFn: () => getObservations({ page, limit: 12, sort, ...(category && { category }), ...(status && { status }), ...(mine && user && { userId: user.id }) }),
    enabled: !mine || !!user, placeholderData: keepPreviousData,
  })
  function filter(key: string, value: string) {
    setParams(previous => { const next = new URLSearchParams(previous); if (value) next.set(key, value); else next.delete(key); next.delete('page'); return next })
  }
  function go(nextPage: number) {
    setParams(previous => { const next = new URLSearchParams(previous); next.set('page', String(nextPage)); return next })
    document.getElementById('observations')?.scrollIntoView({ behavior: 'instant', block: 'start' })
  }
  return <section id="observations" className={mine ? '' : 'feed-section'}>
    {mine ? <header className="page-heading section-heading"><div><span className="eyebrow">Mon carnet de terrain</span><h1>Mes observations</h1><p>Toutes vos rencontres, au même endroit.</p></div><Link className="button button-small" to="/observations/new"><Icon name="plus" size={16} />Nouvelle observation</Link></header>
      : <div className="section-heading feed-heading"><h2>Explorer les observations</h2><span className="eyebrow">Le carnet de la communauté</span></div>}
    <ObservationFilters category={category} status={status} sort={sort} onChange={filter} />
    {feed.isPending && <Loading cards />}
    {feed.isError && <ErrorState error={feed.error} retry={() => feed.refetch()} />}
    {feed.data && !feed.isError && <div aria-busy={feed.isFetching} style={{ opacity: feed.isPlaceholderData ? .55 : 1 }}>
      <div className="results-line" aria-live="polite"><span>{feed.data.pagination.total} observation{feed.data.pagination.total > 1 ? 's' : ''}{feed.isFetching ? ' · Actualisation…' : ''}</span>{(category || status) && <button className="text-link" onClick={() => setParams({})}>Effacer les filtres</button>}</div>
      {feed.data.data.length ? <div className="observation-grid">{feed.data.data.map(o => <ObservationCard key={o.id} observation={o} />)}</div> : <EmptyState title={category || status ? 'La découverte se fait attendre' : 'Le carnet ne demande qu’à s’ouvrir'} action={<Link className="button" to="/observations/new"><Icon name="plus" size={16} />Partager une observation</Link>}>{category || status ? 'Aucune observation ne correspond à ces filtres. Essayez une autre catégorie ou un autre statut.' : 'Une rencontre au jardin ou au détour d’un sentier ? Soyez la première personne à la partager.'}</EmptyState>}
      <Pagination pagination={feed.data.pagination} onChange={go} />
    </div>}
  </section>
}
