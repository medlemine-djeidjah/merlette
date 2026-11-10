import Icon from './Icon'
import type { PaginationData } from '../../types'
export default function Pagination({ pagination, onChange }: { pagination: PaginationData; onChange: (page: number) => void }) {
  if (pagination.totalPages < 2) return null
  const { page, totalPages } = pagination
  return <nav className="pagination" aria-label="Pagination">
    <button className="button button-secondary" disabled={page <= 1} onClick={() => onChange(page - 1)}><Icon name="back" /> Précédent</button>
    <span aria-live="polite">Page <strong>{page}</strong> sur {totalPages}</span>
    <button className="button button-secondary" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>Suivant <Icon name="arrow" /></button>
  </nav>
}
