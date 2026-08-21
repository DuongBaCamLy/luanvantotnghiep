import { api } from "./axios";

export interface NotificationDto {
  id: number;
  userId: number;
  title: string;
  message: string;
  type: string;
  isRead: boolean;
  createdAt: string;
}

export const notificationApi = {
  getUserNotifications: async (userId: number): Promise<NotificationDto[]> => {
    const response = await api.get(`/api/notifications/user/${userId}`);
    return response.data;
  },

  markAsRead: async (id: number): Promise<void> => {
    await api.put(`/api/notifications/${id}/read`);
  },

  markAllAsRead: async (userId: number): Promise<void> => {
    await api.put(`/api/notifications/user/${userId}/read-all`);
  },
};
