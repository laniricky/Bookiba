package co.booknook.feature.reels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
class ReelsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(ReelsUiState())
    val state: StateFlow<ReelsUiState> = _state.asStateFlow()

    init {
        loadReels()
    }

    private fun loadReels() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            // TODO: wire to actual API
            _state.update { it.copy(isLoading = false, reels = sampleReels()) }
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

    private fun sampleReels() = listOf(
        ReelItem(
            id = "1",
            videoUrl = "",
            thumbnailUrl = "",
            username = "booknook.co",
            userHandle = "@booknook",
            description = "Flipping through a 1984 vintage copy of Animal Farm. The annotations inside are absolutely beautiful ✨",
            audioLabel = "Original audio",
            likeCount = 1234,
            commentCount = 56,
            shareCount = 342,
            linkedBookTitle = "Animal Farm — 1984 Edition"
        )
    )
}
