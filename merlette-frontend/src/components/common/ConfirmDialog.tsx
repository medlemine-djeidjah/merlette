import { useEffect, useRef } from 'react'
import type { ReactNode } from 'react'
import Icon from './Icon'
export default function ConfirmDialog({ title, children, onClose, onConfirm, pending = false }: { title: string; children: ReactNode; onClose: () => void; onConfirm: () => void; pending?: boolean }) {
  const ref = useRef<HTMLDialogElement>(null)
  useEffect(() => { ref.current?.showModal() }, [])
  return <dialog ref={ref} className="modal-dialog" onCancel={e => { if (pending) e.preventDefault(); else onClose() }} aria-labelledby="confirm-title">
    <div className="section-heading"><h2 id="confirm-title">{title}</h2><button className="icon-button" aria-label="Fermer" disabled={pending} onClick={onClose}><Icon name="close" /></button></div>
    <div className="muted">{children}</div><div className="dialog-actions"><button className="button button-secondary" disabled={pending} onClick={onClose}>Annuler</button><button className="button button-danger" disabled={pending} onClick={onConfirm}>{pending ? 'Suppression…' : 'Confirmer la suppression'}</button></div>
  </dialog>
}
