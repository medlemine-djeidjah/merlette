import { useQuery } from '@tanstack/react-query'
import { getCategories } from '../../api/categories'
import { ErrorState } from '../common/Feedback'
interface Props { category: string; status: string; sort: string; onChange: (key: string, value: string) => void }
export default function ObservationFilters({ category, status, sort, onChange }: Props) {
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  return <>{categories.isError && <ErrorState error={categories.error} retry={() => categories.refetch()} />}
    <div className="filter-bar"><div className="category-tabs" aria-label="Filtrer par catégorie"><button className={`category-tab ${!category ? 'selected' : ''}`} aria-pressed={!category} onClick={() => onChange('category', '')}>Toutes les catégories</button>
      {categories.data?.map(c => <button key={c.id} className={`category-tab ${category === c.slug ? 'selected' : ''}`} aria-pressed={category === c.slug} onClick={() => onChange('category', c.slug)}>{c.name}</button>)}
    </div><div className="filter-selects"><label><span className="sr-only">Statut d’identification</span><select className="select-clean" value={status} onChange={e => onChange('status', e.target.value)}><option value="">Tous les statuts</option><option value="NEEDS_ID">À identifier</option><option value="IDENTIFIED">Identifiées</option></select></label>
      <label><span className="sr-only">Trier les observations</span><select className="select-clean" value={sort} onChange={e => onChange('sort', e.target.value)}><option value="recent">Les plus récentes</option><option value="oldest">Les plus anciennes</option></select></label>
    </div></div>
  </>
}
