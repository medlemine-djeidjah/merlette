import { Outlet } from 'react-router'
import Navbar from './Navbar'

// Mise en page commune : barre de navigation + contenu de la page
export default function Layout() {
  return (
    <div className="min-h-screen bg-base-200">
      <Navbar />
      <main className="container mx-auto p-4">
        <Outlet />
      </main>
    </div>
  )
}
