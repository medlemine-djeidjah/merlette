import { imageUrl } from '../../api/client'
import { initials } from '../../utils/format'
export default function Avatar({ name, url, large = false }: { name: string; url?: string | null; large?: boolean }) {
  return <span className={`avatar-circle ${large ? 'avatar-large' : ''}`} aria-label={name}>{imageUrl(url) ? <img src={imageUrl(url)} alt="" /> : initials(name)}</span>
}
