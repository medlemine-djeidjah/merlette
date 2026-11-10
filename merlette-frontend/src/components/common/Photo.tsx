import { useState } from 'react'
import { imageUrl } from '../../api/client'
import Icon from './Icon'
export default function Photo({ path, alt, className = '', eager = false }: { path?: string | null; alt: string; className?: string; eager?: boolean }) {
  const src = imageUrl(path)
  const [failed, setFailed] = useState<string>()
  return src && failed !== src ? <img src={src} alt={alt} className={className} loading={eager ? 'eager' : 'lazy'} onError={() => setFailed(src)} /> : <span className={`image-placeholder ${className}`} role="img" aria-label={alt || 'Photo indisponible'}><Icon name="leaf" size={35} /></span>
}
