import type { CSSProperties } from 'react'
const paths = {
  leaf: 'M20 4C10 2 3 7 5 14c2 7 13 7 15-10ZM5 21 16 10M9 17v-5m4 1h5',
  grid: 'M3 3h7v7H3zM14 3h7v7h-7zM3 14h7v7H3zM14 14h7v7h-7z',
  camera: 'M4 7h4l2-3h4l2 3h4v13H4zM16 13a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  plus: 'M12 5v14M5 12h14',
  arrow: 'M5 12h14m-6-6 6 6-6 6',
  back: 'M19 12H5m6-6-6 6 6 6',
  search: 'M21 21l-6-6M17 10a7 7 0 1 1-14 0 7 7 0 0 1 14 0',
  bell: 'M5 17h14l-2-4V9A5 5 0 0 0 7 9v4zM10 21h4',
  chat: 'M4 4h16v12H9l-5 4z',
  user: 'M16 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0M4 21v-3a8 8 0 0 1 16 0v3',
  check: 'm5 12 4 4L19 6',
  close: 'm6 6 12 12M6 18 18 6',
  menu: 'M4 6h16M4 12h16M4 18h16',
  pin: 'M12 22s7-7 7-14A7 7 0 0 0 5 8c0 7 7 14 7 14ZM15 8a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  calendar: 'M4 5h16v16H4zM8 2v6m8-6v6M4 11h16',
  upload: 'M12 16V3m-5 5 5-5 5 5M4 15v6h16v-6',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0',
  logout: 'M10 4H4v16h6M10 12h11m-5-5 5 5-5 5',
  settings: 'M12 3v3m0 12v3M3 12h3m12 0h3M5.6 5.6l2.1 2.1m8.6 8.6 2.1 2.1m0-12.8-2.1 2.1m-8.6 8.6-2.1 2.1M16 12a4 4 0 1 1-8 0 4 4 0 0 1 8 0',
  info: 'M12 11v6m0-10v1M22 12A10 10 0 1 1 2 12a10 10 0 0 1 20 0',
  trash: 'M3 6h18M9 6V3h6v3M6 6l1 15h10l1-15M10 10v7m4-7v7',
  edit: 'm15 4 5 5M4 20l5-1L21 7l-5-5L4 14z',
  heart: 'M12 21 3 12a5 5 0 0 1 9-6 5 5 0 0 1 9 6z',
} as const
export type IconName = keyof typeof paths
export default function Icon({ name, size = 20, className = '', style }: { name: IconName; size?: number; className?: string; style?: CSSProperties }) {
  return <svg className={className} style={style} width={size} height={size} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.65" strokeLinecap="round" strokeLinejoin="round" aria-hidden="true"><path d={paths[name]} /></svg>
}
