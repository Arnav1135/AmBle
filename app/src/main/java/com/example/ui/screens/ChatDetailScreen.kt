package com.example.ui.screens

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.data.MessageEntity
import com.example.data.UserEntity
import com.example.data.ChatEntity
import com.example.data.ContactEntity
import com.example.ui.theme.*
import com.example.ui.components.*
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import kotlin.math.roundToInt
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatDetailScreen(viewModel: ChatViewModel) {
    val activeChat by viewModel.activeChat.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val activeParticipants by viewModel.activeParticipants.collectAsState()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsState()
    val isVoiceLocked by viewModel.isVoiceLocked.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val replyToMessage by viewModel.replyToMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val chats by viewModel.chats.collectAsState()
    val contacts by viewModel.contacts.collectAsState()
    val users by viewModel.users.collectAsState()

    var showCallLockedDialog by remember { mutableStateOf(false) }
    var showAddContactDialog by remember { mutableStateOf(false) }

    var messageInput by remember { mutableStateOf("") }
    var showAttachmentMenu by remember { mutableStateOf(false) }
    var showDocumentPicker by remember { mutableStateOf(false) }
    var showGalleryPicker by remember { mutableStateOf(false) }
    var showCameraPicker by remember { mutableStateOf(false) }
    var showAudioPicker by remember { mutableStateOf(false) }
    var showLocationPicker by remember { mutableStateOf(false) }
    var showContactPicker by remember { mutableStateOf(false) }

    var showEmojiKeyboard by remember { mutableStateOf(false) }
    var editingMessage by remember { mutableStateOf<MessageEntity?>(null) }
    var editInput by remember { mutableStateOf("") }

    var showOptionsDropdown by remember { mutableStateOf(false) }
    var showDisappearingDialog by remember { mutableStateOf(false) }
    var showForwardDialog by remember { mutableStateOf<MessageEntity?>(null) }
    var showRetryDialog by remember { mutableStateOf<MessageEntity?>(null) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportResultInfo by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showScheduleDialog by remember { mutableStateOf(false) }
    var lightboxImageUrl by remember { mutableStateOf<String?>(null) }
    var expandedThreadIds by remember { mutableStateOf(setOf<String>()) }

    val repliesByParent = remember(activeMessages) {
        activeMessages.filter { !it.replyToMessageId.isNullOrEmpty() }.groupBy { it.replyToMessageId!! }
    }

    val topLevelMessages = remember(activeMessages) {
        activeMessages.filter { msg ->
            msg.replyToMessageId.isNullOrEmpty() || activeMessages.none { parent -> parent.messageId == msg.replyToMessageId }
        }
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Determine the user we are chatting with in 1-on-1 chats
    val otherUser = remember(activeParticipants, currentUser) {
        activeParticipants.firstOrNull { it.uid != currentUser?.uid }
    }

    val isGroupChat = activeChat?.isGroup == true
    val typingGroupUser = if (isGroupChat) {
        users.firstOrNull { it.uid != currentUser?.uid && it.isTyping }
    } else null
    val isPartnerTyping = if (isGroupChat) {
        typingGroupUser != null
    } else {
        otherUser?.isTyping == true
    }

    // Check if otherUser is saved in our contacts list
    val isOtherSavedInMyContacts = remember(contacts, otherUser) {
        otherUser != null && contacts.any { it.contactPhoneNumber == otherUser.phoneNumber }
    }

    // Also check if they have saved our number.
    var hasOtherSavedMyContact by remember { mutableStateOf(false) }

    val activeChatId by viewModel.activeChatId.collectAsState()

    LaunchedEffect(activeChatId) {
        if (activeChatId != null) {
            messageInput = viewModel.getDraft(activeChatId!!)
        }
    }

    LaunchedEffect(otherUser, contacts) {
        if (otherUser != null) {
            hasOtherSavedMyContact = viewModel.hasOtherUserAddedMe(otherUser.uid, otherUser.phoneNumber)
        }
    }

    val isMutualContact = remember(isOtherSavedInMyContacts, hasOtherSavedMyContact) {
        isOtherSavedInMyContacts && hasOtherSavedMyContact
    }

    if (showCallLockedDialog) {
        AlertDialog(
            onDismissRequest = { showCallLockedDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Connection Locked", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
                }
            },
            text = {
                Text(
                    text = "To voice call or video call, both you and ${otherUser?.name ?: "this contact"} must have each other's phone numbers saved in your address books. This ensures strict privacy and spam protection.",
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showCallLockedDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
                ) {
                    Text("Okay")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    if (showAddContactDialog && otherUser != null) {
        var contactNameInput by remember { mutableStateOf(otherUser.name) }
        var contactPhoneInput by remember { mutableStateOf(otherUser.phoneNumber) }
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            title = {
                Text("Save Contact", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = contactNameInput,
                        onValueChange = { contactNameInput = it },
                        label = { Text("Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactPhoneInput,
                        onValueChange = { contactPhoneInput = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = false
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.addContact(contactNameInput.trim(), contactPhoneInput.trim())
                        showAddContactDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E))
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Scroll to bottom on new message
    LaunchedEffect(activeMessages.size) {
        if (activeMessages.isNotEmpty()) {
            lazyListState.animateScrollToItem(activeMessages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            showOptionsDropdown = true
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            AsyncImage(
                                model = activeChat?.groupPhoto ?: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = activeChat?.groupName ?: "Chat Details",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (activeChat?.disappearingDuration != null && activeChat!!.disappearingDuration > 0) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.Timelapse,
                                        contentDescription = "Disappearing Messages Active",
                                        tint = ChatWaveGreen,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                            
                            // Real-time online / typing indicator status
                            val isGroupChat = activeChat?.isGroup == true
                            val typingGroupUser = if (isGroupChat) {
                                users.firstOrNull { it.uid != currentUser?.uid && it.isTyping }
                            } else null

                            val isTyping = if (isGroupChat) {
                                typingGroupUser != null
                            } else {
                                otherUser?.isTyping == true
                            }
                            val isOnline = otherUser?.isOnline == true

                            if (isTyping) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AnimatedTypingDots(color = ChatWaveGreen, dotSize = 5.dp)
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (typingGroupUser != null) "${typingGroupUser.name} is typing…" else "typing…",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = ChatWaveGreen
                                    )
                                }
                            } else {
                                val statusText = when {
                                    isGroupChat -> "group chat"
                                    isOnline -> "online"
                                    else -> "offline"
                                }
                                Text(
                                    text = statusText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = if (isOnline) ChatWaveGreen else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isLocked = false
                    IconButton(onClick = { viewModel.navigateTo("gemini_live_voice") }) {
                        Icon(
                            Icons.Default.GraphicEq,
                            contentDescription = "Gemini Live Voice Conversation",
                            tint = Color(0xFF2563EB)
                        )
                    }
                    IconButton(onClick = {
                        if (isLocked) {
                            showCallLockedDialog = true
                        } else {
                            viewModel.startCallFromActiveChat("voice")
                        }
                    }) {
                        Icon(
                            Icons.Default.Call,
                            contentDescription = "Voice Call",
                            tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(onClick = {
                        if (isLocked) {
                            showCallLockedDialog = true
                        } else {
                            viewModel.startCallFromActiveChat("video")
                        }
                    }) {
                        Icon(
                            Icons.Default.Videocam,
                            contentDescription = "Video Call",
                            tint = if (isLocked) Color.Gray else MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = { showExportDialog = true },
                        modifier = Modifier.testTag("export_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Download,
                            contentDescription = "Export Chat",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { showOptionsDropdown = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "Options")
                    }

                    // Chat options dropdown menu
                    DropdownMenu(
                        expanded = showOptionsDropdown,
                        onDismissRequest = { showOptionsDropdown = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Export Chat History") },
                            onClick = {
                                showExportDialog = true
                                showOptionsDropdown = false
                            },
                            leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981)) }
                        )

                        DropdownMenuItem(
                            text = { Text("Gemini Live Voice Session") },
                            onClick = {
                                viewModel.navigateTo("gemini_live_voice")
                                showOptionsDropdown = false
                            },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = Color(0xFF2563EB)) }
                        )

                        DropdownMenuItem(
                            text = { Text("Disappearing Messages") },
                            onClick = {
                                showDisappearingDialog = true
                                showOptionsDropdown = false
                            },
                            leadingIcon = { Icon(Icons.Default.Timelapse, contentDescription = null) }
                        )

                        val isMuted = currentUser?.mutedChatIds?.contains(activeChat?.chatId ?: "") == true
                        DropdownMenuItem(
                            text = { Text(if (isMuted) "Unmute Notifications" else "Mute Notifications") },
                            onClick = {
                                activeChat?.let { viewModel.toggleMuteChat(it.chatId) }
                                showOptionsDropdown = false
                            },
                            leadingIcon = { Icon(if (isMuted) Icons.Default.VolumeUp else Icons.Default.VolumeOff, contentDescription = null) }
                        )

                        DropdownMenuItem(
                            text = { Text("Clear Chat") },
                            onClick = {
                                // Simple mock clear
                                showOptionsDropdown = false
                            },
                            leadingIcon = { Icon(Icons.Default.ClearAll, contentDescription = null) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFEAF2FB),
                            Color(0xFFD6E8FA)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 840.dp)
                    .align(Alignment.Center)
            ) {
                // Messages List Section
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(topLevelMessages, key = { it.messageId }) { parentMessage ->
                        val isMe = parentMessage.senderId == currentUser?.uid
                        val directReplies = repliesByParent[parentMessage.messageId] ?: emptyList()
                        val isThreadExpanded = expandedThreadIds.contains(parentMessage.messageId)

                        Column(modifier = Modifier.fillMaxWidth()) {
                            // Slide-to-reply Root Message Bubble
                            SwipeToReplyWrapper(
                                onReply = { viewModel.setReplyToMessage(parentMessage) }
                            ) {
                                MessageBubble(
                                    message = parentMessage,
                                    isMe = isMe,
                                    viewModel = viewModel,
                                    onReplyClick = { viewModel.setReplyToMessage(parentMessage) },
                                    onDeleteMe = { viewModel.deleteMessage(parentMessage, forEveryone = false) },
                                    onDeleteEveryone = { viewModel.deleteMessage(parentMessage, forEveryone = true) },
                                    onEditClick = {
                                        editingMessage = parentMessage
                                        editInput = parentMessage.text
                                    },
                                    onForwardClick = { showForwardDialog = parentMessage },
                                    onFailedClick = { showRetryDialog = parentMessage },
                                    onImageClick = { lightboxImageUrl = it }
                                )
                            }

                            // Threaded Replies Collapsible Sub-Group
                            if (directReplies.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(start = if (isMe) 0.dp else 16.dp, end = if (isMe) 16.dp else 0.dp),
                                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                                ) {
                                    Surface(
                                        modifier = Modifier
                                            .clickable {
                                                expandedThreadIds = if (isThreadExpanded) {
                                                    expandedThreadIds - parentMessage.messageId
                                                } else {
                                                    expandedThreadIds + parentMessage.messageId
                                                }
                                            }
                                            .testTag("thread_toggle_${parentMessage.messageId}"),
                                        shape = RoundedCornerShape(16.dp),
                                        color = Color(0xFF2563EB).copy(alpha = 0.12f),
                                        border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.3f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Forum,
                                                contentDescription = "Thread Replies",
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (isThreadExpanded) "Hide Thread (${directReplies.size} ${if (directReplies.size == 1) "reply" else "replies"})"
                                                       else "🧵 View Thread (${directReplies.size} ${if (directReplies.size == 1) "reply" else "replies"})",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF1E40AF)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = if (isThreadExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                                contentDescription = null,
                                                tint = Color(0xFF2563EB),
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }

                                AnimatedVisibility(
                                    visible = isThreadExpanded,
                                    enter = fadeIn() + expandVertically(),
                                    exit = fadeOut() + shrinkVertically()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 16.dp, top = 6.dp, bottom = 4.dp)
                                            .drawBehind {
                                                drawLine(
                                                    color = Color(0xFF2563EB).copy(alpha = 0.5f),
                                                    start = androidx.compose.ui.geometry.Offset(x = 8.dp.toPx(), y = 0f),
                                                    end = androidx.compose.ui.geometry.Offset(x = 8.dp.toPx(), y = size.height),
                                                    strokeWidth = 2.5.dp.toPx()
                                                )
                                            }
                                            .padding(start = 16.dp)
                                    ) {
                                        directReplies.forEach { replyMsg ->
                                            val replyIsMe = replyMsg.senderId == currentUser?.uid
                                            Spacer(modifier = Modifier.height(4.dp))
                                            SwipeToReplyWrapper(
                                                onReply = { viewModel.setReplyToMessage(replyMsg) }
                                            ) {
                                                MessageBubble(
                                                    message = replyMsg,
                                                    isMe = replyIsMe,
                                                    viewModel = viewModel,
                                                    onReplyClick = { viewModel.setReplyToMessage(replyMsg) },
                                                    onDeleteMe = { viewModel.deleteMessage(replyMsg, forEveryone = false) },
                                                    onDeleteEveryone = { viewModel.deleteMessage(replyMsg, forEveryone = true) },
                                                    onEditClick = {
                                                        editingMessage = replyMsg
                                                        editInput = replyMsg.text
                                                    },
                                                    onForwardClick = { showForwardDialog = replyMsg },
                                                    onFailedClick = { showRetryDialog = replyMsg },
                                                    onImageClick = { lightboxImageUrl = it }
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                                            horizontalArrangement = Arrangement.Start
                                        ) {
                                            TextButton(
                                                onClick = { viewModel.setReplyToMessage(parentMessage) },
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Icon(Icons.Default.Reply, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color(0xFF2563EB))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text("Reply to this thread", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2563EB))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Live Real-Time Typing Bubble
                AnimatedVisibility(
                    visible = isPartnerTyping,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        TypingIndicatorBubble(
                            userName = typingGroupUser?.name,
                            dotColor = ChatWaveGreen
                        )
                    }
                }

                // Reply preview bar
                if (replyToMessage != null) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Reply, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Reply to message", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Text(replyToMessage!!.text, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { viewModel.setReplyToMessage(null) }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }

                val isLocked = false
                if (isLocked) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .navigationBarsPadding(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = "Lock icon",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Chat Locked • Privacy Shield",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = Color(0xFF1B2A5E)
                                )
                            }

                            Text(
                                text = if (!isOtherSavedInMyContacts) {
                                    "You haven't saved ${otherUser?.name ?: "this user"}'s phone number (${otherUser?.phoneNumber ?: ""}) in your contacts list yet. Save their number to start chatting and calling."
                                } else {
                                    "${otherUser?.name ?: "This contact"} doesn't have your number (${currentUser?.phoneNumber ?: ""}) saved in their address book yet. Both users must have each other's numbers saved to connect."
                                },
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                            ) {
                                if (!isOtherSavedInMyContacts) {
                                    Button(
                                        onClick = { showAddContactDialog = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B2A5E)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Save Contact", fontSize = 13.sp)
                                    }
                                } else {
                                    Button(
                                        onClick = {
                                            val current = currentUser
                                            if (otherUser != null && current != null) {
                                                viewModel.simulateOtherUserAddingMe(
                                                    otherUser.uid,
                                                    otherUser.name,
                                                    current.phoneNumber,
                                                    true
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Icon(Icons.Default.VerifiedUser, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Simulate ${otherUser?.name ?: "Contact"} Saving Me", fontSize = 13.sp)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Bottom Input Control Panel
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                            .navigationBarsPadding(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Input Box (Text area, Emoji toggle, Attach toggle)
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 6.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            ) {
                                IconButton(onClick = { showEmojiKeyboard = !showEmojiKeyboard }) {
                                    Icon(
                                        imageVector = if (showEmojiKeyboard) Icons.Default.Keyboard else Icons.Default.SentimentSatisfied,
                                        contentDescription = "Emojis",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Dynamic Switch for voice recording display
                                if (isRecordingVoice) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        IconButton(onClick = { viewModel.cancelVoiceRecording() }) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Cancel recording",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }

                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.error)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))

                                        Text(
                                            text = formatDuration(recordingDuration),
                                            color = MaterialTheme.colorScheme.error,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )

                                        Spacer(modifier = Modifier.width(8.dp))

                                        // Real-time amplitude bars
                                        Row(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(24.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            val amps = viewModel.voiceAmplitudes.collectAsState().value
                                            amps.forEach { amp ->
                                                val barHeight = (amp * 22).dp.coerceAtLeast(3.dp)
                                                Box(
                                                    modifier = Modifier
                                                        .width(3.dp)
                                                        .height(barHeight)
                                                        .clip(RoundedCornerShape(2.dp))
                                                        .background(ChatWaveGreen)
                                                )
                                            }
                                        }

                                        if (isVoiceLocked) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(start = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Lock,
                                                    contentDescription = "Locked",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Text("Locked", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                            }
                                        } else {
                                            Text(
                                                text = "‹ Slide",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                } else {
                                    OutlinedTextField(
                                        value = messageInput,
                                        onValueChange = {
                                            messageInput = it
                                            if (it.isNotBlank()) {
                                                viewModel.userStartedTyping()
                                            } else {
                                                viewModel.userStoppedTyping()
                                            }
                                            activeChatId?.let { cid -> viewModel.saveDraft(cid, it) }
                                        },
                                        placeholder = { Text("Type a message...") },
                                        modifier = Modifier
                                            .weight(1f)
                                            .testTag("message_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        maxLines = 4
                                    )

                                    IconButton(onClick = { showAttachmentMenu = true }) {
                                        Icon(
                                            imageVector = Icons.Default.AttachFile,
                                            contentDescription = "Attachments",
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }

                        // Floating Round Button on right (Mic / Send / Schedule on Long Press)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(ChatWaveGreen)
                                .combinedClickable(
                                    onClick = {
                                        if (isRecordingVoice) {
                                            viewModel.stopVoiceRecording()
                                        } else if (messageInput.trim().isNotEmpty()) {
                                            viewModel.sendTextMessage(messageInput)
                                            messageInput = ""
                                            showEmojiKeyboard = false
                                        } else {
                                            viewModel.startVoiceRecording()
                                        }
                                    },
                                    onLongClick = {
                                        showScheduleDialog = true
                                    }
                                )
                                .testTag("send_fab"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isRecordingVoice) {
                                    Icons.AutoMirrored.Filled.Send
                                } else if (messageInput.trim().isNotEmpty()) {
                                    Icons.AutoMirrored.Filled.Send
                                } else {
                                    Icons.Default.Mic
                                },
                                contentDescription = "Send Action",
                                tint = Color.White
                            )
                        }
                    }
                }

                // Inline Emojis & Stickers WhatsApp-style keyboard tray
                if (showEmojiKeyboard) {
                    WhatsAppEmojiStickerPickerPanel(
                        onEmojiSelect = { messageInput += it },
                        onBackspace = {
                            if (messageInput.isNotEmpty()) {
                                messageInput = messageInput.dropLast(1)
                            }
                        },
                        onStickerSelect = { stickerUrl, stickerName ->
                            viewModel.sendStickerMessage(stickerUrl, stickerName)
                        },
                        onGifSelect = { gifUrl, title ->
                            viewModel.sendAttachment("image", gifUrl)
                        },
                        onClose = { showEmojiKeyboard = false }
                    )
                }
            }

            // Media attachment menu
            if (showAttachmentMenu) {
                AttachmentMenuSheet(
                    onDismiss = { showAttachmentMenu = false },
                    onSelectOption = { option ->
                        showAttachmentMenu = false
                        when (option) {
                            "document" -> showDocumentPicker = true
                            "gallery" -> showGalleryPicker = true
                            "camera" -> showCameraPicker = true
                            "audio" -> showAudioPicker = true
                            "location" -> showLocationPicker = true
                            "contact" -> showContactPicker = true
                        }
                    }
                )
            }

            // Attachment Option Dialogs
            if (showDocumentPicker) {
                DocumentPickerDialog(
                    onDismiss = { showDocumentPicker = false },
                    onSend = { fileName, fileSize ->
                        viewModel.sendDocumentAttachment(fileName, fileSize)
                        showDocumentPicker = false
                    }
                )
            }

            if (showGalleryPicker) {
                GalleryPickerDialog(
                    onDismiss = { showGalleryPicker = false },
                    onSend = { uris, caption ->
                        viewModel.sendGalleryAttachments(uris, caption)
                        showGalleryPicker = false
                    }
                )
            }

            if (showCameraPicker) {
                CameraCaptureDialog(
                    onDismiss = { showCameraPicker = false },
                    onSend = { uri, isVideo, caption ->
                        viewModel.sendCameraAttachment(uri, isVideo, caption)
                        showCameraPicker = false
                    }
                )
            }

            if (showAudioPicker) {
                AudioPickerDialog(
                    onDismiss = { showAudioPicker = false },
                    onSend = { fileName, fileSize, duration ->
                        viewModel.sendAudioAttachment(fileName, fileSize, duration)
                        showAudioPicker = false
                    }
                )
            }

            if (showLocationPicker) {
                LocationPickerDialog(
                    onDismiss = { showLocationPicker = false },
                    onSend = { lat, lng, address, liveDurationMs ->
                        viewModel.sendLocationAttachment(lat, lng, address, liveDurationMs)
                        showLocationPicker = false
                    }
                )
            }

            if (showContactPicker) {
                ContactPickerDialog(
                    onDismiss = { showContactPicker = false },
                    onSend = { name, phone ->
                        viewModel.sendContactAttachment(name, phone)
                        showContactPicker = false
                    }
                )
            }

            // Edit dialog
            if (editingMessage != null) {
                AlertDialog(
                    onDismissRequest = { editingMessage = null },
                    title = { Text("Edit Message", fontWeight = FontWeight.Bold) },
                    text = {
                        OutlinedTextField(
                            value = editInput,
                            onValueChange = { editInput = it },
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.editMessage(editingMessage!!, editInput)
                            editingMessage = null
                        }) {
                            Text("Save")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { editingMessage = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Forward Dialog
            showForwardDialog?.let { msg ->
                AlertDialog(
                    onDismissRequest = { showForwardDialog = null },
                    title = { Text("Forward Message to", fontWeight = FontWeight.Bold) },
                    text = {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(chats) { chat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            viewModel.forwardMessage(msg, chat.chatId)
                                            showForwardDialog = null
                                        }
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AsyncImage(
                                        model = chat.groupPhoto,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.size(36.dp).clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(chat.groupName, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showForwardDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Disappearing duration config selector
            if (showDisappearingDialog) {
                AlertDialog(
                    onDismissRequest = { showDisappearingDialog = false },
                    title = { Text("Disappearing Messages", fontWeight = FontWeight.Bold) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Set message automatic expiration duration:", fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            val currentDuration = activeChat?.disappearingDuration ?: 0L
                            
                            val options = listOf(
                                "Off" to 0L,
                                "24 Hours" to 86400000L,
                                "7 Days" to 604800000L,
                                "90 Days" to 7776000000L
                            )

                            options.forEach { (label, duration) ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            activeChat?.let { viewModel.updateChatDisappearing(it.chatId, duration) }
                                            showDisappearingDialog = false
                                        }
                                        .padding(vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = currentDuration == duration,
                                        onClick = {
                                            activeChat?.let { viewModel.updateChatDisappearing(it.chatId, duration) }
                                            showDisappearingDialog = false
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(label, fontSize = 15.sp)
                                }
                            }
                        }
                    },
                    confirmButton = {}
                )
            }

            // Retry send dialog
            showRetryDialog?.let { failedMsg ->
                AlertDialog(
                    onDismissRequest = { showRetryDialog = null },
                    title = { Text("Send Failure", fontWeight = FontWeight.Bold) },
                    text = { Text("This message could not be sent because you are currently offline. Retry sending now?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.retrySendMessage(failedMsg)
                            showRetryDialog = null
                        }) {
                            Text("Retry Sending")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showRetryDialog = null }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            // Export Chat Dialog
            if (showExportDialog) {
                ExportChatDialog(
                    viewModel = viewModel,
                    chatId = activeChatId ?: "chat",
                    onDismiss = { showExportDialog = false },
                    onExportComplete = { fileName, content ->
                        exportResultInfo = Pair(fileName, content)
                        showExportDialog = false
                    }
                )
            }

            // Export Result Preview Dialog
            exportResultInfo?.let { (fileName, fileContent) ->
                ExportResultDialog(
                    fileName = fileName,
                    fileContent = fileContent,
                    onDismiss = { exportResultInfo = null }
                )
            }

            // Schedule Message Dialog
            if (showScheduleDialog) {
                ScheduleMessageDialog(
                    viewModel = viewModel,
                    currentInputText = messageInput,
                    chatId = activeChatId ?: "chat",
                    onDismiss = { showScheduleDialog = false },
                    onScheduled = {
                        messageInput = ""
                        showScheduleDialog = false
                    }
                )
            }

            // Lightbox Fullscreen Image Viewer Dialog
            lightboxImageUrl?.let { url ->
                LightboxImageViewerDialog(
                    imageUrl = url,
                    onDismiss = { lightboxImageUrl = null }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    viewModel: ChatViewModel,
    onReplyClick: () -> Unit,
    onDeleteMe: () -> Unit,
    onDeleteEveryone: () -> Unit,
    onEditClick: () -> Unit,
    onForwardClick: () -> Unit,
    onFailedClick: () -> Unit,
    onImageClick: (String) -> Unit = {}
) {
    var isVisible by remember { mutableStateOf(false) }
    LaunchedEffect(message.messageId) {
        isVisible = true
    }

    var expandedMenu by remember { mutableStateOf(false) }

    val containerColor = if (isMe) {
        Color(0xFF1B2A5E) // AmBle deep navy blue for sent messages
    } else {
        Color(0xFFFFFFFF) // White / Soft sky blue for received messages
    }

    val contentColor = if (isMe) {
        Color.White // White text on dark navy surface
    } else {
        Color(0xFF1B2A5E) // Dark navy text on white surface
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessMediumLow)) +
                scaleIn(
                    initialScale = 0.82f,
                    transformOrigin = if (isMe) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f),
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ) +
                slideInHorizontally(
                    initialOffsetX = { if (isMe) it / 3 else -it / 3 },
                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow)
                ),
        exit = fadeOut(animationSpec = spring(stiffness = Spring.StiffnessMedium)) +
               scaleOut(
                   targetScale = 0.82f,
                   transformOrigin = if (isMe) TransformOrigin(1f, 1f) else TransformOrigin(0f, 1f),
                   animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
               ) +
               slideOutHorizontally(
                   targetOffsetX = { if (isMe) it / 3 else -it / 3 },
                   animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
               )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 1.dp),
            contentAlignment = if (isMe) Alignment.CenterEnd else Alignment.CenterStart
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Left Failure warning exclamation mark for offline simulated messages
            if (isMe && message.sendFailed) {
                IconButton(onClick = onFailedClick) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = "Send failed",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start,
                modifier = Modifier.widthIn(max = 295.dp)
            ) {
                if (message.type == "sticker") {
                    StickerMessageBubble(
                        message = message,
                        isMe = isMe,
                        onLongClick = { expandedMenu = true }
                    )
                } else {
                    Card(
                        shape = RoundedCornerShape(
                            topStart = 24.dp,
                            topEnd = 24.dp,
                            bottomStart = if (isMe) 24.dp else 4.dp,
                            bottomEnd = if (isMe) 4.dp else 24.dp
                        ),
                        colors = CardDefaults.cardColors(containerColor = containerColor),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier
                            .combinedClickable(
                                onLongClick = { expandedMenu = true },
                                onClick = {}
                            )
                    ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        // Forward status marker
                        if (message.isForwarded) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Reply,
                                    contentDescription = null,
                                    tint = contentColor.copy(alpha = 0.6f),
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    "Forwarded",
                                    fontSize = 11.sp,
                                    color = contentColor.copy(alpha = 0.6f),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Reply quote display
                        if (message.replyToMessageId != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(1.5.dp, Color(0xFF3B7DD8)), RoundedCornerShape(8.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = message.replyToText ?: "Reply reference",
                                    fontSize = 12.sp,
                                    color = contentColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                        }

                        // Render based on attachment type
                        when (message.type) {
                            "voice" -> {
                                VoiceNotePlayerBubble(
                                    message = message,
                                    isMe = isMe,
                                    viewModel = viewModel,
                                    contentColor = contentColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            "file" -> {
                                DocumentMessageBubble(
                                    message = message,
                                    contentColor = contentColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            "image", "gallery", "video" -> {
                                MediaMessageBubble(
                                    message = message,
                                    contentColor = contentColor,
                                    onImageClick = { onImageClick(it) }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            "audio" -> {
                                AudioMessageBubble(
                                    message = message,
                                    isMe = isMe,
                                    viewModel = viewModel,
                                    contentColor = contentColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            "location", "live_location" -> {
                                LocationMessageBubble(
                                    message = message,
                                    contentColor = contentColor
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                            "contact" -> {
                                ContactMessageBubble(
                                    message = message,
                                    contentColor = contentColor,
                                    onMessageClick = {
                                        // Quick reply to contact
                                    }
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                            }
                        }

                        // Text body
                        Text(
                            text = message.text,
                            fontSize = 14.sp,
                            color = contentColor
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Bottom row: Time, Star icon, Edited, ticks
                        Row(
                            modifier = Modifier.align(Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (message.isStarred) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = "Starred",
                                    tint = Color(0xFFFFC107),
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            if (message.isEdited) {
                                Text("Edited", fontSize = 10.sp, color = contentColor.copy(alpha = 0.5f))
                            }

                            Text(
                                text = formatTime(message.timestamp),
                                fontSize = 11.sp,
                                color = contentColor.copy(alpha = 0.6f)
                            )

                            if (isMe) {
                                when {
                                    message.status == "retrying" || message.sendFailed -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Retrying",
                                                tint = Color(0xFFFB923C),
                                                modifier = Modifier.size(13.dp)
                                            )
                                            Text(
                                                text = "Retrying",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color(0xFFFB923C)
                                            )
                                        }
                                    }
                                    message.status == "sent" -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sent",
                                            tint = contentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    message.status == "delivered" -> {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Delivered",
                                            tint = contentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    message.status == "read" -> {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = "Read",
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    else -> {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Sent",
                                            tint = contentColor.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

                // Emojis reaction pills layout
                if (message.reactions.isNotEmpty()) {
                    Row(
                        modifier = Modifier.padding(top = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        val reactionsList = message.reactions.split(",")
                        reactionsList.groupBy { it.substringBefore(":") }.forEach { (emoji, list) ->
                            Box(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
                                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                    .clickable { viewModel.toggleMessageReaction(message, emoji) }
                            ) {
                                Text(
                                    text = "$emoji ${list.size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // WhatsApp Style Long-Press Dropdown menu with dynamic emojis reaction top bar
        DropdownMenu(
            expanded = expandedMenu,
            onDismissRequest = { expandedMenu = false }
        ) {
            // Horizontal reaction emojis row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("❤️", "😂", "👍", "😮", "🙏", "🔥").forEach { emoji ->
                    Text(
                        text = emoji,
                        fontSize = 24.sp,
                        modifier = Modifier
                            .clickable {
                                viewModel.toggleMessageReaction(message, emoji)
                                expandedMenu = false
                            }
                            .padding(4.dp)
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            DropdownMenuItem(
                text = { Text("Reply") },
                onClick = { onReplyClick(); expandedMenu = false },
                leadingIcon = { Icon(Icons.Default.Reply, contentDescription = null) }
            )

            DropdownMenuItem(
                text = { Text(if (message.isStarred) "Unstar Message" else "Star Message") },
                onClick = { viewModel.toggleMessageStar(message); expandedMenu = false },
                leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = ChatWaveOrange) }
            )

            DropdownMenuItem(
                text = { Text("Forward Message") },
                onClick = { onForwardClick(); expandedMenu = false },
                leadingIcon = { Icon(Icons.Default.Share, contentDescription = null) }
            )

            if (isMe && !message.isDeleted) {
                DropdownMenuItem(
                    text = { Text("Edit Message") },
                    onClick = { onEditClick(); expandedMenu = false },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                )
                DropdownMenuItem(
                    text = { Text("Delete for Everyone") },
                    onClick = { onDeleteEveryone(); expandedMenu = false },
                    leadingIcon = { Icon(Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                )
            }
            DropdownMenuItem(
                text = { Text("Delete for Me") },
                onClick = { onDeleteMe(); expandedMenu = false },
                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null) }
            )
        }
    }
}
}

@Composable
fun VoiceNotePlayer(filePath: String) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playbackSpeed by remember { mutableFloatStateOf(1.0f) }

    DisposableEffect(filePath) {
        onDispose {
            try {
                mediaPlayer?.stop()
                mediaPlayer?.release()
            } catch (e: Exception) {
                // Ignore
            }
            mediaPlayer = null
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        IconButton(
            onClick = {
                try {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        if (mediaPlayer == null) {
                            mediaPlayer = MediaPlayer().apply {
                                setDataSource(filePath)
                                prepare()
                                start()
                                setOnCompletionListener {
                                    isPlaying = false
                                    progress = 0f
                                }
                            }
                        } else {
                            mediaPlayer?.start()
                        }
                        isPlaying = true
                    }
                } catch (e: Exception) {
                    isPlaying = !isPlaying
                }
            },
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Custom wavy Canvas visualizer simulation
        Canvas(
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
        ) {
            val count = 20
            val spacing = size.width / count
            for (i in 0 until count) {
                val speedFactor = playbackSpeed
                val height = if (isPlaying) {
                    (Math.sin((i.toFloat() + System.currentTimeMillis() * speedFactor / 150.0)) * 10 + 12).coerceAtLeast(3.0)
                } else {
                    (Math.sin(i.toDouble()) * 4 + 8).coerceAtLeast(2.0)
                }
                drawRect(
                    color = if (i < count * progress) ChatWaveTeal else Color.LightGray,
                    topLeft = androidx.compose.ui.geometry.Offset(i * spacing + 2, (size.height - height.toFloat()) / 2),
                    size = androidx.compose.ui.geometry.Size(4f, height.toFloat())
                )
            }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Playback Speed Button (1x -> 1.5x -> 2x)
        TextButton(
            onClick = {
                playbackSpeed = when (playbackSpeed) {
                    1.0f -> 1.5f
                    1.5f -> 2.0f
                    else -> 1.0f
                }
            },
            colors = ButtonDefaults.textButtonColors(contentColor = ChatWaveTeal),
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
            modifier = Modifier.height(28.dp)
        ) {
            Text("${playbackSpeed}x", fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class StickerPack(
    val id: String,
    val name: String,
    val icon: String,
    val items: List<StickerItem>
)

data class StickerItem(
    val id: String,
    val url: String,
    val name: String,
    val isEmoji: Boolean = false
)

data class GifItem(
    val id: String,
    val url: String,
    val title: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StickerMessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    onLongClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(vertical = 4.dp)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = {}
            )
    ) {
        Column(horizontalAlignment = if (isMe) Alignment.End else Alignment.Start) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.15f))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (message.mediaUrl != null && message.mediaUrl.startsWith("http")) {
                    AsyncImage(
                        model = message.mediaUrl,
                        contentDescription = "Sticker",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Text(
                        text = message.mediaUrl ?: message.text.removePrefix("🏷️ "),
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color.Black.copy(alpha = 0.5f),
                modifier = Modifier.padding(horizontal = 2.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = formatTime(message.timestamp),
                        fontSize = 10.sp,
                        color = Color.White
                    )
                    if (isMe) {
                        val tickColor = if (message.status == "read") Color(0xFF4FC3F7) else Color.LightGray
                        Icon(
                            imageVector = if (message.status == "sent") Icons.Default.Check else Icons.Default.DoneAll,
                            contentDescription = null,
                            tint = tickColor,
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomStickerStudioDialog(
    onDismiss: () -> Unit,
    onStickerCreated: (stickerUrl: String, stickerName: String) -> Unit
) {
    var selectedMediaType by remember { mutableStateOf("photo") }
    var selectedUri by remember { mutableStateOf<String?>(null) }
    var stickerNameInput by remember { mutableStateOf("Custom Sticker") }
    var captionText by remember { mutableStateOf("AmBle ✨") }
    var textColorHex by remember { mutableLongStateOf(0xFF00FFCC) }
    var cutoutShape by remember { mutableStateOf("squircle") }
    var strokeStyle by remember { mutableStateOf("glow") }
    var selectedEmojiOverlay by remember { mutableStateOf("🔥") }
    
    var videoDurationSeconds by remember { mutableFloatStateOf(6f) }
    var videoSpeed by remember { mutableFloatStateOf(1.0f) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri.toString()
            selectedMediaType = "photo"
        }
    }

    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            selectedUri = uri.toString()
            selectedMediaType = "video"
        }
    }

    val samplePhotoTemplates = listOf(
        "https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400",
        "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400",
        "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=400",
        "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400"
    )

    val activeImage = selectedUri ?: samplePhotoTemplates.first()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Custom Sticker Studio", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    "Create animated or static stickers from gallery photos and videos (up to 10 sec clip)",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { photoLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Photo", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    Button(
                        onClick = { videoLauncher.launch("video/*") },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Video (10s)", fontSize = 11.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .clip(
                            when (cutoutShape) {
                                "circle" -> CircleShape
                                else -> RoundedCornerShape(20.dp)
                            }
                        )
                        .background(
                            when (strokeStyle) {
                                "glow" -> Brush.radialGradient(listOf(Color(0xFF00FFCC), Color.Transparent))
                                "pink" -> Brush.radialGradient(listOf(Color(0xFFFF007F), Color.Transparent))
                                "yellow" -> Brush.radialGradient(listOf(Color(0xFFFFD700), Color.Transparent))
                                else -> Brush.linearGradient(listOf(Color.LightGray, Color.White))
                            }
                        )
                        .padding(if (strokeStyle != "none") 6.dp else 0.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(
                                when (cutoutShape) {
                                    "circle" -> CircleShape
                                    else -> RoundedCornerShape(16.dp)
                                }
                            )
                    ) {
                        AsyncImage(
                            model = activeImage,
                            contentDescription = "Sticker Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )

                        Text(
                            text = selectedEmojiOverlay,
                            fontSize = 26.sp,
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(6.dp)
                        )

                        if (selectedMediaType == "video") {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.Black.copy(alpha = 0.75f),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(6.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Green, modifier = Modifier.size(12.dp))
                                    Text("${videoDurationSeconds.toInt()}s clip • ${videoSpeed}x", fontSize = 10.sp, color = Color.White)
                                }
                            }
                        }

                        if (captionText.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color.Black.copy(alpha = 0.7f),
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(bottom = 6.dp)
                            ) {
                                Text(
                                    text = captionText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(textColorHex),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                if (selectedMediaType == "video") {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("🎬 Trim Video Length (Max 10s)", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text("${videoDurationSeconds.toInt()}s", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = videoDurationSeconds,
                                onValueChange = { videoDurationSeconds = it },
                                valueRange = 1f..10f,
                                steps = 8
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("⚡ Video Speed", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    listOf(0.5f to "0.5x", 1.0f to "1x", 1.5f to "1.5x", 2.0f to "2x").forEach { (speed, label) ->
                                        FilterChip(
                                            selected = videoSpeed == speed,
                                            onClick = { videoSpeed = speed },
                                            label = { Text(label, fontSize = 10.sp) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = captionText,
                    onValueChange = { captionText = it },
                    label = { Text("Caption Text") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = stickerNameInput,
                    onValueChange = { stickerNameInput = it },
                    label = { Text("Sticker Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Text("Border Outline Effect", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("glow" to "✨ Neon", "pink" to "💖 Pink", "yellow" to "⭐ Gold", "none" to "Off").forEach { (style, label) ->
                        FilterChip(
                            selected = strokeStyle == style,
                            onClick = { strokeStyle = style },
                            label = { Text(label, fontSize = 10.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text("Emoji Stamp", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(listOf("🔥", "❤️", "🤣", "😎", "💯", "🎉", "⚡", "👑", "👀", "✨", "🚀", "💖")) { emoji ->
                        Surface(
                            onClick = { selectedEmojiOverlay = emoji },
                            shape = CircleShape,
                            color = if (selectedEmojiOverlay == emoji) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(6.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val finalName = if (captionText.isNotEmpty()) "$stickerNameInput ($captionText)" else stickerNameInput
                    onStickerCreated(activeImage, finalName)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Create & Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}

@Composable
fun WhatsAppEmojiStickerPickerPanel(
    onEmojiSelect: (String) -> Unit,
    onBackspace: () -> Unit,
    onStickerSelect: (stickerUrl: String, stickerName: String) -> Unit,
    onGifSelect: (gifUrl: String, title: String) -> Unit,
    onClose: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var emojiSearchQuery by remember { mutableStateOf("") }
    var selectedEmojiCategory by remember { mutableIntStateOf(0) }
    var showCustomStickerStudio by remember { mutableStateOf(false) }

    val myCustomStickers = remember {
        mutableStateListOf(
            StickerItem("custom1", "https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400", "Playful AmBle"),
            StickerItem("custom2", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=400", "Vibe Master")
        )
    }

    if (showCustomStickerStudio) {
        CustomStickerStudioDialog(
            onDismiss = { showCustomStickerStudio = false },
            onStickerCreated = { url, name ->
                val newSticker = StickerItem("user_${System.currentTimeMillis()}", url, name)
                myCustomStickers.add(0, newSticker)
                onStickerSelect(url, name)
            }
        )
    }

    val emojiCategories = listOf(
        "🕒 Popular" to listOf("😂", "❤️", "😍", "👍", "🔥", "🙏", "😊", "✨", "🎉", "🤣", "🥹", "🥰", "🥳", "😭", "🙌", "💯", "👀", "😎", "🚀", "💀", "🫡", "🤝", "⚡", "💖", "🫶"),
        "😀 Smileys" to listOf("😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "🥹", "😊", "😇", "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🥸", "🤩", "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔", "🫣", "🤭", "🫡", "🤫", "🫠", "🤥", "😶", "🫥", "😐", "😑", "😬", "🙄", "😯", "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐", "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈", "👿", "👹", "👺", "🤡", "💩", "👻", "💀", "☠️", "👽", "👾", "🤖", "🎃"),
        "🖐️ Gestures" to listOf("👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞", "🫰", "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "🫵", "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "🫶", "👐", "🤲", "🤝", "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂", "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅", "👄", "🫦", "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩", "🧓", "👴", "👵", "👲", "👳‍♀️", "👳‍♂️", "🧕", "👮‍♂️", "👮‍♀️", "👷‍♂️", "👷‍♀️", "💂‍♂️", "💂‍♀️", "🕵️‍♂️", "🕵️‍♀️", "👩‍⚕️", "👨‍⚕️", "👩‍🌾", "👨‍🌾", "👩‍🍳", "👨‍🍳", "👩‍🎓", "👨‍🎓", "👩‍🎤", "👨‍🎤", "👩‍🏫", "👨‍🏫", "👩‍🏭", "👨‍🏭", "👩‍💻", "👨‍💻", "👩‍💼", "👨‍💼", "👩‍🔧", "👨‍🔧", "👩‍🔬", "👨‍🔬", "👩‍🎨", "👨‍🎨", "👩‍🚒", "👨‍🚒", "👩‍✈️", "👨‍✈️", "👩‍🚀", "👨‍🚀", "👩‍⚖️", "👨‍⚖️", "🦸‍♂️", "🦸‍♀️", "🦹‍♂️", "🦹‍♀️", "🧙‍♂️", "🧙‍♀️", "🧚‍♂️", "🧚‍♀️", "🧛‍♂️", "🧛‍♀️", "🧜‍♂️", "🧜‍♀️", "🧝‍♂️", "🧝‍♀️", "🧞‍♂️", "🧞‍♀️", "🧟‍♂️", "🧟‍♀️", "💃", "🕺"),
        "🐶 Animals" to listOf("🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨", "🐯", "🦁", "🐮", "🐷", "🐸", "🐵", "🙈", "🙉", "🙊", "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉", "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪲", "🐛", "🦋", "🐌", "🐞", "🐜", "🦟", "🦗", "🕷️", "🦂", "🐢", "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡", "🐠", "🐟", "🐬", "🐳", "🐋", "🦈", "🦭", "🐊", "🐅", "🐆", "🦓", "🦬", "🐘", "🦣", "🦏", "🦛", "🐪", "🐫", "🦒", "🦘", "🦬", "🐃", "🐂", "🐎", "🐖", "🐏", "🐑", "🦙", "🐐", "🦌", "🐕", "🐩", "🦮", "🐕‍🦺", "🐈", "🐈‍⬛", "🐓", "🦃", "🦚", "🦜", "🦩", "🕊️", "🐇", "🦝", "🦨", "🦡", "🦫", "🦦", "🦥", "🐁", "🐀", "🐿️", "🦔", "🐾", "🐉", "🐲", "🌵", "🎄", "🌲", "🌳", "🌴", "🌱", "🌿", "☘️", "🍀", "🎍", "🪴", "🎋", "🍃", "🍂", "🍁", "🍄", "🐚", "🪨", "🌾", "💐", "🌷", "🌹", "🥀", "🌺", "🌸", "🌼", "🌻", "🌞", "🌝", "🌛", "🌜", "🌚", "🌕", "🌖", "🌗", "🌘", "🌑", "🌒", "🌓", "🌔", "🌙", "🌎", "🌍", "🌏", "🪐", "💫", "⭐️", "🌟", "✨", "⚡️", "☄️", "💥", "🔥", "🌪️", "🌈", "☀️", "🌤️", "⛅️", "🌥️", "☁️", "🌦️", "🌧️", "⛈️", "🌩️", "🌨️", "❄️", "☃️", "⛄️", "🌬️", "💨", "💧", "💦", "☔️", "☂️", "🌊", "🌫️"),
        "🍕 Food & Drink" to listOf("🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑", "🥦", "🥬", "🥒", "🌶️", "🌽", "🥕", "🧄", "🧅", "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳", "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🌭", "🍔", "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🫔", "🥗", "🥘", "🫕", "🥫", "🍝", "🍜", "🍲", "🍛", "🍣", "🍱", "🥟", "🦪", "🍤", "🍙", "🍚", "🍘", "🍥", "🥠", "🥮", "🍢", "🍡", "🍧", "🍨", "🍦", "🥧", "🧁", "🍰", "🎂", "🍮", "🍭", "🍬", "🍫", "🍿", "🍩", "🍪", "🌰", "🥜", "🍯", "🥛", "☕️", "🍵", "🧃", "🥤", "🧋", "🍶", "🍺", "🍻", "🥂", "🍷", "🥃", "🍸", "🍹", "🍾", "🍹", "🧉", "🍾", "🧊", "🥄", "🍴", "🍽️", "🥣", "🥡", "🥢", "🧂"),
        "⚽ Sports & Fun" to listOf("⚽️", "🏀", "🏈", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱", "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳️", "🪁", "🏹", "🎣", "🦺", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷", "⛸️", "🎿", "🏂", "🏋️‍♂️", "🤸‍♀️", "🧘‍♀️", "🏄‍♂️", "🏊‍♂️", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🎯", "🎮", "🎲", "🧩", "🎭", "🎨", "🎬", "🎤", "🎧", "🎼", "🎹", "🥁", "🎷", "🎺", "🎸", "🪕", "🎻", "🪗", "🎬", "🎪", "🎫", "🎟️", "🎟️", "🎗️", "📌", "📍"),
        "🚗 Places" to listOf("🚗", "🚕", "🚙", "🚌", "🏎️", "🚓", "🚑", "🚒", "🚐", "🛻", "🚚", "🚛", "🚜", "🛴", "🚲", "🛵", "🏍️", "🚨", "🚔", "✈️", "🛫", "🛬", "🚀", "🛸", "🚁", "⛵️", "🚤", "🛳️", "⚓️", "🚥", "🗺️", "🗿", "🗽", "🗼", "🏰", "🏯", "🎡", "🎢", "🏖️", "🏝️", "🌋", "⛰️", "🏔️", "🏕️", "🏠", "🏡", "🏢", "🏦", "🏨", "🏫", "🏛️", "⛪️", "🕌", "🛕", "🕍", "⛩️", "🕋", "🌁", "🌃", "🏙️", "🌅", "🌄", "🌆", "🌇", "🌉", "♨️", "🎠", "🎡", "🎢", "💈", "🎪"),
        "💡 Tech & Objects" to listOf("⌚️", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "💽", "💾", "💿", "📀", "🎥", "🎞️", "📽️", "🎬", "📺", "📷", "📸", "📹", "🔍", "🔎", "🕯️", "💡", "🔦", "🏮", "📔", "📕", "📖", "📗", "📘", "📙", "📚", "📓", "📒", "📃", "📜", "📄", "📰", "🏷️", "💰", "🪙", "💴", "💵", "💶", "💷", "💸", "💳", "✉️", "📧", "📦", "✏️", "✒️", "📝", "📁", "📂", "📅", "📊", "📌", "📍", "📎", "✂️", "🔒", "🔓", "🔑", "🔨", "🪓", "💣", "🛡️", "🔧", "⚙️", "🧪", "🔬", "🔭", "📡", "💉", "💊", "🚪", "🛋️", "🛒", "🎁", "🎉", "🎈", "💎", "🔮", "🪄", "🪞", "🪟", "🛎️", "🔑", "🔑", "🗡️", "⚔️", "🏹", "🛡️"),
        "💖 Hearts & Symbols" to listOf("❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔", "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "❤️‍🔥", "❤️‍🩹", "☮️", "✝️", "☪️", "🕉️", "☸️", "✡️", "☯️", "♈️", "♉️", "♊️", "♋️", "♌️", "♍️", "♎️", "♏️", "♐️", "♑️", "♒️", "♓️", "⚛️", "✴️", "🆚", "💮", "🅰️", "🅱️", "🆎", "🅾️", "🆘", "❌", "⭕️", "🛑", "⛔️", "💯", "💢", "♨️", "⚠️", "🔱", "🔰", "♻️", "✅", "❇️", "🌐", "💠", "💤", "🏧", "♿️", "🅿️", "🈳", "🛃", "🛄", "🛅", "🚹", "🚺", "🚼", "🚻", "🚮", "ℹ️", "🆗", "🆙", "🆕", "🆓", "0️⃣", "1️⃣", "2️⃣", "3️⃣", "4️⃣", "5️⃣", "6️⃣", "7️⃣", "8️⃣", "9️⃣", "🔟", "▶️", "⏸️", "⏹️", "⏺️", "➡️", "⬅️", "⬆️", "⬇️", "🔄", "🔴", "🔵", "🟡", "🟢", "🟣", "🟠", "⚫️", "⚪️", "🟤", "⬛️", "⬜️", "🏳️", "🏴", "🏁", "🚩", "🏳️‍🌈", "🏴‍☠️")
    )

    val stickerPacks = remember(myCustomStickers.size) {
        listOf(
            StickerPack(
                id = "my_custom",
                name = "✨ My Stickers",
                icon = "✨",
                items = myCustomStickers.toList()
            ),
            StickerPack(
                id = "cats",
                name = "AmBle Cats",
                icon = "🐱",
                items = listOf(
                    StickerItem("c1", "https://images.unsplash.com/photo-1514888286974-6c03e2ca1dba?w=400", "Happy Kitty"),
                    StickerItem("c2", "https://images.unsplash.com/photo-1573865526739-10659fec78a5?w=400", "Sleepy Paws"),
                    StickerItem("c3", "https://images.unsplash.com/photo-1533738363-b7f9aef128ce?w=400", "Cool Shades Cat"),
                    StickerItem("c4", "https://images.unsplash.com/photo-1543852786-1cf6624b9987?w=400", "Playful Whiskers"),
                    StickerItem("c5", "https://images.unsplash.com/photo-1495360010541-f48722b34f7d?w=400", "Shocked Fluff"),
                    StickerItem("c6", "https://images.unsplash.com/photo-1561948955-570b270e7c36?w=400", "Gentleman Cat")
                )
            ),
            StickerPack(
                id = "3d",
                name = "3D Glossy",
                icon = "🎭",
                items = listOf(
                    StickerItem("3d1", "😍", "Love Eyes", isEmoji = true),
                    StickerItem("3d2", "🔥", "Super Fire", isEmoji = true),
                    StickerItem("3d3", "🥳", "Party Horn", isEmoji = true),
                    StickerItem("3d4", "🚀", "To The Moon", isEmoji = true),
                    StickerItem("3d5", "🤯", "Mind Blown", isEmoji = true),
                    StickerItem("3d6", "😎", "Super Cool", isEmoji = true),
                    StickerItem("3d7", "💯", "One Hundred", isEmoji = true),
                    StickerItem("3d8", "👑", "Royal Crown", isEmoji = true)
                )
            ),
            StickerPack(
                id = "memes",
                name = "Memes & Art",
                icon = "🐸",
                items = listOf(
                    StickerItem("m1", "https://images.unsplash.com/photo-1579783902614-a3fb3927b675?w=400", "Pop Art Wow"),
                    StickerItem("m2", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=400", "Neon Wave"),
                    StickerItem("m3", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=400", "Cosmic Dream"),
                    StickerItem("m4", "https://images.unsplash.com/photo-1509198397868-475647b2a1e5?w=400", "Winner Gamer")
                )
            ),
            StickerPack(
                id = "love",
                name = "Cute Hearts",
                icon = "💖",
                items = listOf(
                    StickerItem("l1", "💖", "Sparkle Heart", isEmoji = true),
                    StickerItem("l2", "💘", "Heart Arrow", isEmoji = true),
                    StickerItem("l3", "💝", "Heart Ribbon", isEmoji = true),
                    StickerItem("l4", "💞", "Revolving Hearts", isEmoji = true),
                    StickerItem("l5", "💗", "Growing Heart", isEmoji = true),
                    StickerItem("l6", "🧸", "Teddy Bear", isEmoji = true)
                )
            ),
            StickerPack(
                id = "boba",
                name = "Boba & Food",
                icon = "🧋",
                items = listOf(
                    StickerItem("b1", "🧋", "Boba Milk Tea", isEmoji = true),
                    StickerItem("b2", "🍕", "Delicious Pizza", isEmoji = true),
                    StickerItem("b3", "🍩", "Glazed Donut", isEmoji = true),
                    StickerItem("b4", "🍦", "Soft Ice Cream", isEmoji = true),
                    StickerItem("b5", "🥑", "Fresh Avocado", isEmoji = true),
                    StickerItem("b6", "🍣", "Fresh Sushi", isEmoji = true)
                )
            )
        )
    }

    var selectedStickerPackIndex by remember { mutableIntStateOf(0) }

    val gifs = listOf(
        GifItem("g1", "https://images.unsplash.com/photo-1518173946687-a4c8a383392e?w=500", "Nature Vibe"),
        GifItem("g2", "https://images.unsplash.com/photo-1534447677768-be436bb09401?w=500", "Galaxy Stars"),
        GifItem("g3", "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?w=500", "Beach Sunset"),
        GifItem("g4", "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500", "Retro Arcade"),
        GifItem("g5", "https://images.unsplash.com/photo-1508700115892-45ecd05ae2ad?w=500", "Neon Party"),
        GifItem("g6", "https://images.unsplash.com/photo-1513151233558-d860c5398176?w=500", "Confetti Yay")
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        label = { Text("😀 Emojis", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { if (selectedTab == 0) Icon(Icons.Default.SentimentSatisfied, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        label = { Text("🏷️ Stickers", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { if (selectedTab == 1) Icon(Icons.Default.Sell, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    FilterChip(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        label = { Text("🎞️ GIFs", fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = { if (selectedTab == 2) Icon(Icons.Default.Gif, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                }

                IconButton(onClick = onClose, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = emojiSearchQuery,
                            onValueChange = { emojiSearchQuery = it },
                            placeholder = { Text("Search emojis...", fontSize = 12.sp) },
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp),
                            shape = RoundedCornerShape(20.dp),
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            singleLine = true
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        IconButton(
                            onClick = onBackspace,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f))
                        ) {
                            Icon(Icons.Default.Backspace, contentDescription = "Backspace", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                        }
                    }

                    if (emojiSearchQuery.isEmpty()) {
                        LazyRow(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            itemsIndexed(emojiCategories) { index, (catName, _) ->
                                Surface(
                                    onClick = { selectedEmojiCategory = index },
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (selectedEmojiCategory == index) MaterialTheme.colorScheme.primary else Color.Transparent,
                                    border = BorderStroke(1.dp, if (selectedEmojiCategory == index) Color.Transparent else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                                ) {
                                    Text(
                                        text = catName,
                                        fontSize = 11.sp,
                                        fontWeight = if (selectedEmojiCategory == index) FontWeight.Bold else FontWeight.Normal,
                                        color = if (selectedEmojiCategory == index) Color.White else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }

                    val displayedEmojis = remember(emojiSearchQuery, selectedEmojiCategory) {
                        if (emojiSearchQuery.isNotEmpty()) {
                            emojiCategories.flatMap { it.second }.filter { it.contains(emojiSearchQuery) }.distinct()
                        } else {
                            emojiCategories.getOrNull(selectedEmojiCategory)?.second ?: emptyList()
                        }
                    }

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 40.dp),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(displayedEmojis) { emoji ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onEmojiSelect(emoji) },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = emoji, fontSize = 24.sp)
                            }
                        }
                    }
                }
            }

            if (selectedTab == 1) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LazyRow(
                            modifier = Modifier.weight(1f),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            itemsIndexed(stickerPacks) { index, pack ->
                                Surface(
                                    onClick = { selectedStickerPackIndex = index },
                                    shape = RoundedCornerShape(14.dp),
                                    color = if (selectedStickerPackIndex == index) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                    border = BorderStroke(1.dp, if (selectedStickerPackIndex == index) MaterialTheme.colorScheme.primary else Color.Transparent)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(pack.icon, fontSize = 15.sp)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = pack.name,
                                            fontSize = 11.sp,
                                            fontWeight = if (selectedStickerPackIndex == index) FontWeight.Bold else FontWeight.Medium,
                                            color = if (selectedStickerPackIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.width(6.dp))

                        Button(
                            onClick = { showCustomStickerStudio = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Create", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    val activePack = stickerPacks.getOrNull(selectedStickerPackIndex) ?: stickerPacks.first()

                    if (activePack.items.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No custom stickers created yet!", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { showCustomStickerStudio = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                ) {
                                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Make Sticker from Photo/Video")
                                }
                            }
                        }
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(activePack.items) { item ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(90.dp)
                                        .clickable {
                                            onStickerSelect(item.url, item.name)
                                        },
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        if (item.isEmoji) {
                                            Text(text = item.url, fontSize = 42.sp)
                                        } else {
                                            AsyncImage(
                                                model = item.url,
                                                contentDescription = item.name,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .size(54.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = item.name,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (selectedTab == 2) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "Trending WhatsApp GIFs",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 4.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(gifs) { gif ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clickable {
                                        onGifSelect(gif.url, gif.title)
                                    },
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    AsyncImage(
                                        model = gif.url,
                                        contentDescription = gif.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .align(Alignment.BottomCenter)
                                            .background(Color.Black.copy(alpha = 0.6f))
                                            .padding(4.dp)
                                    ) {
                                        Text(
                                            text = gif.title,
                                            fontSize = 11.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold,
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AttachmentMenuSheet(
    onDismiss: () -> Unit,
    onSelectOption: (option: String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Attachment", fontWeight = FontWeight.Bold, color = Color(0xFF1B2A5E)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Document, Camera, Gallery
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.InsertDriveFile,
                        label = "Document",
                        color = Color(0xFF5157AE),
                        onClick = { onSelectOption("document") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.CameraAlt,
                        label = "Camera",
                        color = Color(0xFFE91E63),
                        onClick = { onSelectOption("camera") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Image,
                        label = "Gallery",
                        color = Color(0xFFAC44CF),
                        onClick = { onSelectOption("gallery") }
                    )
                }

                // Row 2: Audio, Location, Contact
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    AttachmentOptionItem(
                        icon = Icons.Default.Headphones,
                        label = "Audio",
                        color = Color(0xFFF06292),
                        onClick = { onSelectOption("audio") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.LocationOn,
                        label = "Location",
                        color = Color(0xFF25D366),
                        onClick = { onSelectOption("location") }
                    )
                    AttachmentOptionItem(
                        icon = Icons.Default.Person,
                        label = "Contact",
                        color = Color(0xFF009688),
                        onClick = { onSelectOption("contact") }
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        shape = RoundedCornerShape(24.dp)
    )
}

@Composable
fun AttachmentOptionItem(
    icon: ImageVector,
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(color),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color.White,
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun VoiceNotePlayerBubble(
    message: MessageEntity,
    isMe: Boolean,
    viewModel: ChatViewModel,
    contentColor: Color
) {
    val playingMessageId by viewModel.playingMessageId.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val audioProgress by viewModel.audioProgress.collectAsState()
    val playbackSpeed by viewModel.audioPlaybackSpeed.collectAsState()

    val isThisPlaying = playingMessageId == message.messageId && isPlayingAudio
    val currentProgress = if (playingMessageId == message.messageId) audioProgress else 0f

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
    ) {
        // Play / Pause Icon Button
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isMe) Color.White.copy(alpha = 0.2f) else ChatWaveGreen.copy(alpha = 0.15f))
                .clickable {
                    viewModel.toggleAudioPlayback(
                        message.messageId,
                        message.mediaUrl,
                        message.mediaDuration
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = if (isThisPlaying) "Pause" else "Play",
                tint = if (isMe) Color.White else ChatWaveGreen,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Dynamic Waveform and Duration
        Column(modifier = Modifier.weight(1f)) {
            // Waveform amplitude bars
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .clickable {
                        viewModel.seekAudioTo(0.5f)
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val totalBars = 18
                val filledBars = (currentProgress * totalBars).toInt()
                for (i in 0 until totalBars) {
                    val isFilled = i <= filledBars
                    val barHeight = ((i * 7 + 11) % 18 + 6).dp
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(barHeight)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (isFilled) (if (isMe) Color.White else ChatWaveGreen)
                                else contentColor.copy(alpha = 0.3f)
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDuration(message.mediaDuration.coerceAtLeast(1)),
                    fontSize = 11.sp,
                    color = contentColor.copy(alpha = 0.8f)
                )

                if (playingMessageId == message.messageId) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(contentColor.copy(alpha = 0.15f))
                            .clickable { viewModel.toggleAudioSpeed() }
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${playbackSpeed}x",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = contentColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DocumentMessageBubble(
    message: MessageEntity,
    contentColor: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF5157AE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.InsertDriveFile,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.fileName ?: message.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${message.fileSize ?: "1.8 MB"} • PDF Document",
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }

        Icon(
            imageVector = Icons.Default.FileDownload,
            contentDescription = "Download",
            tint = contentColor.copy(alpha = 0.8f),
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun MediaMessageBubble(
    message: MessageEntity,
    contentColor: Color,
    onImageClick: (String) -> Unit = {}
) {
    val mediaUrl = message.mediaUrl?.split(",")?.firstOrNull() ?: "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=600"

    Column {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black.copy(alpha = 0.1f))
                .clickable { onImageClick(mediaUrl) }
                .testTag("chat_image_thumbnail")
        ) {
            AsyncImage(
                model = mediaUrl,
                contentDescription = "Media attachment",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (message.type == "video") {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .align(Alignment.Center),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            if (message.type == "gallery" && (message.mediaUrl?.split(",")?.size ?: 0) > 1) {
                val count = message.mediaUrl!!.split(",").size
                Box(
                    modifier = Modifier
                        .padding(8.dp)
                        .align(Alignment.TopEnd)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("+$count Photos", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun AudioMessageBubble(
    message: MessageEntity,
    isMe: Boolean,
    viewModel: ChatViewModel,
    contentColor: Color
) {
    val playingMessageId by viewModel.playingMessageId.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val isThisPlaying = playingMessageId == message.messageId && isPlayingAudio

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(contentColor.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(Color(0xFFF06292)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = message.fileName ?: message.text,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${formatDuration(message.mediaDuration)} • ${message.fileSize ?: "3.4 MB"}",
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.7f)
            )
        }

        IconButton(
            onClick = {
                viewModel.toggleAudioPlayback(message.messageId, message.mediaUrl, message.mediaDuration)
            }
        ) {
            Icon(
                imageVector = if (isThisPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = "Play/Pause",
                tint = contentColor
            )
        }
    }
}

@Composable
fun LocationMessageBubble(
    message: MessageEntity,
    contentColor: Color
) {
    val isLive = message.type == "live_location"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF25D366),
                    modifier = Modifier.size(36.dp)
                )
                Text(
                    text = if (isLive) "LIVE LOCATION" else "CURRENT LOCATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
            }
        }

        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = message.fileName ?: "San Francisco, CA, USA",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = "Lat: ${String.format("%.4f", if (message.latitude != 0.0) message.latitude else 37.7749)}, Lng: ${String.format("%.4f", if (message.longitude != 0.0) message.longitude else -122.4194)}",
                fontSize = 11.sp,
                color = contentColor.copy(alpha = 0.7f)
            )

            if (isLive) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF25D366))
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Updating live • Active for 1 hour",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF25D366)
                    )
                }
            }
        }
    }
}

@Composable
fun ContactMessageBubble(
    message: MessageEntity,
    contentColor: Color,
    onMessageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(contentColor.copy(alpha = 0.08f))
            .padding(10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF009688)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = message.contactName ?: message.text.removePrefix("👤 Contact: "),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = contentColor
                )
                Text(
                    text = message.contactPhone ?: "+1 (555) 019-2834",
                    fontSize = 12.sp,
                    color = contentColor.copy(alpha = 0.7f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Divider(color = contentColor.copy(alpha = 0.15f))
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Message Contact",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onMessageClick)
            )
        }
    }
}

// ATTACHMENT DIALOG COMPOSABLES
@Composable
fun DocumentPickerDialog(
    onDismiss: () -> Unit,
    onSend: (fileName: String, fileSize: String) -> Unit
) {
    var fileName by remember { mutableStateOf("Quarterly_Report_2026.pdf") }
    var fileSize by remember { mutableStateOf("2.4 MB") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Document", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = fileName,
                    onValueChange = { fileName = it },
                    label = { Text("Document File Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = fileSize,
                    onValueChange = { fileSize = it },
                    label = { Text("File Size") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(fileName, fileSize) }) {
                Text("Send Document")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun GalleryPickerDialog(
    onDismiss: () -> Unit,
    onSend: (uris: List<String>, caption: String) -> Unit
) {
    var caption by remember { mutableStateOf("") }
    val samplePhotos = listOf(
        "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=600",
        "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=600"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Photos / Videos", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    samplePhotos.forEach { url ->
                        AsyncImage(
                            model = url,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Add a caption...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(samplePhotos, caption) }) {
                Text("Send Media")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun CameraCaptureDialog(
    onDismiss: () -> Unit,
    onSend: (uri: String, isVideo: Boolean, caption: String) -> Unit
) {
    var isVideo by remember { mutableStateOf(false) }
    var caption by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Camera Capture", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isVideo) Icons.Default.Videocam else Icons.Default.CameraAlt,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    FilterChip(
                        selected = !isVideo,
                        onClick = { isVideo = false },
                        label = { Text("Photo") }
                    )
                    FilterChip(
                        selected = isVideo,
                        onClick = { isVideo = true },
                        label = { Text("Video") }
                    )
                }

                OutlinedTextField(
                    value = caption,
                    onValueChange = { caption = it },
                    label = { Text("Caption") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSend("https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=600", isVideo, caption)
            }) {
                Text("Capture & Send")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun AudioPickerDialog(
    onDismiss: () -> Unit,
    onSend: (fileName: String, fileSize: String, duration: Int) -> Unit
) {
    var songName by remember { mutableStateOf("Acoustic_Guitar_Track.mp3") }
    var durationSec by remember { mutableIntStateOf(180) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Audio File", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = songName,
                    onValueChange = { songName = it },
                    label = { Text("Audio Track Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(songName, "4.2 MB", durationSec) }) {
                Text("Send Audio")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun LocationPickerDialog(
    onDismiss: () -> Unit,
    onSend: (lat: Double, lng: Double, address: String, liveDurationMs: Long) -> Unit
) {
    var isLiveLocation by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Share Location", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLiveLocation = false },
                    colors = CardDefaults.cardColors(containerColor = if (!isLiveLocation) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MyLocation, contentDescription = null, tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Send Your Current Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Accurate to 5 meters", fontSize = 11.sp)
                        }
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isLiveLocation = true },
                    colors = CardDefaults.cardColors(containerColor = if (isLiveLocation) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GpsFixed, contentDescription = null, tint = Color(0xFF25D366))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Share Live Location", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Updates in real-time as you move", fontSize = 11.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSend(37.7749, -122.4194, "Market Street, San Francisco, CA", if (isLiveLocation) 3600000L else 0L)
            }) {
                Text(if (isLiveLocation) "Share Live Location" else "Send Location")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun ContactPickerDialog(
    onDismiss: () -> Unit,
    onSend: (name: String, phone: String) -> Unit
) {
    var contactName by remember { mutableStateOf("Alex Morgan") }
    var contactPhone by remember { mutableStateOf("+1 (555) 019-2834") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Contact", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = contactName,
                    onValueChange = { contactName = it },
                    label = { Text("Contact Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactPhone,
                    onValueChange = { contactPhone = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSend(contactName, contactPhone) }) {
                Text("Send Contact Card")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

fun formatTime(timestamp: Long): String {
    val date = Date(timestamp)
    val sdf = SimpleDateFormat("h:mm a", Locale.getDefault())
    return sdf.format(date)
}

fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return String.format(Locale.getDefault(), "%02d:%02d", m, s)
}

@Composable
fun ExportChatDialog(
    viewModel: ChatViewModel,
    chatId: String,
    onDismiss: () -> Unit,
    onExportComplete: (String, String) -> Unit
) {
    val context = LocalContext.current
    var selectedFormat by remember { mutableStateOf("JSON") }
    var isExporting by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Export Chat History", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Convert and download the active conversation history directly to your device as JSON or formatted text.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Text("Select Format:", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedFormat == "JSON",
                        onClick = { selectedFormat = "JSON" },
                        label = { Text("JSON (.json)") },
                        leadingIcon = { if (selectedFormat == "JSON") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) else null },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = selectedFormat == "TXT",
                        onClick = { selectedFormat = "TXT" },
                        label = { Text("Plain Text (.txt)") },
                        leadingIcon = { if (selectedFormat == "TXT") Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) else null },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isExporting = true
                    viewModel.exportChatHistory(context, chatId, selectedFormat) { name, content ->
                        isExporting = false
                        onExportComplete(name, content)
                    }
                },
                enabled = !isExporting,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
            ) {
                if (isExporting) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Exporting...")
                } else {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export & Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportResultDialog(
    fileName: String,
    fileContent: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Chat Exported!", fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text("Saved to Downloads folder:", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(fileName, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Text("File Content Preview:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 180.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        text = fileContent,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        color = Color(0xFF38BDF8)
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Done")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("Exported Chat", fileContent)
                    clipboard.setPrimaryClip(clip)
                    android.widget.Toast.makeText(context, "Copied content to clipboard!", android.widget.Toast.LENGTH_SHORT).show()
                }
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy Text")
            }
        }
    )
}

@Composable
fun ScheduleMessageDialog(
    viewModel: ChatViewModel,
    currentInputText: String,
    chatId: String,
    onDismiss: () -> Unit,
    onScheduled: () -> Unit
) {
    val context = LocalContext.current
    var textInput by remember { mutableStateOf(currentInputText) }
    var selectedPresetMillis by remember { mutableStateOf(10_000L) }
    var selectedPresetLabel by remember { mutableStateOf("In 10 Seconds (Quick Test)") }

    val scheduledList by viewModel.scheduledMessages.collectAsState()
    val chatScheduled = scheduledList.filter { it.chatId == chatId }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF3B82F6))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Schedule Message", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = textInput,
                    onValueChange = { textInput = it },
                    label = { Text("Message Content") },
                    placeholder = { Text("Type message to schedule...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                Text("Pick Delivery Time:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)

                val presets = listOf(
                    "In 10 Seconds (Quick Test)" to 10_000L,
                    "In 1 Minute" to 60_000L,
                    "In 1 Hour" to 3_600_000L,
                    "Tomorrow at 9 AM" to 86_400_000L
                )

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    presets.forEach { (label, millis) ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selectedPresetMillis == millis) Color(0xFF3B82F6).copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (selectedPresetMillis == millis) BorderStroke(1.dp, Color(0xFF3B82F6)) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedPresetMillis = millis
                                    selectedPresetLabel = label
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedPresetMillis == millis,
                                    onClick = {
                                        selectedPresetMillis = millis
                                        selectedPresetLabel = label
                                    }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(label, fontSize = 13.sp, fontWeight = if (selectedPresetMillis == millis) FontWeight.Bold else FontWeight.Normal)
                            }
                        }
                    }
                }

                if (chatScheduled.isNotEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)))
                    Text("Pending Scheduled Messages (${chatScheduled.size}):", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFF59E0B))

                    chatScheduled.forEach { scheduled ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(scheduled.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                    Text("🕒 Delivery: ${scheduled.displayFormattedTime}", fontSize = 11.sp, color = Color(0xFF3B82F6))
                                }
                                IconButton(onClick = { viewModel.cancelScheduledMessage(scheduled.id) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Cancel", tint = Color.Red, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (textInput.trim().isEmpty()) {
                        android.widget.Toast.makeText(context, "Please enter message text", android.widget.Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    viewModel.scheduleTextMessage(chatId, textInput.trim(), selectedPresetMillis, selectedPresetLabel)
                    android.widget.Toast.makeText(context, "Message scheduled: $selectedPresetLabel", android.widget.Toast.LENGTH_LONG).show()
                    onScheduled()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Schedule Delivery")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun LightboxImageViewerDialog(
    imageUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var scale by remember { mutableFloatStateOf(1f) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.92f))
                .testTag("lightbox_container")
        ) {
            // Main Zoomable Image
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            if (scale > 1f) {
                                offsetX += pan.x
                                offsetY += pan.y
                            } else {
                                offsetX = 0f
                                offsetY = 0f
                            }
                        }
                    }
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onDoubleTap = {
                                if (scale > 1f) {
                                    scale = 1f
                                    offsetX = 0f
                                    offsetY = 0f
                                } else {
                                    scale = 2.5f
                                }
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Full Screen Lightbox",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offsetX,
                            translationY = offsetY
                        )
                        .testTag("lightbox_image")
                )
            }

            // Top Control Bar Overlay
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(16.dp)
                    .align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(Color.Black.copy(alpha = 0.5f))
                        .testTag("close_lightbox_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Lightbox",
                        tint = Color.White
                    )
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text(
                        text = "${(scale * 100).toInt()}% • Double tap / Pinch to Zoom",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (scale > 1f) {
                        IconButton(
                            onClick = {
                                scale = 1f
                                offsetX = 0f
                                offsetY = 0f
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.5f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.ZoomOutMap,
                                contentDescription = "Reset Zoom",
                                tint = Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Image Link", imageUrl)
                            clipboard.setPrimaryClip(clip)
                            android.widget.Toast.makeText(context, "Image URL copied to clipboard", android.widget.Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.5f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Share Image",
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SwipeToReplyWrapper(
    onReply: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var offsetX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val maxSwipePx = with(density) { 80.dp.toPx() }
    val replyThresholdPx = with(density) { 45.dp.toPx() }

    Box(
        modifier = modifier.fillMaxWidth()
    ) {
        // Slide / Swipe Reply indicator icon on the left
        if (offsetX > 6f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                val progress = (offsetX / replyThresholdPx).coerceIn(0f, 1f)
                Surface(
                    shape = CircleShape,
                    color = if (offsetX >= replyThresholdPx) Color(0xFF2563EB) else Color(0xFF94A3B8),
                    modifier = Modifier.size((28 * progress + 10).dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Reply,
                            contentDescription = "Swipe to Reply",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX >= replyThresholdPx) {
                                onReply()
                            }
                            coroutineScope.launch {
                                Animatable(offsetX).animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring()
                                ) {
                                    offsetX = value
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                Animatable(offsetX).animateTo(0f) {
                                    offsetX = value
                                }
                            }
                        },
                        onHorizontalDrag = { _, dragAmount ->
                            val newOffset = offsetX + dragAmount
                            offsetX = newOffset.coerceIn(0f, maxSwipePx)
                        }
                    )
                }
        ) {
            content()
        }
    }
}
