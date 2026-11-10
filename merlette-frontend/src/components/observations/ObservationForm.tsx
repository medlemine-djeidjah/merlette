// Formulaire photos + catégorie, date, lieu, description (#1, #13)
// TODO

import { useEffect, useState } from 'react'
import type { FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router'
import { getCategories } from '../../api/categories'
import { createObservation } from '../../api/observations'
import { errorMessage } from '../../api/client'

export default function ObservationForm() {
  const categories = useQuery({ queryKey: ['categories'], queryFn: getCategories })
  const [files, setFiles] = useState<File[]>([])
  const [previews, setPreviews] = useState<string[]>([])
  const [error, setError] = useState('')
  const cache = useQueryClient()
  const navigate = useNavigate()
  const mutation = useMutation({ mutationFn: createObservation })
  const [today] = useState(() => new Date().toLocaleDateString('en-CA'))
  useEffect(() => () => previews.forEach(url => URL.revokeObjectURL(url)), [previews])
  function selectFiles(selected: File[]) {
    setFiles(selected)
    setPreviews(selected.map(file => URL.createObjectURL(file)))
  }
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError('')
    if (!files.length || files.length > 5 || files.some(f => f.size > 5 * 1024 * 1024 || !['image/jpeg', 'image/png', 'image/webp'].includes(f.type))) {
      setError('Choisis entre 1 et 5 photos JPEG, PNG ou WEBP, de 5 Mo maximum chacune.'); return
    }
    const form = new FormData(event.currentTarget)
    form.delete('photos')
    files.forEach(file => form.append('photos', file))
    for (const key of ['latitude', 'longitude']) if (!form.get(key)) form.delete(key)
    try {
      const observation = await mutation.mutateAsync(form)
      await cache.invalidateQueries({ queryKey: ['observations'] })
      navigate(`/observations/${observation.id}`)
    } catch (err) { setError(errorMessage(err)) }
  }
  return <section className="card mx-auto my-6 max-w-2xl bg-base-100 shadow-sm"><div className="card-body gap-5">
    <h1 className="text-2xl font-bold">Partager une observation</h1>
    <p>Ajoute des photos et quelques informations pour présenter ta découverte.</p>
    {(error || categories.isError) && <p role="alert" className="alert alert-error">{error || errorMessage(categories.error)}</p>}
    <form onSubmit={submit} className="flex flex-col gap-4">
      <label className="flex flex-col gap-1">Photos *
        <input className="file-input w-full" type="file" name="photos" accept="image/jpeg,image/png,image/webp" multiple required
          onChange={e => selectFiles(Array.from(e.target.files || []))} />
        <span className="text-sm opacity-70">1 à 5 photos, 5 Mo maximum par photo.</span>
      </label>
      <div className="flex flex-wrap gap-2">{previews.map((url, i) => <img key={url} className="h-24 w-24 rounded-box object-cover" src={url} alt={`Aperçu ${i + 1}`} />)}</div>
      <label className="flex flex-col gap-1">Titre<input className="input w-full" name="title" maxLength={120} /></label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-1">Catégorie *
          <select className="select w-full" name="categoryId" required defaultValue="" disabled={categories.isPending}>
            <option value="" disabled>Choisir une catégorie</option>
            {categories.data?.map(c => <option key={c.id} value={c.id}>{c.name}</option>)}
          </select>
        </label>
        <label className="flex flex-col gap-1">Date d’observation *<input className="input w-full" name="observedAt" type="date" required max={today} defaultValue={today} /></label>
      </div>
      <label className="flex flex-col gap-1">Description<textarea className="textarea w-full" name="description" maxLength={10000} rows={4} /></label>
      <label className="flex flex-col gap-1">Lieu<input className="input w-full" name="locationName" maxLength={200} /></label>
      <div className="grid gap-4 sm:grid-cols-2">
        <label className="flex flex-col gap-1">Latitude<input className="input w-full" name="latitude" type="number" min={-90} max={90} step="0.000001" /></label>
        <label className="flex flex-col gap-1">Longitude<input className="input w-full" name="longitude" type="number" min={-180} max={180} step="0.000001" /></label>
      </div>
      <button className="btn btn-primary" disabled={mutation.isPending || !categories.data?.length}>{mutation.isPending ? 'Publication…' : 'Publier mon observation'}</button>
    </form>
  </div></section>
}
