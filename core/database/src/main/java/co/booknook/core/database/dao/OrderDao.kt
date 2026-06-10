package co.booknook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import co.booknook.core.database.model.OrderEntity
import co.booknook.core.database.model.OrderItemEntity
import co.booknook.core.database.model.OrderWithItems
import kotlinx.coroutines.flow.Flow

@Dao
interface OrderDao {

    @Transaction
    @Query("SELECT * FROM orders ORDER BY dateMs DESC")
    fun getOrders(): Flow<List<OrderWithItems>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrder(order: OrderEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrderItems(items: List<OrderItemEntity>)
}
