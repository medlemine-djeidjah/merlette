import { api } from './client'
import type { Proposals } from '../types'
export async function getProposals(id: number) { return (await api.get<Proposals>(`/observations/${id}/proposals`)).data }
export async function proposeSpecies(id: number, speciesId: number) { return (await api.post(`/observations/${id}/proposals`, { speciesId })).data }
export async function vote(id: number, proposalId: number) { return (await api.put(`/observations/${id}/vote`, { proposalId })).data }
export async function removeVote(id: number) { return (await api.delete(`/observations/${id}/vote`)).data }
export async function removeProposal(id: number) { await api.delete(`/proposals/${id}`) }
