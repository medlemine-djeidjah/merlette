import { useQuery } from '@tanstack/react-query'
import { unreadCount } from '../api/notifications'
import { features } from '../config/features'
import { useAuth } from './useAuth'
export function useNotifications() {
  const { user } = useAuth()
  return useQuery({ queryKey: ['notification-count', user?.id], queryFn: unreadCount, enabled: !!user && features.notifications, refetchInterval: 30000, retry: false })
}
