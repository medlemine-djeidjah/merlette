import { createBrowserRouter } from 'react-router'
import Layout from './components/layout/Layout'
import WorkspaceLayout from './components/layout/WorkspaceLayout'
import ProtectedRoute from './components/layout/ProtectedRoute'
import HomePage from './pages/HomePage'
import LoginPage from './pages/LoginPage'
import RegisterPage from './pages/RegisterPage'
import NewObservationPage from './pages/NewObservationPage'
import ObservationDetailPage from './pages/ObservationDetailPage'
import MyObservationsPage from './pages/MyObservationsPage'
import MyConversationsPage from './pages/MyConversationsPage'
import NotificationsPage from './pages/NotificationsPage'
import ProfilePage from './pages/ProfilePage'
import AdminPage from './pages/AdminPage'
import NotFoundPage from './pages/NotFoundPage'
import HowItWorksPage from './pages/HowItWorksPage'
import SpeciesPage from './pages/SpeciesPage'
import RouteError from './pages/RouteError'

export const router = createBrowserRouter([{ Component: Layout, errorElement: <RouteError />, children: [
  { index: true, Component: HomePage },
  { path: 'login', Component: LoginPage }, { path: 'register', Component: RegisterPage },
  { path: 'observations/:id', Component: ObservationDetailPage },
  { path: 'users/:id', Component: ProfilePage },
  { path: 'comment-ca-marche', Component: HowItWorksPage }, { path: 'especes', Component: SpeciesPage },
  { Component: ProtectedRoute, children: [
    { path: 'observations/new', Component: NewObservationPage },
    { Component: WorkspaceLayout, children: [
      { path: 'me/observations', Component: MyObservationsPage },
      { path: 'me/conversations', Component: MyConversationsPage },
      { path: 'notifications', Component: NotificationsPage },
      { element: <ProtectedRoute admin />, children: [{ path: 'admin', Component: AdminPage }] },
    ] },
  ] },
  { path: '*', Component: NotFoundPage },
] }])
