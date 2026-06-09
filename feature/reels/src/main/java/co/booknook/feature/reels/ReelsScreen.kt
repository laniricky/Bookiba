package co.booknook.feature.reels

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
                ReelPage(
                    reel = state.reels[pageIndex],
                    isActive = pagerState.settledPage == pageIndex,
                    onLike = { viewModel.onToggleLike(state.reels[pageIndex].id) },
                    onFollow = { viewModel.onToggleFollow(state.reels[pageIndex].id) },
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
    onFollow: () -> Unit,
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
            // Placeholder gradient when no video URL yet
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
                .height(350.dp)
                .background(
                    Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.75f)))
                )
        )

        // ── Right action rail ─────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 14.dp, bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            ReelActionButton(
                icon = if (reel.isLiked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                label = formatCount(reel.likeCount),
                tint = if (reel.isLiked) Color.Red else Color.White,
                onClick = onLike
            )
            ReelActionButton(
                icon = Icons.Outlined.Email,
                label = formatCount(reel.commentCount),
                tint = Color.White,
                onClick = {}
            )
            ReelActionButton(
                icon = Icons.Outlined.Send,
                label = formatCount(reel.shareCount),
                tint = Color.White,
                onClick = {}
            )
            ReelActionButton(
                icon = Icons.Outlined.MoreVert,
                label = "",
                tint = Color.White,
                onClick = {}
            )
        }

        // ── Bottom info ───────────────────────────────────────────
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 100.dp, end = 70.dp)
        ) {
            // Username + Follow
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(WarmBrown)
                )
                Text(reel.username, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!reel.isFollowing) {
                    TextButton(onClick = onFollow) {
                        Text("Follow", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Description
            Text(
                text = reel.description,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(8.dp))

            // Audio label
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(14.dp))
                Text(reel.audioLabel, color = Color.White, fontSize = 12.sp)
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
                        Text(title, color = Cream, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
private fun ReelActionButton(icon: ImageVector, label: String, tint: Color, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(onClick = onClick) {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(30.dp))
        }
        if (label.isNotEmpty()) {
            Text(label, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
        }
    }
}

private fun formatCount(count: Int): String = when {
    count >= 1_000_000 -> "${"%.1f".format(count / 1_000_000.0)}M"
    count >= 1_000 -> "${"%.1f".format(count / 1_000.0)}K"
    else -> count.toString()
}
