import type { FormEvent } from 'react'
export default function CommentForm({ content, onChange, onSubmit, pending }: { content: string; onChange: (value: string) => void; onSubmit: (event: FormEvent<HTMLFormElement>) => void; pending: boolean }) {
  return <form className="comment-form" onSubmit={onSubmit}><label className="field">Votre regard compte<textarea value={content} onChange={e => onChange(e.target.value)} required minLength={1} maxLength={1000} placeholder="Un détail, un indice ou une question…" rows={3} /></label><div className="section-heading"><span className="field-help">{content.length} / 1 000 caractères</span><button className="button button-small" disabled={pending || !content.trim()}>{pending ? 'Envoi…' : 'Partager mon commentaire'}</button></div></form>
}
