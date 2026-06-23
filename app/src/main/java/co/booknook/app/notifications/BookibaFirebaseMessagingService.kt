package co.booknook.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import co.booknook.app.MainActivity
import co.booknook.core.network.api.BookibaApi
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BookibaFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var api: BookibaApi

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        scope.launch {
            try {
                // FcmTokenRequest should be defined in BookibaApi or here
                api.uploadFcmToken(co.booknook.core.network.model.FcmTokenRequest(token))
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val orderId = message.data["orderId"]
        val status = message.data["status"]
        val targetRoute = message.data["target_route"]

        if (orderId != null && status != null) {
            showNotification(orderId, status, targetRoute)
        }
    }

    private fun showNotification(orderId: String, status: String, targetRoute: String?) {
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "bookiba_orders_fcm"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Order Updates (Instant)",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Real-time updates about your orders"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            if (targetRoute != null) {
                putExtra("target_route", targetRoute)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            orderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val text = when (status) {
            "Processing" -> "Your order #${orderId.take(8)} is now being processed."
            "Shipped" -> "Your order #${orderId.take(8)} has shipped!"
            "Delivered" -> "Your order #${orderId.take(8)} has been delivered."
            "Cancelled" -> "Your order #${orderId.take(8)} was cancelled."
            else -> "Order #${orderId.take(8)} status changed to $status"
        }

        val notification = NotificationCompat.Builder(this, channelId)
            // Use standard app icon instead of missing ic_notification
            .setSmallIcon(co.booknook.app.R.mipmap.ic_launcher)
            .setContentTitle("Order Update")
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(orderId.hashCode(), notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }
}
