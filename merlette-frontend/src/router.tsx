import { createBrowserRouter } from 'react-router'
import Layout from './components/layout/Layout'
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

export const router = createBrowserRouter([
  {
    path: '/',
    Component: Layout,
    children: [
      { index: true, Component: HomePage },
      { path: 'login', Component: LoginPage },
      { path: 'register', Component: RegisterPage },
      { path: 'observations/new', Component: NewObservationPage },
      { path: 'observations/:id', Component: ObservationDetailPage },
      { path: 'me/observations', Component: MyObservationsPage },
      { path: 'me/conversations', Component: MyConversationsPage },
      { path: 'notifications', Component: NotificationsPage },
      { path: 'users/:id', Component: ProfilePage },
      { path: 'admin', Component: AdminPage },
      { path: '*', Component: NotFoundPage },
    ],
  },
])
