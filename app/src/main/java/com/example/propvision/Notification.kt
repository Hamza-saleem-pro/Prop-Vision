package com.example.propvision

import java.io.Serializable

data class Notification(
    val title: String,
    val message: String,
    val time: String,
    var isRead: Boolean = false
) : Serializable

object NotificationRepository {
    val notifications = mutableListOf<Notification>()
    
    fun addNotification(notification: Notification) {
        notifications.add(0, notification)
    }
    
    fun getUnreadCount(): Int {
        return notifications.count { !it.isRead }
    }
    
    fun markAllAsRead() {
        notifications.forEach { it.isRead = true }
    }
}
