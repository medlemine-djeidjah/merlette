import { useEffect, useRef, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate } from 'react-router'
import { getCategories } from '../../api/categories'
import { createObservation } from '../../api/observations'
import { errorMessage } from '../../api/client'
import { localToday } from '../../utils/format'
import { ErrorState } from '../common/Feedback'
import Icon from '../common/Icon'

export default function ObservationForm() {
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const [photos, setPhotos] = useState<{ file: File; url: string }[]>([])
  const urls = useRef(new Set<string>())
  const [error, setError] = useState('')
  const [dragging, setDragging] = useState(false)
  const cache = useQueryClient()
  const navigate = useNavigate()
  const mutation = useMutation({ mutationFn: createObservation })
  const [today] = useState(localToday)
  useEffect(() => { const tracked = urls.current; return () => tracked.forEach(url => URL.revokeObjectURL(url)) }, [])
  function selectFiles(selected: File[]) {
    if (mutation.isPending) return
    const unique = selected.filter(file => !photos.some(p => p.file.name === file.name && p.file.size === file.size && p.file.lastModified === file.lastModified))
    if (unique.some(file => !['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 5 * 1024 * 1024 || file.size === 0)) { setError('Choisissez des photos JPEG, PNG ou WEBP, non vides, de 5 Mo maximum chacune.'); return }
    if (photos.length + unique.length > 5) { setError('Vous pouvez ajouter jusqu’à 5 photos. Retirez-en une avant de continuer.'); return }
    setError('')
    setPhotos([...photos, ...unique.map(file => { const url = URL.createObjectURL(file); urls.current.add(url); return { file, url } })])
  }
  function removePhoto(index: number) {
    const photo = photos[index]; URL.revokeObjectURL(photo.url); urls.current.delete(photo.url)
    setPhotos(photos.filter((_, i) => i !== index))
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError('')
    if (!photos.length) { setError('Ajoutez au moins une photo pour partager votre observation.'); document.getElementById('photo-input')?.focus(); return }
    const form = new FormData(event.currentTarget)
    form.delete('photos'); photos.forEach(photo => form.append('photos', photo.file))
    for (const key of ['latitude', 'longitude']) if (!form.get(key)) form.delete(key)
    try {
      const observation = await mutation.mutateAsync(form)
      await cache.invalidateQueries({ queryKey: ['observations'] })
      navigate(`/observations/${observation.id}`, { state: { published: true } })
    } catch (err) { setError(errorMessage(err)) }
  }
  return <><header className="page-heading"><Link className="back-link" to="/"><Icon name="back" size={15} />Retour aux observations</Link><h1>Une rencontre à partager.</h1><p>Quelques détails suffisent pour que la communauté puisse vous aider.</p></header>
    <div className="form-layout"><form id="observation-create" className="form-stack" onSubmit={submit}>
      {error && <div role="alert" className="feedback feedback-error"><Icon name="info" size={18} /><p>{error}</p></div>}
      <section className="panel"><div className="panel-heading"><span className="step-number">1</span><h2>Vos photos</h2><span className="pill" style={{ marginLeft: 'auto' }}>{photos.length} / 5</span></div>
        <div className={`upload-zone ${dragging ? 'is-dragging' : ''}`} onDragOver={e => { e.preventDefault(); setDragging(true) }} onDragLeave={() => setDragging(false)} onDrop={e => { e.preventDefault(); setDragging(false); selectFiles(Array.from(e.dataTransfer.files)) }}>
          <Icon name="upload" size={30} /><strong>Déposez vos photos ici</strong><p>ou choisissez des fichiers sur votre appareil</p><label className="button button-secondary button-small" htmlFor="photo-input">Choisir des photos</label><input id="photo-input" className="sr-only" type="file" name="photos" accept="image/jpeg,image/png,image/webp" multiple disabled={mutation.isPending} onChange={e => { selectFiles(Array.from(e.target.files || [])); e.target.value = '' }} /><p>JPEG, PNG ou WEBP · 5 Mo maximum par photo</p>
        </div>{photos.length > 0 && <div className="upload-previews" style={{ marginTop: 18 }}>{photos.map((photo, i) => <div className="upload-preview" key={photo.url}><img src={photo.url} alt={`Aperçu de ${photo.file.name}`} /><button type="button" disabled={mutation.isPending} aria-label={`Retirer ${photo.file.name}`} onClick={() => removePhoto(i)}><Icon name="close" size={13} /></button>{i === 0 && <span className="upload-caption">Photo de couverture</span>}</div>)}</div>}
      </section><section className="panel"><div className="panel-heading"><span className="step-number">2</span><h2>Les petits détails</h2></div><div className="form-stack">
        <label className="field">Titre <span className="optional">Facultatif</span><input name="title" placeholder="Ex. Une visite au jardin" maxLength={120} /></label>
        {categories.isError && <ErrorState error={categories.error} retry={() => categories.refetch()} />}
        <div className="field-grid"><label className="field">Catégorie *<select name="categoryId" required defaultValue="" disabled={categories.isPending}><option value="" disabled>{categories.isPending ? 'Chargement…' : 'Choisir une catégorie'}</option>{categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}</select></label><label className="field">Date d’observation *<input name="observedAt" type="date" required max={today} defaultValue={today} /></label></div>
        <label className="field">Lieu <span className="optional">Facultatif</span><input name="locationName" placeholder="Un parc, un sentier, votre jardin…" maxLength={200} /><small>Un lieu approximatif suffit. Évitez de partager une adresse personnelle.</small></label>
        <label className="field">Ce que vous avez remarqué <span className="optional">Facultatif</span><textarea name="description" placeholder="Sa couleur, sa taille, son comportement… Chaque détail peut aider." maxLength={10000} rows={4} /></label>
        <details className="coordinates-toggle"><summary>Ajouter des coordonnées géographiques</summary><div className="field-grid"><label className="field">Latitude<input name="latitude" type="number" min={-90} max={90} step="0.000001" placeholder="Ex. 43.676" /></label><label className="field">Longitude<input name="longitude" type="number" min={-180} max={180} step="0.000001" placeholder="Ex. 4.630" /></label></div></details>
      </div></section>
    </form><aside className="form-sidebar"><span className="eyebrow">Un coup de pouce</span><h2 style={{ marginTop: 12 }}>Avant de publier</h2><p>Une bonne observation aide les autres à reconnaître l’espèce.</p><ul className="checklist"><li><Icon name="check" size={17} />Une photo nette et bien éclairée</li><li><Icon name="check" size={17} />Une catégorie choisie</li><li><Icon name="check" size={17} />La date de votre rencontre</li><li><Icon name="check" size={17} />Un lieu ou quelques indices</li></ul><button form="observation-create" className="button button-full" disabled={mutation.isPending || !categories.data?.length}>{mutation.isPending ? 'Publication en cours…' : 'Publier l’observation'}<Icon name="arrow" size={16} /></button><p className="draft-note">Votre brouillon n’est pas enregistré.<br />* Champs obligatoires</p></aside></div>
  </>
}
