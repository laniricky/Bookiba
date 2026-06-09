package co.booknook.feature.reels

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.runtime.collectAsState
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Cream = Color(0xFFF5F0E8)
private val WarmBrown = Color(0xFF8B7355)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ReelsScreen(
    onBookClick: (String) -> Unit,
    viewModel: ReelsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val pagerState = rememberPagerState(pageCount = { state.reels.size })

    // Sync pager page → viewModel
    LaunchedEffect(pagerState.settledPage) {
        viewModel.onPageChange(pagerState.settledPage)
    }

    if (state.error != null) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text(state.error ?: "Error", color = Cream, fontSize = 16.sp, modifier = Modifier.padding(16.dp))
        }
        return
    }

    if (state.reels.isEmpty() && !state.isLoading) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            Text("No reels yet", color = Cream, fontSize = 16.sp)
        }
        return
    }

    var isRefreshing by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoading) {
        if (!state.isLoading) {
            isRefreshing = false
        }
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refresh()
        },
        modifier = Modifier.fillMaxSize()
    ) {
        VerticalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            if (pageIndex < state.reels.size) {
                // Pause immediately when user starts swiping between pages
                val isActive = pagerState.settledPage == pageIndex && !pagerState.isScrollInProgress
                ReelPage(
                    reel = state.reels[pageIndex],
                    isActive = isActive,
                    onLike = { viewModel.onToggleLike(state.reels[pageIndex].id) },
                    onBookClick = onBookClick
                )
            }
        }
    }
}

@Composable
private fun ReelPage(
    reel: ReelItem,
    isActive: Boolean,
    onLike: () -> Unit,
    onBookClick: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // ── Video Player ────────────────────────────────────────────
        if (reel.videoUrl.isNotBlank()) {
            VideoPlayer(videoUrl = reel.videoUrl, isActive = isActive)
        } else {
            Box(
                modifier = Modifier.fillMaxSize().background(
                    Brush.verticalGradient(listOf(Color(0xFF1A1512), Color(0xFF2A1F16), Color(0xFF1A1512)))
                )
            )
        }

        // ── Bottom gradient scrim ─────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(320.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f)))
                )
        )

        // ── Right action rail (glassmorphism buttons) ─────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Wishlist / Like
            ReelActionButton(
                icon = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(reel.likeCount),
                tint = if (reel.isLiked) Color(0xFFFF4D6D) else Color.White,
                onClick = onLike
            )
            // Reviews / Ratings
            ReelActionButton(
                icon = Icons.Outlined.ChatBubbleOutline,
                label = formatCount(reel.commentCount),
                tint = Color.White,
                onClick = {}
            )
            // Save to shelf
            ReelActionButton(
                icon = Icons.Outlined.BookmarkBorder,
                label = "Save",
                tint = Color.White,
                onClick = {}
            )
            // Share
            ReelActionButton(
                icon = Icons.Outlined.Share,
                label = "Share",
                tint = Color.White,
                onClick = {}
            )
        }

        // ── Bottom info ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 28.dp, end = 82.dp)
        ) {
            // Book reel title / description
            Text(
                text = reel.description,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 20.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Audio label with music note icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.MusicNote,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.75f),
                    modifier = Modifier.size(13.dp)
                )
                Text(reel.audioLabel, color = Color.White.copy(alpha = 0.75f), fontSize = 12.sp)
            }

            // Linked book chip
            reel.linkedBookTitle?.let { title ->
                Spacer(Modifier.height(10.dp))
                Surface(
                    modifier = Modifier.clickable { reel.linkedBookId?.let(onBookClick) },
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White.copy(alpha = 0.18f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.ShoppingCart, contentDescription = null, tint = Cream, modifier = Modifier.size(14.dp))
                        Text(
                            title,
                            color = Cream,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(videoUrl: String, isActive: Boolean) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                useController = false
                player = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = androidx.media3.common.MediaItem.fromUri(videoUrl)
                    setMediaItem(mediaItem)
                    repeatMode = ExoPlayer.REPEAT_MODE_ONE
                    prepare()
                }
            }
        },
        update = { playerView ->
            if (isActive) playerView.player?.play() else playerView.player?.pause()
        },
        onRelease = { it.player?.release() },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
private fun ReelActionButton(
    icon: ImageVector,
    label: String,
    tint: Color = Color.White,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.18f))
                .border(1.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
    count >= 1_000 -> "${"%.1f".format(count / 1_000.0)}K"
    else -> count.toString()
}
