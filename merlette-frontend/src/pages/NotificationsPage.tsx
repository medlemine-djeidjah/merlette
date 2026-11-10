import { useNavigate, useSearchParams } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getNotifications, markAllRead, markRead } from '../api/notifications'
import type { Notification } from '../types'
import { features } from '../config/features'
import { useAuth } from '../hooks/useAuth'
import NotificationItem from '../components/notifications/NotificationItem'
import Pagination from '../components/common/Pagination'
import { ComingSoon, EmptyState, ErrorState, Loading } from '../components/common/Feedback'
import Icon from '../components/common/Icon'
export default function NotificationsPage() {
  const { user } = useAuth()
  const [params, setParams] = useSearchParams()
  const unread = params.get('unread') === 'true'
  const rawPage = Number(params.get('page') || 1)
  const page = Number.isSafeInteger(rawPage) && rawPage > 0 ? rawPage : 1
  const cache = useQueryClient()
  const navigate = useNavigate()
  const query = useQuery({ queryKey: ['notifications', user?.id, page, unread], queryFn: () => getNotifications(page, unread), enabled: features.notifications && !!user, refetchInterval: 30000 })
  const action = useMutation({ mutationFn: async (notification?: Notification) => { if (notification) { if (!notification.isRead) await markRead(notification.id) } else await markAllRead() }, onSuccess: async (_, notification) => {
    await Promise.all([cache.invalidateQueries({ queryKey: ['notifications'] }), cache.invalidateQueries({ queryKey: ['notification-count'] })]); if (notification?.observationId != null) navigate(`/observations/${notification.observationId}`)
  } })
  return <><header className="page-heading"><div className="section-heading"><div><span className="eyebrow">Les nouvelles du carnet</span><h1>Notifications</h1></div><button className="button button-secondary button-small" disabled={!features.notifications || action.isPending || !query.data?.data.length} onClick={() => action.mutate(undefined)}><Icon name="check" size={15} />Tout marquer comme lu</button></div><p>Les nouveautés sur vos observations et vos discussions.</p></header>
    <div className="page-tabs"><button className={!unread ? 'active' : ''} aria-pressed={!unread} onClick={() => setParams({})}>Toutes</button><button className={unread ? 'active' : ''} aria-pressed={unread} onClick={() => setParams({ unread: 'true' })}>Non lues</button></div>
    {!features.notifications ? <ComingSoon title="Les nouvelles viendront à vous" icon="bell">Une proposition, un commentaire ou une identification : vous pourrez bientôt suivre chaque étape ici.</ComingSoon> : <>
      {query.isPending && <Loading />}{query.isError && <ErrorState error={query.error} retry={() => query.refetch()} />}{action.isError && <ErrorState error={action.error} />}
      {query.data && (query.data.data.length ? <><div className="list-panel">{query.data.data.map(n => <NotificationItem key={n.id} notification={n} pending={action.isPending} onOpen={() => action.mutate(n)} />)}</div><Pagination pagination={query.data.pagination} onChange={next => setParams({ page: String(next), ...(unread && { unread: 'true' }) })} /></> : <EmptyState title={unread ? 'Vous êtes à jour' : 'Tout est calme pour le moment'} icon="bell">Les nouvelles de vos observations apparaîtront ici.</EmptyState>)}
    </>}
  </>
}
