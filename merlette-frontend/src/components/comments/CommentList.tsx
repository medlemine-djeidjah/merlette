import { useState } from 'react'
import type { FormEvent } from 'react'
import { Link, useLocation } from 'react-router'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { createComment, deleteComment, getComments, updateComment } from '../../api/comments'
import { features } from '../../config/features'
import { useAuth } from '../../hooks/useAuth'
import { formatDate } from '../../utils/format'
import Avatar from '../common/Avatar'
import { ErrorState } from '../common/Feedback'
import ConfirmDialog from '../common/ConfirmDialog'
import CommentForm from './CommentForm'
export default function CommentList({ observationId }: { observationId: number }) {
  const { user } = useAuth()
  const location = useLocation()
  const cache = useQueryClient()
  const [content, setContent] = useState('')
  const [editing, setEditing] = useState<number | null>(null)
  const [edited, setEdited] = useState('')
  const [deleting, setDeleting] = useState<number | null>(null)
  const query = useQuery({ queryKey: ['comments', observationId], queryFn: () => getComments(observationId), enabled: features.comments })
  const action = useMutation({ mutationFn: async ({ kind, id, text }: { kind: 'create' | 'update' | 'delete'; id?: number; text?: string }) => {
    if (kind === 'delete') await deleteComment(id!)
    else if (kind === 'update') await updateComment(id!, text!)
    else await createComment(observationId, text!)
  },
    onSuccess: async (_, input) => { if (input.kind === 'create') setContent(''); setEditing(null); setDeleting(null); await Promise.all([cache.invalidateQueries({ queryKey: ['comments', observationId] }), cache.invalidateQueries({ queryKey: ['observation', String(observationId)] }), cache.invalidateQueries({ queryKey: ['observations'] })]) } })
  function submit(event: FormEvent<HTMLFormElement>) { event.preventDefault(); if (content.trim()) action.mutate({ kind: 'create', text: content.trim() }) }
  return <section className="detail-section"><h2>Au fil de la discussion{query.data ? ` · ${query.data.length}` : ''}</h2>
    {!features.comments ? <p className="soft-note" style={{ marginTop: 18 }}>Un espace pour échanger vos indices et vos questions sera bientôt disponible ici.</p> : <>
      {query.isLoading && <p role="status" className="section-subtitle">Chargement de la discussion…</p>}{query.isError && <ErrorState error={query.error} retry={() => query.refetch()} />}{action.isError && !deleting && <ErrorState error={action.error} />}
      {query.data?.length === 0 && <p className="section-subtitle">Tout commence par un premier regard. Partagez un indice ou posez une question.</p>}
      {query.data?.map(comment => <article className="comment" key={comment.id}><div className="comment-header"><Avatar name={comment.author.username} /><Link to={`/users/${comment.author.id}`}>{comment.author.username}</Link><time dateTime={comment.createdAt}>{formatDate(comment.createdAt, { day: 'numeric', month: 'short', hour: '2-digit', minute: '2-digit' })}{comment.edited ? ' · modifié' : ''}</time></div>
        {editing === comment.id ? <form className="form-stack" onSubmit={e => { e.preventDefault(); if (edited.trim()) action.mutate({ kind: 'update', id: comment.id, text: edited.trim() }) }}><label className="field"><span className="sr-only">Modifier votre commentaire</span><textarea required minLength={1} maxLength={1000} value={edited} onChange={e => setEdited(e.target.value)} /></label><div className="page-actions"><button className="button button-small" disabled={action.isPending || !edited.trim()}>Enregistrer</button><button className="button button-quiet button-small" type="button" onClick={() => setEditing(null)}>Annuler</button></div></form> : <p>{comment.content}</p>}
        <div className="comment-actions">{user?.id === comment.author.id && <button onClick={() => { setEditing(comment.id); setEdited(comment.content) }}>Modifier</button>}{(user?.id === comment.author.id || user?.role === 'ADMIN') && <button disabled={action.isPending} onClick={() => setDeleting(comment.id)}>Supprimer</button>}</div>
      </article>)}
      {user ? <CommentForm content={content} onChange={setContent} onSubmit={submit} pending={action.isPending} /> : <p className="soft-note" style={{ marginTop: 18 }}><Link className="text-link" to="/login" state={{ from: location.pathname }}>Connectez-vous</Link> pour rejoindre la discussion.</p>}
      {deleting && <ConfirmDialog title="Supprimer ce commentaire ?" pending={action.isPending} onClose={() => { setDeleting(null); action.reset() }} onConfirm={() => action.mutate({ kind: 'delete', id: deleting })}><p>Cette action est définitive.</p>{action.isError && <ErrorState error={action.error} />}</ConfirmDialog>}
    </>}
  </section>
}
