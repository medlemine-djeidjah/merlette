// Enable each module only after its endpoints are deployed (see docs/frontend-design.md).
export const features = {
  identification: import.meta.env.VITE_IDENTIFICATION_ENABLED === 'true',
  comments: import.meta.env.VITE_COMMENTS_ENABLED === 'true',
  notifications: import.meta.env.VITE_NOTIFICATIONS_ENABLED === 'true',
  profiles: import.meta.env.VITE_PROFILES_ENABLED === 'true',
  conversations: import.meta.env.VITE_CONVERSATIONS_ENABLED === 'true',
  management: import.meta.env.VITE_MANAGEMENT_ENABLED === 'true',
  deletion: import.meta.env.VITE_DELETION_ENABLED !== 'false',
  administration: import.meta.env.VITE_ADMINISTRATION_ENABLED === 'true',
}
