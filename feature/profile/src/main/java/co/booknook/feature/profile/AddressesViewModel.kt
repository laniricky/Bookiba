package co.booknook.feature.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.domain.model.Address
import co.booknook.core.domain.repository.AddressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddressesUiState(
    val addresses: List<Address> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class AddressesViewModel @Inject constructor(
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _state = MutableStateFlow(AddressesUiState(isLoading = true))
    val state: StateFlow<AddressesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            addressRepository.getAddresses().collect { list ->
                _state.update { it.copy(addresses = list, isLoading = false) }
            }
        }
    }

    fun addAddress(label: String, fullAddress: String, isDefault: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = addressRepository.createAddress(label, fullAddress, isDefault)
            if (result.isFailure) {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            } else {
                // Flow will auto-update if we refresh, but since our repository flow doesn't emit on change currently,
                // we should re-fetch. But wait, getAddresses() returns a single flow of network call in this simple setup.
                // Actually, let's just trigger a refresh.
                refresh()
            }
        }
    }

    fun deleteAddress(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = addressRepository.deleteAddress(id)
            if (result.isFailure) {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            } else {
                refresh()
            }
        }
    }

    fun setAsDefault(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val result = addressRepository.updateAddress(id, null, null, true)
            if (result.isFailure) {
                _state.update { it.copy(isLoading = false, error = result.exceptionOrNull()?.message) }
            } else {
                refresh()
            }
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            addressRepository.getAddresses().collect { list ->
                _state.update { it.copy(addresses = list, isLoading = false) }
            }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
