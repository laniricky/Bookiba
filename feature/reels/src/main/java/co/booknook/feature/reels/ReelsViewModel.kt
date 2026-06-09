package co.booknook.feature.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.booknook.core.network.api.BookibaApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReelItem(
    val id: String,
    val videoUrl: String,
    val thumbnailUrl: String,
    val username: String,
    val userHandle: String,
    val description: String,
    val audioLabel: String = "Original audio",
    val likeCount: Int = 0,
    val commentCount: Int = 0,
    val shareCount: Int = 0,
    val linkedBookId: String? = null,
    val linkedBookTitle: String? = null,
    val isFollowing: Boolean = false,
    val isLiked: Boolean = false
)

data class ReelsUiState(
    val reels: List<ReelItem> = emptyList(),
    val currentPage: Int = 0,
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class ReelsViewModel @Inject constructor(
    private val api: BookibaApi
) : ViewModel() {

    private val _state = MutableStateFlow(ReelsUiState())
    val state: StateFlow<ReelsUiState> = _state.asStateFlow()

    init {
        loadReels()
    }

    private fun loadReels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val networkReels = api.getReels()
                val uiReels = networkReels.map { nr ->
                    ReelItem(
                        id = nr.id,
                        videoUrl = nr.videoUrl,
                        thumbnailUrl = nr.thumbnailUrl ?: "",
                        username = "Bookiba Admin",
                        userHandle = "@bookiba",
                        description = nr.title,
                        linkedBookId = nr.bookId,
                        linkedBookTitle = nr.bookTitle
                    )
                }
                _state.update { it.copy(isLoading = false, reels = uiReels) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.localizedMessage ?: "Failed to load reels") }
            }
        }
    }

    fun onPageChange(page: Int) {
        _state.update { it.copy(currentPage = page) }
    }

    fun onToggleLike(reelId: String) {
        _state.update { state ->
            state.copy(
                reels = state.reels.map { reel ->
                    if (reel.id == reelId) reel.copy(
                        isLiked = !reel.isLiked,
                        likeCount = if (reel.isLiked) reel.likeCount - 1 else reel.likeCount + 1
                    ) else reel
                }
            )
        }
    }

    fun onToggleFollow(reelId: String) {
        _state.update { state ->
            state.copy(
                reels = state.reels.map { reel ->
                    if (reel.id == reelId) reel.copy(isFollowing = !reel.isFollowing) else reel
                }
            )
        }
    }
}
