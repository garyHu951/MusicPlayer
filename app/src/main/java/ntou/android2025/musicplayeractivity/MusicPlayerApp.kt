package ntou.android2025.musicplayeractivity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.util.Locale

@Composable
fun MusicPlayerApp(
    context: Context,
    playerProvider: () -> MediaPlayer?,
    onPlayerUpdate: (MediaPlayer?) -> Unit
) {
    val songs = remember {
        listOf(
            getSongInfo(context, R.raw.first_song),
            getSongInfo(context, R.raw.second_song),
            getSongInfo(context, R.raw.third_song),
            getSongInfo(context, R.raw.fourth_song),
            getSongInfo(context, R.raw.fifth_song),
            getSongInfo(context, R.raw.sixth_song),
            getSongInfo(context, R.raw.seventh_song),
            getSongInfo(context, R.raw.eighth_song),
            getSongInfo(context, R.raw.ninth_song),
            getSongInfo(context, R.raw.tenth_song),
            getSongInfo(context, R.raw.eleventh_song),
            getSongInfo(context, R.raw.twelfth_song),
            getSongInfo(context, R.raw.thirteenth_song),
            getSongInfo(context, R.raw.fourteenth_song),
            getSongInfo(context, R.raw.fifteenth_song),
            getSongInfo(context, R.raw.sixteenth_song),
            getSongInfo(context, R.raw.seventeenth_song),
            getSongInfo(context, R.raw.eighteenth_song),
            getSongInfo(context, R.raw.nineteenth_song),
            getSongInfo(context, R.raw.twentieth_song),
            getSongInfo(context, R.raw.twenty_first_song),
            getSongInfo(context, R.raw.twenty_second_song)
        )
    }

    var isPlaying by remember { mutableStateOf(false) }
    var currentSong by remember { mutableStateOf<Song?>(null) }
    var showPlayerView by remember { mutableStateOf(false) }
    var isShuffleMode by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableIntStateOf(0) }
    var duration by remember { mutableIntStateOf(0) }
    var isTransitioning by remember { mutableStateOf(false) }
    var songToPlay by remember { mutableStateOf<Song?>(null) }
    var isDragging by remember { mutableStateOf(false) }
    var sliderPosition by remember { mutableFloatStateOf(0f) }

    // 專輯圖片縮放動畫
    val albumScale by animateFloatAsState(
        targetValue = if (showPlayerView && !isTransitioning) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = EaseInOutCubic),
        label = "albumScale"
    )

    // 列表偏移動畫 - 調整為更合適的偏移量
    val listOffset by animateDpAsState(
        targetValue = if (showPlayerView) 0.dp else 40.dp, // 進一步減少偏移量讓列表更靠上
        animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic),
        label = "listOffset"
    )

    // 格式化時間的輔助函數
    fun formatTime(timeInMs: Int): String {
        val minutes = timeInMs / 1000 / 60
        val seconds = (timeInMs / 1000) % 60
        return String.format(Locale.getDefault(), "%d:%02d", minutes, seconds)
    }

    // 更新播放進度
    LaunchedEffect(isPlaying, currentSong) {
        while (isPlaying && currentSong != null && !isDragging) {
            playerProvider()?.let { player ->
                try {
                    if (player.isPlaying) {
                        currentPosition = player.currentPosition
                        if (duration == 0) {
                            duration = player.duration
                        }
                        // 當不在拖動狀態時，更新滑桿位置
                        if (!isDragging && duration > 0) {
                            sliderPosition = currentPosition.toFloat() / duration.toFloat()
                        }
                    }
                } catch (_: Exception) {
                    // 處理可能的異常，防止崩潰
                    isPlaying = false
                }
            }
            delay(1000)
        }
    }

    // 處理歌曲切換邏輯
    LaunchedEffect(songToPlay) {
        songToPlay?.let { song ->
            if (currentSong != null) {
                // 如果有當前歌曲，先開始縮小動畫
                isTransitioning = true
                delay(300) // 等待縮小動畫完成一半
            }

            // 停掉原本的 player
            playerProvider()?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                    release()
                } catch (_: Exception) {
                    // 處理可能的異常
                }
            }

            // 建立新的 MediaPlayer
            try {
                val newPlayer = MediaPlayer.create(context, song.resId)
                newPlayer?.let { player ->
                    player.setOnCompletionListener {
                        if (isShuffleMode) {
                            // 隨機播放下一首
                            val randomSong = songs.filter { it != song }.random()
                            songToPlay = randomSong
                        } else {
                            isPlaying = false
                            showPlayerView = false
                            currentSong = null
                            currentPosition = 0
                            duration = 0
                            sliderPosition = 0f
                        }
                    }
                    player.start()
                    onPlayerUpdate(player)
                    isPlaying = true
                    currentSong = song
                    duration = player.duration
                    currentPosition = 0
                    sliderPosition = 0f
                    showPlayerView = true
                }
            } catch (_: Exception) {
                // 處理 MediaPlayer 創建失敗的情況
                isPlaying = false
                showPlayerView = false
                currentSong = null
                sliderPosition = 0f
            }

            // 重置過渡狀態，開始放大動畫
            isTransitioning = false
            songToPlay = null // 重置觸發器
        }
    }

    // 播放指定歌曲的函數
    fun playSong(song: Song) {
        songToPlay = song
    }

    // 安全地停止播放並恢復初始狀態的函數
    fun stopPlayingAndReset() {
        try {
            // 先停止播放
            playerProvider()?.apply {
                if (isPlaying) {
                    pause()
                }
                // 不要立即釋放，讓它自然結束
            }

            // 重置所有狀態到初始值
            isPlaying = false
            showPlayerView = false
            currentSong = null
            currentPosition = 0
            duration = 0
            isShuffleMode = false
            isTransitioning = false
            songToPlay = null
            sliderPosition = 0f

            // 最後才清空 player 引用
            onPlayerUpdate(null)
        } catch (_: Exception) {
            // 即使發生異常也要重置狀態
            isPlaying = false
            showPlayerView = false
            currentSong = null
            currentPosition = 0
            duration = 0
            isShuffleMode = false
            sliderPosition = 0f
            onPlayerUpdate(null)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp)
    ) {
        // 播放器視圖（當有歌曲在播放時顯示）
        AnimatedVisibility(
            visible = showPlayerView && currentSong != null,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic)
            ) + fadeIn(animationSpec = tween(durationMillis = 600)),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(durationMillis = 600, easing = EaseInOutCubic)
            ) + fadeOut(animationSpec = tween(durationMillis = 600))
        ) {
            Column {
                // 專輯封面區域 - 增加上方間距，讓整個區域下移
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp), // 從 20.dp 增加到 40.dp
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // 專輯封面 - 帶縮放動畫
                    currentSong?.let { song ->
                        Image(
                            bitmap = song.image.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier
                                .size(200.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .graphicsLayer {
                                    scaleX = albumScale
                                    scaleY = albumScale
                                    alpha = albumScale
                                },
                            contentScale = ContentScale.Crop
                        )
                    }

                    // 只有當圖片完全顯示時才顯示文字信息
                    AnimatedVisibility(
                        visible = albumScale > 0.8f,
                        enter = fadeIn(animationSpec = tween(durationMillis = 300)),
                        exit = fadeOut(animationSpec = tween(durationMillis = 300))
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(32.dp)) // 從 24.dp 增加到 32.dp

                            // 歌曲標題
                            Text(
                                text = "New Memories",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )

                            // 歌手名稱
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp) // 從 4.dp 增加到 8.dp
                            ) {
                                Text(
                                    text = "▶",
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = currentSong!!.artist,
                                    color = Color.Gray,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }

                            // 播放時間 - 顯示真實的歌曲時長
                            Text(
                                text = if (duration > 0) formatTime(duration) else "載入中...",
                                color = Color.Gray,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(top = 8.dp) // 從 4.dp 增加到 8.dp
                            )
                        }
                    }
                }

                // 控制按鈕行 - 在專輯信息下方，增加間距
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp), // 從 16.dp 增加到 24.dp
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // 隨機播放按鈕
                    Button(
                        onClick = { isShuffleMode = !isShuffleMode },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isShuffleMode) Color.Green else Color.Gray
                        )
                    ) {
                        Text(
                            text = "隨機",
                            color = Color.White,
                            fontSize = 12.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // 播放/暫停按鈕
                    Button(
                        onClick = {
                            try {
                                playerProvider()?.apply {
                                    if (isPlaying) {
                                        pause()
                                    } else {
                                        start()
                                    }
                                    isPlaying = !isPlaying
                                }
                            } catch (_: Exception) {
                                // 如果操作失敗，重置狀態
                                stopPlayingAndReset()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Green
                        ),
                        modifier = Modifier.size(48.dp),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            text = if (isPlaying) "⏸" else "▶",
                            color = Color.Black,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        // 歌曲列表 - 調整偏移動畫，讓列表位置更合適
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .offset(y = listOffset),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(
                bottom = if (showPlayerView && currentSong != null) 160.dp else 80.dp
            ) // 添加底部內容填充，當播放器顯示時給更多空間
        ) {
            items(songs) { song ->
                SongItem(
                    song = song,
                    isCurrentPlaying = currentSong == song && isPlaying,
                    onClick = { playSong(song) }
                )
            }
        }

        // 底部進度條（當有歌曲播放時顯示）- 改用 Slider 支援拖動
        if (showPlayerView && currentSong != null && duration > 0) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp)
            ) {
                // 時間顯示行
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = formatTime(currentPosition),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = formatTime(duration),
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }

                // 可拖動的播放進度滑桿
                Slider(
                    value = sliderPosition,
                    onValueChange = { newValue ->
                        isDragging = true
                        sliderPosition = newValue
                        // 實時更新顯示的時間位置
                        currentPosition = (newValue * duration).toInt()
                    },
                    onValueChangeFinished = {
                        // 拖動結束後，真正跳轉到指定位置
                        try {
                            val newPosition = (sliderPosition * duration).toInt()
                            playerProvider()?.seekTo(newPosition)
                            currentPosition = newPosition
                        } catch (_: Exception) {
                            // 處理 seek 操作失敗的情況
                        } finally {
                            isDragging = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = Color.Green,
                        activeTrackColor = Color.Green,
                        inactiveTrackColor = Color.Gray
                    )
                )

                // 當前播放歌曲信息（迷你版）
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = currentSong!!.image.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp)
                    ) {
                        Text(
                            text = currentSong!!.title,
                            color = Color.White,
                            fontSize = 14.sp,
                            maxLines = 1
                        )
                        Text(
                            text = currentSong!!.artist,
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SongItem(
    song: Song,
    isCurrentPlaying: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            bitmap = song.image.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = if (isCurrentPlaying) Color.Green else Color.White,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Normal
            )
            Text(
                text = song.artist,
                color = Color.Gray,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

data class Song(val title: String, val artist: String, val image: Bitmap, val resId: Int)

fun getSongInfo(context: Context, resId: Int): Song {
    val retriever = MediaMetadataRetriever()
    val fd = context.resources.openRawResourceFd(resId)
    retriever.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)

    val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: "Unknown Title"
    val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "Unknown Artist"

    val artData = retriever.embeddedPicture
    val image = if (artData != null) {
        BitmapFactory.decodeByteArray(artData, 0, artData.size)
    } else {
        BitmapFactory.decodeResource(context.resources, android.R.drawable.ic_media_play)
    }

    retriever.release()
    return Song(title, artist, image, resId)
}