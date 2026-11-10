import { useState } from 'react'
import { Link, useLocation } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getProposals, proposeSpecies, removeProposal, removeVote, vote } from '../../api/proposals'
import { features } from '../../config/features'
import { useAuth } from '../../hooks/useAuth'
import type { Observation, Species } from '../../types'
import { EmptyState, ErrorState } from '../common/Feedback'
import SpeciesSearch from './SpeciesSearch'
import Icon from '../common/Icon'
export default function ProposalList({ observation }: { observation: Observation }) {
  const { user } = useAuth()
  const location = useLocation()
  const cache = useQueryClient()
  const [searching, setSearching] = useState(false)
  const query = useQuery({ queryKey: ['proposals', observation.id], queryFn: () => getProposals(observation.id), enabled: features.identification })
  const action = useMutation({ mutationFn: async ({ kind, id }: { kind: 'vote' | 'removeVote' | 'removeProposal' | 'propose'; id: number }) => {
    if (kind === 'vote') return vote(observation.id, id)
    if (kind === 'removeVote') return removeVote(observation.id)
    if (kind === 'removeProposal') return removeProposal(id)
    return proposeSpecies(observation.id, id)
  }, onSuccess: async () => { setSearching(false); await Promise.all([cache.invalidateQueries({ queryKey: ['proposals', observation.id] }), cache.invalidateQueries({ queryKey: ['observation', String(observation.id)] }), cache.invalidateQueries({ queryKey: ['observations'] }), cache.invalidateQueries({ queryKey: ['notification-count'] })]) } })
  function select(species: Species) { action.mutate({ kind: 'propose', id: species.id }) }
  if (!features.identification) return <section className="detail-section"><h2>Les regards de la communauté</h2><p className="section-subtitle">Reconnaître le vivant, ensemble.</p><div className="soft-note"><Icon name="leaf" size={19} /><p style={{ marginTop: 10 }}>Les propositions d’espèces et les votes arrivent bientôt. Votre observation est déjà visible et prête à être découverte.</p></div></section>
  return <section className="detail-section"><div className="section-heading"><h2>Propositions de la communauté</h2>{query.data?.consensus.reached && <span className="status-badge identified"><Icon name="check" size={13} />Consensus atteint</span>}</div>
    {query.isLoading && <p role="status" className="section-subtitle">Chargement des propositions…</p>}{query.isError && <ErrorState error={query.error} retry={() => query.refetch()} />}{action.isError && <ErrorState error={action.error} />}
    {query.data && <><p className="section-subtitle">{query.data.totalVotes} vote(s) · {query.data.proposals.length} proposition(s)</p>{!query.data.proposals.length && <EmptyState title="Un nom à trouver">Vous reconnaissez cette espèce ? Proposez une identification pour ouvrir la discussion.</EmptyState>}
      {query.data.proposals.map(proposal => {
        const selected = !!user && proposal.voters.some(voter => voter.id === user.id)
        const percent = Math.max(0, Math.min(100, proposal.percent))
        return <article key={proposal.id} className={`proposal-card ${selected ? 'selected' : ''}`}><div className="proposal-top"><div><h3>{proposal.species.commonName}</h3><p className="species-latin">{proposal.species.scientificName}</p><span className="field-help">Proposée par {proposal.proposedBy.username}</span></div>{user && <button className={`button button-small ${selected ? '' : 'button-secondary'}`} disabled={action.isPending} onClick={() => action.mutate({ kind: selected ? 'removeVote' : 'vote', id: proposal.id })}>{selected ? 'Retirer mon vote' : 'Voter'}</button>}</div>
          <div className="vote-bar" role="meter" aria-label={`Votes pour ${proposal.species.commonName}`} aria-valuenow={percent} aria-valuemin={0} aria-valuemax={100}><span style={{ width: `${percent}%` }} /></div><div className="vote-meta"><span>{proposal.votes} vote(s) · {percent.toLocaleString('fr-FR', { maximumFractionDigits: 1 })} %</span>{selected && <span>Votre vote</span>}</div>
          <details style={{ marginTop: 10 }} className="field-help"><summary>Voir les personnes ayant voté</summary><p>{proposal.voters.map(v => v.username).join(', ') || 'Aucun vote'}</p></details>
          {user?.id === proposal.proposedBy.id && !proposal.voters.some(v => v.id !== user.id) && <button className="text-link" style={{ marginTop: 10 }} disabled={action.isPending} onClick={() => action.mutate({ kind: 'removeProposal', id: proposal.id })}>Retirer ma proposition</button>}
        </article>
      })}</>}
    {user ? <><button className="button button-secondary button-full" style={{ marginTop: 15 }} aria-expanded={searching} onClick={() => setSearching(!searching)}><Icon name="plus" size={16} />Proposer une espèce</button>{searching && <SpeciesSearch category={observation.category.slug} onSelect={select} disabled={action.isPending} />}</> : <div className="soft-note" style={{ marginTop: 20 }}>Connectez-vous pour proposer une espèce ou voter. <Link className="text-link" to="/login" state={{ from: location.pathname }}>Se connecter</Link></div>}
  </section>
}
