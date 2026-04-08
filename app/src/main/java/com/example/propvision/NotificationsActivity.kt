package com.example.propvision

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class NotificationsActivity : AppCompatActivity() {

    private lateinit var notificationsContainer: LinearLayout
    private lateinit var tvEmpty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notifications)

        notificationsContainer = findViewById(R.id.notificationsContainer)
        tvEmpty = findViewById(R.id.tvEmpty)

        findViewById<View>(R.id.backBtn).setOnClickListener {
            finish()
        }

        loadNotifications()
        
        // Mark all as read when user opens this screen
        NotificationRepository.markAllAsRead()
    }

    private fun loadNotifications() {
        notificationsContainer.removeAllViews()
        val list = NotificationRepository.notifications

        if (list.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
        } else {
            tvEmpty.visibility = View.GONE
            val inflater = LayoutInflater.from(this)
            for (notification in list) {
                val itemView = inflater.inflate(R.layout.item_notification, notificationsContainer, false)
                
                itemView.findViewById<TextView>(R.id.tvNotificationTitle).text = notification.title
                itemView.findViewById<TextView>(R.id.tvNotificationMessage).text = notification.message
                itemView.findViewById<TextView>(R.id.tvNotificationTime).text = notification.time
                
                // If notification was unread, you could highlight it (optional)
                
                notificationsContainer.addView(itemView)
            }
        }
    }
}
