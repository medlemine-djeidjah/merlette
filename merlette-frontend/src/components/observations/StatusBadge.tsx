import Icon from '../common/Icon'
export default function StatusBadge({ status }: { status: 'NEEDS_ID' | 'IDENTIFIED' }) {
  return <span className={`status-badge ${status === 'IDENTIFIED' ? 'identified' : ''}`}>{status === 'IDENTIFIED' ? <Icon name="check" size={13} /> : <span className="status-dot" />}{status === 'IDENTIFIED' ? 'Identifiée' : 'À identifier'}</span>
}
