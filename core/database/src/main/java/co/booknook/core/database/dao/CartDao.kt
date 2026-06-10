package co.booknook.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import co.booknook.core.database.model.CartItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CartDao {

    @Query("SELECT * FROM cart_items ORDER BY addedAt ASC")
    fun getCartItems(): Flow<List<CartItemEntity>>

    @Query("SELECT * FROM cart_items WHERE bookId = :bookId")
    fun getCartItem(bookId: String): Flow<CartItemEntity?>

    @Query("SELECT COUNT(*) FROM cart_items")
    fun getCartCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertItem(item: CartItemEntity)

    @Query("UPDATE cart_items SET quantity = :quantity WHERE bookId = :bookId")
    suspend fun updateQuantity(bookId: String, quantity: Int)

    @Query("UPDATE cart_items SET quantity = quantity - 1 WHERE bookId = :bookId AND quantity > 0")
    suspend fun decrementQuantity(bookId: String)

    @Query("DELETE FROM cart_items WHERE bookId = :bookId")
    suspend fun deleteItem(bookId: String)

    @Query("DELETE FROM cart_items")
    suspend fun clearCart()
}
