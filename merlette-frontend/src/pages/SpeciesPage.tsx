import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { getCategories } from '../api/categories'
import { features } from '../config/features'
import SpeciesSearch from '../components/identification/SpeciesSearch'
import { ComingSoon, ErrorState } from '../components/common/Feedback'
export default function SpeciesPage() {
  const [category, setCategory] = useState('')
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  return <div className="search-page"><header className="page-heading"><span className="eyebrow">Le petit répertoire du vivant</span><h1>Un nom à découvrir.</h1><p>Oiseaux, plantes, insectes… Retrouvez une espèce par son nom commun ou scientifique.</p></header><div className="panel"><label className="field">Explorer une catégorie<select value={category} onChange={e => setCategory(e.target.value)}><option value="">Toutes les catégories</option>{categories.data?.map(c => <option value={c.slug} key={c.id}>{c.name}</option>)}</select></label>{categories.isError && <ErrorState error={categories.error} retry={() => categories.refetch()} />}<SpeciesSearch category={category || undefined} disabled={!features.identification} browse /></div>{!features.identification && <div style={{ marginTop: 25 }}><ComingSoon title="Le répertoire prend forme">La recherche d’espèces sera bientôt disponible pour accompagner vos identifications.</ComingSoon></div>}</div>
}
