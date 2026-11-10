import { useRef, useState } from 'react'
import type { Observation } from '../../types'
import { imageUrl } from '../../api/client'
import Photo from '../common/Photo'
import Icon from '../common/Icon'
export default function PhotoGallery({ observation }: { observation: Observation }) {
  const photos = observation.photos?.length ? observation.photos : observation.coverUrl ? [{ id: 0, url: observation.coverUrl, position: 0 }] : []
  const [index, setIndex] = useState(0)
  const selected = photos[index] || photos[0]
  const dialog = useRef<HTMLDialogElement>(null)
  return <div className="detail-gallery"><button className="gallery-main" style={{ display: 'block', padding: 0, overflow: 'hidden' }} aria-label="Agrandir la photo" disabled={!selected} onClick={() => dialog.current?.showModal()}><Photo path={selected?.url} alt={observation.title || 'Photo de l’observation'} className="gallery-main" eager /></button>
    {photos.length > 1 && <div className="gallery-thumbnails">{photos.map((photo, i) => <button key={photo.id} className={index === i ? 'active' : ''} aria-label={`Voir la photo ${i + 1}`} aria-pressed={index === i} onClick={() => setIndex(i)}><Photo path={photo.url} alt={`Miniature ${i + 1}`} /></button>)}</div>}
    <p className="gallery-count"><Icon name="camera" size={14} />{photos.length} photo{photos.length > 1 ? 's' : ''} · Cliquez pour agrandir</p>
    <dialog className="lightbox" ref={dialog} aria-label="Photo agrandie"><button className="icon-button" aria-label="Fermer la photo agrandie" onClick={() => dialog.current?.close()}><Icon name="close" /></button><img src={imageUrl(selected?.url)} alt={observation.title || 'Photo agrandie de l’observation'} /></dialog>
  </div>
}
