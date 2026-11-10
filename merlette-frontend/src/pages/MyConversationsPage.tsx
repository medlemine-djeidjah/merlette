import { useState } from 'react'
import { Link } from 'react-router'
import { useQuery } from '@tanstack/react-query'
import { getConversations } from '../api/users'
import { features } from '../config/features'
import { useAuth } from '../hooks/useAuth'
import { formatDate } from '../utils/format'
import StatusBadge from '../components/observations/StatusBadge'
import Photo from '../components/common/Photo'
import Icon from '../components/common/Icon'
import { ComingSoon, EmptyState, ErrorState, Loading } from '../components/common/Feedback'
export default function MyConversationsPage() {
  const { user } = useAuth()
  const [filter, setFilter] = useState('all')
  const query = useQuery({ queryKey: ['conversations', user?.id], queryFn: getConversations, enabled: features.conversations && !!user })
  const rows = query.data?.data.filter(row => filter === 'all' || (filter === 'author' ? row.myRole.includes('AUTHOR') : row.myRole.some(role => role !== 'AUTHOR')))
  return <><header className="page-heading"><span className="eyebrow">Les regards se croisent</span><h1>Mes discussions</h1><p>Retrouvez les observations auxquelles vous avez contribué.</p></header><div className="page-tabs" aria-label="Filtrer les discussions">{[['all', 'Toutes'], ['author', 'Mes observations'], ['participant', 'Mes contributions']].map(([value, label]) => <button className={filter === value ? 'active' : ''} aria-pressed={filter === value} key={value} onClick={() => setFilter(value)}>{label}</button>)}</div>
    {!features.conversations ? <ComingSoon title="Les échanges prennent racine" icon="chat">Vous retrouverez ici les observations que vous avez publiées, commentées ou aidé à identifier.</ComingSoon> : <>
      {query.isPending && <Loading />}{query.isError && <ErrorState error={query.error} retry={() => query.refetch()} />}
      {rows && (rows.length ? <div className="list-panel">{rows.map(row => <Link className="list-row" to={`/observations/${row.observationId}`} key={row.observationId}><span className="list-thumbnail"><Photo path={row.coverUrl} alt="" /></span><div className="list-text"><h3>{row.title || 'Une rencontre avec le vivant'}</h3><div className="tag-row"><StatusBadge status={row.status} /><span className="field-help">{row.myRole.map(role => ({ AUTHOR: 'Votre observation', COMMENTER: 'Vous avez commenté', PROPOSER: 'Vous avez proposé une espèce' })[role]).join(' · ')}</span></div></div><div className="list-end"><time dateTime={row.lastActivity}>{formatDate(row.lastActivity, { day: 'numeric', month: 'short' })}</time><Icon name="arrow" size={16} /></div></Link>)}</div> : <EmptyState title="La discussion commence avec vous" icon="chat" action={<Link className="button" to="/">Explorer les observations</Link>}>Partagez une observation ou prenez part à une identification pour la retrouver ici.</EmptyState>)}
    </>}
  </>
}
