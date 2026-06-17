package co.booknook.app.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import co.booknook.app.MainActivity
import co.booknook.core.datastore.NotificationPreferencesDataSource
import co.booknook.core.domain.repository.OrderRepository
import co.booknook.core.domain.repository.WishlistRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import co.booknook.app.R

@HiltWorker
class BookibaNotificationWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val orderRepository: OrderRepository,
    private val wishlistRepository: WishlistRepository,
    private val notificationPreferences: NotificationPreferencesDataSource
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        createNotificationChannels()

        try {
            checkOrderUpdates()
            checkWishlistPriceDrops()
            return Result.success()
        } catch (e: Exception) {
            return Result.retry()
        }
    }

    private suspend fun checkOrderUpdates() {
        val currentOrders = orderRepository.getOrders().first()
        val lastKnownStatuses = notificationPreferences.lastKnownOrderStatuses.first()
        
        var hasChanges = false
        val newStatuses = lastKnownStatuses.toMutableMap()

        for (order in currentOrders) {
            val oldStatus = lastKnownStatuses[order.id]
            if (oldStatus != null && oldStatus != order.status.name && order.status.name != "PENDING") {
                // Status changed! Fire notification
                showNotification(
                    id = order.id.hashCode(),
                    channelId = CHANNEL_ORDERS,
                    title = "Order Update",
                    text = "Your order status changed to ${order.status.name}"
                )
            }
            if (newStatuses[order.id] != order.status.name) {
                newStatuses[order.id] = order.status.name
                hasChanges = true
            }
        }

        if (hasChanges) {
            notificationPreferences.updateOrderStatuses(newStatuses)
        }
    }

    private suspend fun checkWishlistPriceDrops() {
        val currentWishlist = wishlistRepository.getWishlist().first()
        val lastKnownPrices = notificationPreferences.lastKnownWishlistPrices.first()

        var hasChanges = false
        val newPrices = lastKnownPrices.toMutableMap()

        for (book in currentWishlist) {
            val oldPrice = lastKnownPrices[book.id]
            val newPrice = book.priceKsh.toLong()
            if (oldPrice != null && newPrice < oldPrice) {
                // Price dropped! Fire notification
                showNotification(
                    id = book.id.hashCode(),
                    channelId = CHANNEL_WISHLIST,
                    title = "Price Drop Alert! \uD83D\uDD25",
                    text = "${book.title} has dropped from KSh $oldPrice to KSh $newPrice"
                )
            }
            if (newPrices[book.id] != newPrice) {
                newPrices[book.id] = newPrice
                hasChanges = true
            }
        }

        if (hasChanges) {
            notificationPreferences.updateWishlistPrices(newPrices)
        }
    }

    private fun showNotification(id: Int, channelId: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                return
            }
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, channelId)
            // fallback to default android icon since we don't have a custom one yet
            .setSmallIcon(android.R.drawable.ic_dialog_info) 
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        with(NotificationManagerCompat.from(context)) {
            notify(id, builder.build())
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val orderChannel = NotificationChannel(
                CHANNEL_ORDERS,
                "Order Updates",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for order status changes" }

            val wishlistChannel = NotificationChannel(
                CHANNEL_WISHLIST,
                "Wishlist Alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "Notifications for price drops on wishlist items" }

            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(orderChannel)
            notificationManager.createNotificationChannel(wishlistChannel)
        }
    }

    companion object {
        const val CHANNEL_ORDERS = "bookiba_orders"
        const val CHANNEL_WISHLIST = "bookiba_wishlist"
    }
}
