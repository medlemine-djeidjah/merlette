import { useEffect, useRef } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { getCategories } from '../../api/categories'
import { addPhotos, deletePhoto, updateObservation } from '../../api/observations'
import type { Observation } from '../../types'
import { localToday } from '../../utils/format'
import { ErrorState } from '../common/Feedback'
import Icon from '../common/Icon'
import Photo from '../common/Photo'
export default function ObservationEditor({ observation: o, onClose }: { observation: Observation; onClose: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  const cache = useQueryClient()
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const save = useMutation({ mutationFn: (input: Record<string, string | number | null>) => updateObservation(o.id, input), onSuccess: async () => { await refresh(); onClose() } })
  const photoAction = useMutation({ mutationFn: async (input: { id?: number; files?: FileList }) => {
    if (input.id !== undefined) { await deletePhoto(o.id, input.id); return }
    const files = Array.from(input.files || [])
    if (!files.length || files.length + (o.photos?.length || 0) > 5 || files.some(f => f.size > 5 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(f.type))) throw new Error('Ajoutez 1 à 5 photos au total, de 5 Mo maximum chacune.')
    const form = new FormData(); files.forEach(file => form.append('photos', file)); await addPhotos(o.id, form)
  }, onSuccess: refresh })
  const busy = save.isPending || photoAction.isPending
  async function refresh() { await Promise.all([cache.invalidateQueries({ queryKey: ['observation', String(o.id)] }), cache.invalidateQueries({ queryKey: ['observations'] })]) }
  useEffect(() => { dialog.current?.showModal() }, [])
  function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); const form = new FormData(event.currentTarget)
    save.mutate({ title: String(form.get('title')), description: String(form.get('description')), locationName: String(form.get('locationName')), categoryId: Number(form.get('categoryId')), observedAt: String(form.get('observedAt')) })
  }
  return <dialog className="modal-dialog" ref={dialog} onCancel={e => { if (busy) e.preventDefault(); else onClose() }} aria-labelledby="edit-title"><div className="section-heading"><h2 id="edit-title">Affiner votre observation</h2><button className="icon-button" disabled={busy} onClick={onClose} aria-label="Fermer"><Icon name="close" /></button></div>
    {save.isError && <ErrorState error={save.error} />}{photoAction.isError && <ErrorState error={photoAction.error} />}
    <form className="form-stack" onSubmit={submit}><label className="field">Titre<input name="title" maxLength={120} defaultValue={o.title || ''} /></label><div className="field-grid"><label className="field">Catégorie<select name="categoryId" required defaultValue={o.category.id}>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label className="field">Date<input name="observedAt" type="date" required max={localToday()} defaultValue={o.observedAt} /></label></div><label className="field">Lieu<input name="locationName" maxLength={200} defaultValue={o.locationName || ''} /></label><label className="field">Description<textarea name="description" maxLength={10000} defaultValue={o.description || ''} /></label>
      <div className="upload-previews">{o.photos?.map(photo => <div className="upload-preview" key={photo.id}><Photo path={photo.url} alt="Photo de l’observation" /><button type="button" aria-label="Supprimer cette photo" disabled={busy || (o.photos?.length || 0) <= 1} onClick={() => photoAction.mutate({ id: photo.id })}><Icon name="close" size={13} /></button></div>)}</div>
      <label className="field">Ajouter des photos<input type="file" accept="image/jpeg,image/png,image/webp" multiple disabled={busy || (o.photos?.length || 0) >= 5} onChange={e => { if (e.target.files?.length) photoAction.mutate({ files: e.target.files }); e.target.value = '' }} /><small>De 1 à 5 photos au total. La dernière photo doit être conservée.</small></label>
      <div className="dialog-actions"><button type="button" className="button button-secondary" disabled={busy} onClick={onClose}>Fermer</button><button className="button" disabled={busy || !categories.data}>{save.isPending ? 'Enregistrement…' : 'Enregistrer les modifications'}</button></div>
    </form>
  </dialog>
}
