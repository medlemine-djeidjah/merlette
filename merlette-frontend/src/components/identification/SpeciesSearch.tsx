import { useEffect, useId, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { searchSpecies } from '../../api/species'
import type { Species } from '../../types'
import { ErrorState } from '../common/Feedback'
import Icon from '../common/Icon'
export default function SpeciesSearch({ category, onSelect, disabled = false, browse = false }: { category?: string; onSelect?: (species: Species) => void; disabled?: boolean; browse?: boolean }) {
  const [value, setValue] = useState('')
  const [search, setSearch] = useState('')
  const [active, setActive] = useState(-1)
  const [closed, setClosed] = useState(false)
  const listId = useId()
  useEffect(() => { const timeout = setTimeout(() => setSearch(value.trim()), 300); return () => clearTimeout(timeout) }, [value])
  const query = useQuery({ queryKey: ['species', search, category], queryFn: () => searchSpecies(search, category), enabled: search.length >= 2 && value.trim().length >= 2 && !disabled && !closed })
  const results = search === value.trim() && !closed ? query.data || [] : []
  const expanded = !disabled && !query.isFetching && results.length > 0
  function choose(species: Species) { setValue(species.commonName); setClosed(true); setActive(-1); onSelect?.(species) }
  return <div className="species-search"><label className="field">{browse ? 'Nom commun ou nom scientifique' : 'Quelle espèce avez-vous reconnue ?'}<input type="search" value={value} onChange={e => { setValue(e.target.value); setActive(-1); setClosed(false) }} onFocus={() => setClosed(false)} placeholder="Ex. Mésange bleue ou Cyanistes…" maxLength={150} disabled={disabled} autoComplete="off" role={onSelect ? 'combobox' : undefined} aria-autocomplete={onSelect ? 'list' : undefined} aria-expanded={onSelect ? expanded : undefined} aria-controls={onSelect && expanded ? listId : undefined} aria-activedescendant={onSelect && expanded && active >= 0 && active < results.length ? `${listId}-${active}` : undefined} onKeyDown={e => {
    if (!onSelect) return
    if (e.key === 'Escape') { setClosed(true); setActive(-1) }
    if (!expanded) return
    if (e.key === 'ArrowDown') { e.preventDefault(); setActive((active + 1) % results.length) }
    if (e.key === 'ArrowUp') { e.preventDefault(); setActive((active <= 0 ? results.length : active) - 1) }
    if (e.key === 'Enter' && active >= 0 && active < results.length) { e.preventDefault(); choose(results[active]) }
  }} /><small>Au moins 2 caractères. Les accents ne sont pas nécessaires.</small></label>
    {value.trim().length >= 2 && !disabled && !closed && <>
      {(query.isFetching || search !== value.trim()) && <p className="field-help" role="status">Recherche en cours…</p>}
      {query.isError && <ErrorState error={query.error} retry={() => query.refetch()} />}
      {query.data && search === value.trim() && !query.isFetching && (query.data.length ? <div className="species-results" id={listId} role={onSelect ? 'listbox' : undefined} aria-label={onSelect ? 'Espèces trouvées' : undefined}>{query.data.map((species, index) => onSelect ? <button className="species-result" style={active === index ? { background: 'var(--light)' } : undefined} role="option" aria-selected={active === index} id={`${listId}-${index}`} disabled={disabled} key={species.id} onClick={() => choose(species)}><span>{species.commonName}<small>{species.scientificName}</small></span><Icon name="plus" size={16} /></button> : <article key={species.id} className="species-result"><span>{species.commonName}<small>{species.scientificName}</small></span>{species.category && <span className="pill">{species.category.name || species.category.slug}</span>}</article>)}</div> : <p className="soft-note">Aucune espèce trouvée. Essayez un autre nom ou une autre catégorie.</p>)}
    </>}
  </div>
}
