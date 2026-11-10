import type { Notification } from '../../types'
import { formatDate } from '../../utils/format'
import Icon from '../common/Icon'
export default function NotificationItem({ notification: n, onOpen, pending }: { notification: Notification; onOpen: () => void; pending: boolean }) {
  return <button className={`list-row ${n.isRead ? '' : 'unread'}`} disabled={pending} onClick={onOpen}><span className="notification-symbol"><Icon name={n.type === 'NEW_COMMENT' ? 'chat' : n.type === 'OBSERVATION_IDENTIFIED' ? 'check' : 'leaf'} /></span><div className="list-text"><p className="notification-message" style={{ color: 'var(--ink)', fontWeight: n.isRead ? 400 : 600 }}>{n.message}</p><p className="notification-time">{n.actor?.username ? `${n.actor.username} · ` : ''}{formatDate(n.createdAt, { day: 'numeric', month: 'long', hour: '2-digit', minute: '2-digit' })}{!n.isRead && ' · Non lue'}</p></div><Icon name="arrow" size={16} /></button>
}
