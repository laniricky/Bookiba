package co.booknook.core.data.repository

import co.booknook.core.datastore.BookibaPreferencesDataSource
import co.booknook.core.domain.model.Address
import co.booknook.core.domain.repository.AddressRepository
import co.booknook.core.network.api.BookibaApi
import co.booknook.core.network.model.NetworkCreateAddressRequest
import co.booknook.core.network.model.NetworkUpdateAddressRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NetworkAddressRepository @Inject constructor(
    private val api: BookibaApi,
    private val preferencesDataSource: BookibaPreferencesDataSource
) : AddressRepository {

    override fun getAddresses(): Flow<List<Address>> = flow {
        try {
            val token = preferencesDataSource.authToken.first()
            if (token != null) {
                val response = api.getAddresses("Bearer $token")
                emit(response.addresses.map { n ->
                    Address(
                        id = n.id,
                        label = n.label,
                        fullAddress = n.fullAddress,
                        isDefault = n.isDefault
                    )
                })
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    override suspend fun createAddress(label: String, fullAddress: String, isDefault: Boolean): Result<Unit> {
        return try {
            val token = preferencesDataSource.authToken.first()
                ?: return Result.failure(IllegalStateException("Not logged in"))
            api.createAddress(
                token = "Bearer $token",
                request = NetworkCreateAddressRequest(label = label, fullAddress = fullAddress, isDefault = isDefault)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateAddress(id: String, label: String?, fullAddress: String?, isDefault: Boolean?): Result<Unit> {
        return try {
            val token = preferencesDataSource.authToken.first()
                ?: return Result.failure(IllegalStateException("Not logged in"))
            api.updateAddress(
                addressId = id,
                token = "Bearer $token",
                request = NetworkUpdateAddressRequest(label = label, fullAddress = fullAddress, isDefault = isDefault)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteAddress(id: String): Result<Unit> {
        return try {
            val token = preferencesDataSource.authToken.first()
                ?: return Result.failure(IllegalStateException("Not logged in"))
            api.deleteAddress(addressId = id, token = "Bearer $token")
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
