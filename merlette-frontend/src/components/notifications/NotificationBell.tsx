import { Link } from 'react-router'
import { useNotifications } from '../../hooks/useNotifications'
import Icon from '../common/Icon'
export default function NotificationBell() {
  const { data } = useNotifications()
  return <Link className="notification-button" to="/notifications" aria-label={`Notifications${data?.count ? `, ${data.count} non lues` : ''}`}><Icon name="bell" size={19} />{!!data?.count && <span className="notification-dot">{data.count > 9 ? '9+' : data.count}</span>}</Link>
}
