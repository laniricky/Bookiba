package co.booknook.core.domain.repository

import co.booknook.core.domain.model.Address
import kotlinx.coroutines.flow.Flow

interface AddressRepository {
    fun getAddresses(): Flow<List<Address>>
    suspend fun createAddress(label: String, fullAddress: String, isDefault: Boolean): Result<Unit>
    suspend fun updateAddress(id: String, label: String?, fullAddress: String?, isDefault: Boolean?): Result<Unit>
    suspend fun deleteAddress(id: String): Result<Unit>
}
