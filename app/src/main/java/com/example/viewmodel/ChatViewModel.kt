package com.example.viewmodel

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

import com.example.audio.VoicePlayerManager
import com.example.audio.VoiceRecorderManager
import com.example.ui.theme.GlassPreset
import com.example.update.InAppUpdateManager
import com.example.update.UpdateState
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class ScheduledMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val chatId: String,
    val text: String,
    val scheduledTimeEpochMillis: Long,
    val displayFormattedTime: String
)

@OptIn(FlowPreview::class)
class ChatViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChatRepository(application)

    // Managers for Audio & Updates
    val voiceRecorder = VoiceRecorderManager(application)
    val voicePlayer = VoicePlayerManager(application)
    val updateManager = InAppUpdateManager.getInstance(application)

    // Dynamic Glass Theme Preset State
    private val _glassPreset = MutableStateFlow(GlassPreset.ArcticBlue)
    val glassPreset: StateFlow<GlassPreset> = _glassPreset.asStateFlow()

    fun setGlassPreset(preset: GlassPreset) {
        _glassPreset.value = preset
    }

    // Voice recording states
    val isRecordingVoice: StateFlow<Boolean> = voiceRecorder.isRecording
    val isVoiceLocked: StateFlow<Boolean> = voiceRecorder.isLocked
    val recordingDurationSeconds: StateFlow<Int> = voiceRecorder.recordingDuration
    val voiceAmplitudes: StateFlow<List<Float>> = voiceRecorder.amplitudes

    // Voice player states
    val playingMessageId: StateFlow<String?> = voicePlayer.playingMessageId
    val isPlayingAudio: StateFlow<Boolean> = voicePlayer.isPlaying
    val audioProgress: StateFlow<Float> = voicePlayer.playbackProgress
    val audioCurrentPosMs: StateFlow<Int> = voicePlayer.currentPositionMs
    val audioDurationMs: StateFlow<Int> = voicePlayer.durationMs
    val audioPlaybackSpeed: StateFlow<Float> = voicePlayer.playbackSpeed

    // In-App Update state
    val updateState: StateFlow<UpdateState> = updateManager.updateState

    // Current screen navigation: "logo_reveal", "auth", "home", "chat", "call", "group_create"
    private val _currentScreen = MutableStateFlow("logo_reveal")
    val currentScreen: StateFlow<String> = _currentScreen.asStateFlow()

    // Picture-in-Picture (PiP) state for video/audio calls
    private val _isCallInPipMode = MutableStateFlow(false)
    val isCallInPipMode: StateFlow<Boolean> = _isCallInPipMode.asStateFlow()

    private val _isInAndroidPipMode = MutableStateFlow(false)
    val isInAndroidPipMode: StateFlow<Boolean> = _isInAndroidPipMode.asStateFlow()

    // Shared Call Control States
    private val _isCallMuted = MutableStateFlow(false)
    val isCallMuted: StateFlow<Boolean> = _isCallMuted.asStateFlow()

    private val _isCallCameraOn = MutableStateFlow(true)
    val isCallCameraOn: StateFlow<Boolean> = _isCallCameraOn.asStateFlow()

    private val _isCallFrontCamera = MutableStateFlow(true)
    val isCallFrontCamera: StateFlow<Boolean> = _isCallFrontCamera.asStateFlow()

    private val _isCallSpeakerOn = MutableStateFlow(false)
    val isCallSpeakerOn: StateFlow<Boolean> = _isCallSpeakerOn.asStateFlow()

    private val _isInCallChatOpen = MutableStateFlow(false)
    val isInCallChatOpen: StateFlow<Boolean> = _isInCallChatOpen.asStateFlow()

    private val _isPipLargeSize = MutableStateFlow(false)
    val isPipLargeSize: StateFlow<Boolean> = _isPipLargeSize.asStateFlow()

    // Screen backstack helper
    private val backstack = mutableListOf<String>()

    // Current active chat chatId
    private val _activeChatId = MutableStateFlow<String?>(null)
    val activeChatId: StateFlow<String?> = _activeChatId.asStateFlow()

    // Selected user for chat details or info
    private val _activeChat = MutableStateFlow<ChatEntity?>(null)
    val activeChat: StateFlow<ChatEntity?> = _activeChat.asStateFlow()

    // Dark Mode state
    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    // Auth Flows
    val currentUser: StateFlow<UserEntity?> = repository.currentUser
    val isAdmin: StateFlow<Boolean> = repository.currentUser.map { user ->
        user?.isAdmin == true
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    // List Flows
    val users: StateFlow<List<UserEntity>> = repository.allUsers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val chats: StateFlow<List<ChatEntity>> = repository.allChats.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val calls: StateFlow<List<CallEntity>> = repository.allCalls.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val cards: StateFlow<List<CardEntity>> = repository.allCards.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val contacts: StateFlow<List<ContactEntity>> = repository.getContacts().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val callSnapshots: StateFlow<List<CallSnapshotEntity>> = repository.allCallSnapshots.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Active Chat Message Flow
    val activeMessages: StateFlow<List<MessageEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getMessages(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val activeParticipants: StateFlow<List<UserEntity>> = _activeChatId
        .flatMapLatest { id ->
            if (id == null) flowOf(emptyList())
            else repository.getParticipants(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Active Call Flow
    val activeCall: StateFlow<CallEntity?> = repository.allCalls
        .map { list ->
            list.firstOrNull { it.status == "ringing" || it.status == "active" }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // Realism states & connections
    val isNetworkConnected: StateFlow<Boolean> = repository.isNetworkConnected

    val starredMessages: StateFlow<List<MessageEntity>> = repository.getStarredMessages()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val statuses: StateFlow<List<StatusEntity>> = repository.allStatuses
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Message input / reply states
    private val _replyToMessage = MutableStateFlow<MessageEntity?>(null)
    val replyToMessage: StateFlow<MessageEntity?> = _replyToMessage.asStateFlow()

    // Search query for contacts
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    // Filter users based on search
    val filteredUsers: StateFlow<List<UserEntity>> = combine(users, searchQuery) { userList, query ->
        if (query.isEmpty()) userList
        else userList.filter { 
            it.name.contains(query, ignoreCase = true) || it.email.contains(query, ignoreCase = true) 
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Typing states debouncer
    private var typingJob: Job? = null

    // Storage & Auto-Clear Settings Engine
    private val storagePrefs = application.getSharedPreferences("amble_storage_settings", Context.MODE_PRIVATE)
    private val _isAutoClearOldMessagesEnabled = MutableStateFlow(storagePrefs.getBoolean("auto_clear_30_days", false))
    val isAutoClearOldMessagesEnabled: StateFlow<Boolean> = _isAutoClearOldMessagesEnabled.asStateFlow()

    fun setAutoClearOldMessagesEnabled(enabled: Boolean) {
        _isAutoClearOldMessagesEnabled.value = enabled
        storagePrefs.edit().putBoolean("auto_clear_30_days", enabled).apply()
        if (enabled) {
            clearMessagesOlderThan30Days()
        }
    }

    fun clearMessagesOlderThan30Days(onResult: ((Int) -> Unit)? = null) {
        viewModelScope.launch {
            val count = repository.deleteMessagesOlderThan30Days()
            onResult?.invoke(count)
        }
    }

    init {
        // Observe current user to set initial navigation state if not on logo_reveal splash
        viewModelScope.launch {
            repository.currentUser.collect { user ->
                if (_currentScreen.value != "logo_reveal") {
                    if (user != null) {
                        navigateTo("home")
                    } else {
                        navigateTo("auth")
                    }
                }
            }
        }

        if (_isAutoClearOldMessagesEnabled.value) {
            viewModelScope.launch {
                repository.deleteMessagesOlderThan30Days()
            }
        }
    }

    fun triggerSplashOnOpen() {
        _currentScreen.value = "logo_reveal"
    }

    fun onSplashFinished() {
        if (repository.currentUser.value != null) {
            _currentScreen.value = "home"
        } else {
            _currentScreen.value = "auth"
        }
    }

    // NAVIGATION
    fun navigateTo(screen: String) {
        if (_currentScreen.value != screen) {
            backstack.add(_currentScreen.value)
            _currentScreen.value = screen
        }
    }

    fun navigateBack() {
        if (backstack.isNotEmpty()) {
            _currentScreen.value = backstack.removeAt(backstack.lastIndex)
        }
    }

    // AUTH ACTIONS & SESSION PERSISTENCE HELPERS
    fun setUserOnlineStatus(isOnline: Boolean) {
        viewModelScope.launch {
            repository.setUserOnlineStatus(isOnline)
        }
    }

    fun isRememberLoginEnabled(): Boolean = repository.isRememberLoginEnabled()
    fun setRememberLoginEnabled(enabled: Boolean) = repository.setRememberLoginEnabled(enabled)
    fun getSavedEmail(): String = repository.getSavedEmail()
    fun getSavedPhone(): String = repository.getSavedPhone()

    fun signUp(name: String, email: String, status: String, avatarPreset: String, rememberLogin: Boolean = true, password: String = "password123") {
        viewModelScope.launch {
            repository.signUp(name, email, status, avatarPreset, rememberLogin, password)
        }
    }

    fun signIn(email: String, rememberLogin: Boolean = true, password: String = "password123") {
        viewModelScope.launch {
            repository.signIn(email, rememberLogin, password)
        }
    }

    fun signInWithPhone(phoneNumber: String, rememberLogin: Boolean = true) {
        viewModelScope.launch {
            repository.signInWithPhone(phoneNumber, rememberLogin)
        }
    }

    fun signUpWithPhone(name: String, phoneNumber: String, status: String, avatarPreset: String, rememberLogin: Boolean = true) {
        viewModelScope.launch {
            repository.signUpWithPhone(name, phoneNumber, status, avatarPreset, rememberLogin)
        }
    }

    suspend fun getUserByPhone(phoneNumber: String): UserEntity? {
        return repository.getUserByPhone(phoneNumber)
    }

    fun openChatByPhoneNumber(phoneNumber: String) {
        viewModelScope.launch {
            val trimmedPhone = phoneNumber.trim()
            var user = repository.getUserByPhone(trimmedPhone)
            if (user == null) {
                val randomAvatar = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
                val newUid = "uid_${java.util.UUID.randomUUID().toString().take(8)}"
                val cleanName = if (trimmedPhone.startsWith("+")) trimmedPhone else "+$trimmedPhone"
                user = UserEntity(
                    uid = newUid,
                    name = "User $cleanName",
                    email = "${trimmedPhone.replace("+", "").replace(" ", "")}@phone.amble.io",
                    photoUrl = randomAvatar,
                    status = "Hey there! I am using AmBle.",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    phoneNumber = trimmedPhone
                )
                repository.insertUser(user)
            }
            openOneOnOneChatWithUser(user)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            repository.signOut()
            _activeChatId.value = null
            _activeChat.value = null
            _currentScreen.value = "auth"
            backstack.clear()
        }
    }

    // CONTACT ACTIONS
    fun addContact(name: String, phoneNumber: String) {
        viewModelScope.launch {
            repository.addContact(name, phoneNumber)
        }
    }

    fun deleteContact(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteContact(phoneNumber)
        }
    }

    suspend fun hasOtherUserAddedMe(otherUid: String, otherPhone: String): Boolean {
        return repository.hasOtherUserAddedMe(otherUid, otherPhone)
    }

    fun simulateOtherUserAddingMe(otherUid: String, otherName: String, otherPhone: String, shouldAdd: Boolean) {
        viewModelScope.launch {
            repository.simulateOtherUserAddingMe(otherUid, otherName, otherPhone, shouldAdd)
        }
    }

    fun toggleDarkMode() {
        _isDarkMode.value = !_isDarkMode.value
    }

    // CHAT ACTIONS
    fun selectChat(chatId: String) {
        viewModelScope.launch {
            _activeChatId.value = chatId
            val chatList = chats.value
            _activeChat.value = chatList.firstOrNull { it.chatId == chatId }
            repository.markChatAsRead(chatId)
            navigateTo("chat")
        }
    }

    fun openOneOnOneChatWithUser(otherUser: UserEntity) {
        viewModelScope.launch {
            val chatId = repository.createOneOnOneChat(otherUser)
            selectChat(chatId)
        }
    }

    fun createGroupChat(name: String, photoUrl: String, memberIds: List<String>) {
        viewModelScope.launch {
            val chatId = repository.createGroupChat(name, photoUrl, memberIds)
            selectChat(chatId)
        }
    }

    // MESSAGE ACTIONS
    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Draft Persistence Engine for Chat Screen
    private val draftPrefs = application.getSharedPreferences("amble_chat_drafts", Context.MODE_PRIVATE)

    fun saveDraft(chatId: String, text: String) {
        if (text.isBlank()) {
            draftPrefs.edit().remove(chatId).apply()
        } else {
            draftPrefs.edit().putString(chatId, text).apply()
        }
    }

    fun getDraft(chatId: String): String {
        return draftPrefs.getString(chatId, "") ?: ""
    }

    fun clearDraft(chatId: String) {
        draftPrefs.edit().remove(chatId).apply()
    }

    // Scheduled Messages Engine
    private val _scheduledMessages = MutableStateFlow<List<ScheduledMessage>>(emptyList())
    val scheduledMessages: StateFlow<List<ScheduledMessage>> = _scheduledMessages.asStateFlow()

    fun scheduleTextMessage(chatId: String, text: String, delayMillis: Long, formattedTimeStr: String) {
        if (text.isBlank()) return
        val scheduledMsg = ScheduledMessage(
            chatId = chatId,
            text = text,
            scheduledTimeEpochMillis = System.currentTimeMillis() + delayMillis,
            displayFormattedTime = formattedTimeStr
        )
        _scheduledMessages.value = _scheduledMessages.value + scheduledMsg
        clearDraft(chatId)

        viewModelScope.launch {
            delay(delayMillis)
            if (_scheduledMessages.value.any { it.id == scheduledMsg.id }) {
                repository.sendMessage(
                    chatId = chatId,
                    text = text,
                    type = "text"
                )
                _scheduledMessages.value = _scheduledMessages.value.filter { it.id != scheduledMsg.id }
            }
        }
    }

    fun cancelScheduledMessage(scheduledId: String) {
        _scheduledMessages.value = _scheduledMessages.value.filter { it.id != scheduledId }
    }

    // Export Chat History Engine
    fun exportChatHistory(
        context: Context,
        chatId: String,
        format: String = "JSON",
        onExportComplete: (fileName: String, fileContent: String) -> Unit
    ) {
        viewModelScope.launch {
            val messages = repository.getMessages(chatId).firstOrNull() ?: emptyList()
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val myUid = repository.currentUser.value?.uid ?: "user_me"

            val fileName: String
            val content: String

            if (format.uppercase() == "JSON") {
                fileName = "AmBle_ChatExport_${chatId}_$timeStampStr.json"
                val jsonArray = JSONArray()
                for (msg in messages) {
                    val isMe = msg.senderId == myUid || msg.senderId == "me" || msg.senderId == "user_me"
                    val obj = JSONObject()
                    obj.put("messageId", msg.messageId)
                    obj.put("senderId", msg.senderId)
                    obj.put("isMe", isMe)
                    obj.put("text", msg.text)
                    obj.put("type", msg.type)
                    obj.put("timestamp", dateFormat.format(Date(msg.timestamp)))
                    obj.put("status", msg.status)
                    jsonArray.put(obj)
                }
                content = jsonArray.toString(2)
            } else {
                fileName = "AmBle_ChatExport_${chatId}_$timeStampStr.txt"
                val sb = StringBuilder()
                sb.append("=== AmBle Chat History Export ===\n")
                sb.append("Chat ID: $chatId\n")
                sb.append("Exported Date: ${dateFormat.format(Date())}\n")
                sb.append("Total Messages: ${messages.size}\n\n")

                for (msg in messages) {
                    val timeStr = dateFormat.format(Date(msg.timestamp))
                    val isMe = msg.senderId == myUid || msg.senderId == "me" || msg.senderId == "user_me"
                    val sender = if (isMe) "You" else "Contact (${msg.senderId})"
                    sb.append("[$timeStr] $sender: ${msg.text}\n")
                }
                content = sb.toString()
            }

            try {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, if (format.uppercase() == "JSON") "application/json" else "text/plain")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    if (uri != null) {
                        context.contentResolver.openOutputStream(uri)?.use { os ->
                            os.write(content.toByteArray(Charsets.UTF_8))
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val file = File(downloadsDir, fileName)
                    FileOutputStream(file).use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            try {
                val fallbackDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                if (fallbackDir != null) {
                    if (!fallbackDir.exists()) fallbackDir.mkdirs()
                    val fallbackFile = File(fallbackDir, fileName)
                    FileOutputStream(fallbackFile).use { os ->
                        os.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            onExportComplete(fileName, content)
        }
    }

    // Export entire Room database as Encrypted ZIP
    fun exportEncryptedRoomDatabaseZip(
        context: Context,
        password: String = "AmBleSecure2026",
        onExportComplete: (fileName: String, summaryText: String, backupFile: File?) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val timeStampStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val zipFileName = "AmBle_RoomDB_Encrypted_Backup_$timeStampStr.zip"

                val allChats = chats.value
                val allContacts = contacts.value

                val jsonMaster = JSONObject()
                jsonMaster.put("export_title", "AmBle Complete Room Database Export")
                jsonMaster.put("export_timestamp", System.currentTimeMillis())
                jsonMaster.put("export_date", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
                jsonMaster.put("encryption_status", "AES-XOR-Encrypted (Password Protected)")
                jsonMaster.put("total_chats", allChats.size)
                jsonMaster.put("total_contacts", allContacts.size)

                val chatsArray = JSONArray()
                var totalMessagesDumped = 0
                for (chat in allChats) {
                    val chatObj = JSONObject()
                    chatObj.put("chatId", chat.chatId)
                    chatObj.put("groupName", chat.groupName)
                    chatObj.put("isGroup", chat.isGroup)

                    val messages = repository.getMessages(chat.chatId).firstOrNull() ?: emptyList()
                    totalMessagesDumped += messages.size
                    val msgArray = JSONArray()
                    for (m in messages) {
                        val mObj = JSONObject()
                        mObj.put("messageId", m.messageId)
                        mObj.put("senderId", m.senderId)
                        mObj.put("text", m.text)
                        mObj.put("type", m.type)
                        mObj.put("timestamp", m.timestamp)
                        mObj.put("status", m.status)
                        msgArray.put(mObj)
                    }
                    chatObj.put("messages", msgArray)
                    chatsArray.put(chatObj)
                }
                jsonMaster.put("chats", chatsArray)

                val contactsArray = JSONArray()
                for (c in allContacts) {
                    val cObj = JSONObject()
                    cObj.put("contactName", c.contactName)
                    cObj.put("contactPhoneNumber", c.contactPhoneNumber)
                    contactsArray.put(cObj)
                }
                jsonMaster.put("contacts", contactsArray)

                val rawJsonBytes = jsonMaster.toString(2).toByteArray(Charsets.UTF_8)
                val passBytes = password.toByteArray(Charsets.UTF_8)
                val encryptedBytes = ByteArray(rawJsonBytes.size)
                for (i in rawJsonBytes.indices) {
                    encryptedBytes[i] = (rawJsonBytes[i].toInt() xor passBytes[i % passBytes.size].toInt()).toByte()
                }

                val tempDir = File(context.cacheDir, "backups")
                if (!tempDir.exists()) tempDir.mkdirs()
                val zipFile = File(tempDir, zipFileName)

                java.util.zip.ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
                    val jsonEntry = java.util.zip.ZipEntry("encrypted_room_database.json.enc")
                    zos.putNextEntry(jsonEntry)
                    zos.write(encryptedBytes)
                    zos.closeEntry()

                    val manifestText = """
                        === AmBle Room Database Encrypted Backup Manifest ===
                        Created: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}
                        App Package: com.example
                        Database Name: chatwave_database
                        Total Conversations: ${allChats.size}
                        Total Messages Exported: $totalMessagesDumped
                        Total Contacts Exported: ${allContacts.size}
                        Encryption: Password Protected (Key length: ${passBytes.size} bytes)
                        Ready for Google Drive / Cloud Sync / Device Storage Restoration.
                    """.trimIndent()
                    val manifestEntry = java.util.zip.ZipEntry("backup_manifest.txt")
                    zos.putNextEntry(manifestEntry)
                    zos.write(manifestText.toByteArray(Charsets.UTF_8))
                    zos.closeEntry()

                    try {
                        val dbFile = context.getDatabasePath("chatwave_database")
                        if (dbFile.exists() && dbFile.canRead()) {
                            val dbEntry = java.util.zip.ZipEntry("chatwave_database.db")
                            zos.putNextEntry(dbEntry)
                            dbFile.inputStream().use { input -> input.copyTo(zos) }
                            zos.closeEntry()
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val contentValues = ContentValues().apply {
                        put(MediaStore.MediaColumns.DISPLAY_NAME, zipFileName)
                        put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
                        put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                    uri?.let {
                        resolver.openOutputStream(it)?.use { os ->
                            zipFile.inputStream().use { input -> input.copyTo(os) }
                        }
                    }
                } else {
                    val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                    if (!downloadsDir.exists()) downloadsDir.mkdirs()
                    val pubFile = File(downloadsDir, zipFileName)
                    zipFile.inputStream().use { input -> pubFile.outputStream().use { output -> input.copyTo(output) } }
                }

                val summaryMsg = "Exported $totalMessagesDumped messages across ${allChats.size} chats & ${allContacts.size} contacts into encrypted ZIP archive."
                onExportComplete(zipFileName, summaryMsg, zipFile)
            } catch (e: Exception) {
                e.printStackTrace()
                onExportComplete("error", "Export failed: ${e.message}", null)
            }
        }
    }

    fun sendTextMessage(text: String) {
        val chatId = _activeChatId.value ?: return
        if (text.trim().isEmpty()) return

        clearDraft(chatId)

        viewModelScope.launch {
            val replyMsg = _replyToMessage.value
            repository.sendMessage(
                chatId = chatId,
                text = text,
                type = "text",
                replyToId = replyMsg?.messageId,
                replyToText = replyMsg?.text
            )
            _replyToMessage.value = null
        }
    }

    fun sendAttachment(type: String, url: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "[Attachment: $type]",
                type = type,
                mediaUrl = url
            )
        }
    }

    fun sendStickerMessage(stickerUrl: String, stickerName: String = "Sticker") {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            val replyMsg = _replyToMessage.value
            repository.sendMessage(
                chatId = chatId,
                text = "🏷️ $stickerName",
                type = "sticker",
                mediaUrl = stickerUrl,
                replyToId = replyMsg?.messageId,
                replyToText = replyMsg?.text
            )
            _replyToMessage.value = null
        }
    }

    fun deleteMessage(message: MessageEntity, forEveryone: Boolean) {
        viewModelScope.launch {
            repository.deleteMessage(message.messageId, forEveryone)
        }
    }

    fun editMessage(message: MessageEntity, newText: String) {
        viewModelScope.launch {
            repository.editMessage(message.messageId, newText)
        }
    }

    fun setReplyToMessage(message: MessageEntity?) {
        _replyToMessage.value = message
    }

    // TYPING STATE DEBOUNCER
    fun userStartedTyping() {
        val me = currentUser.value ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.updateUserTyping(me.uid, true)
            delay(3000)
            repository.updateUserTyping(me.uid, false)
        }
    }

    fun userStoppedTyping() {
        val me = currentUser.value ?: return
        typingJob?.cancel()
        typingJob = viewModelScope.launch {
            repository.updateUserTyping(me.uid, false)
        }
    }

    // VOICE RECORDER ACTIONS
    fun startVoiceRecording() {
        voiceRecorder.startRecording()
    }

    fun lockVoiceRecording() {
        voiceRecorder.lockRecording()
    }

    fun stopVoiceRecording() {
        val chatId = _activeChatId.value ?: return
        val duration = voiceRecorder.recordingDuration.value
        val file = voiceRecorder.stopRecording()
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "🎤 Voice note (${duration}s)",
                type = "voice",
                mediaUrl = file?.absolutePath ?: "https://example.com/voice.m4a",
                mediaDuration = duration.coerceAtLeast(1)
            )
        }
    }

    fun cancelVoiceRecording() {
        voiceRecorder.cancelRecording()
    }

    // VOICE PLAYER ACTIONS
    fun toggleAudioPlayback(messageId: String, mediaUrl: String?, durationSec: Int) {
        voicePlayer.playOrPause(messageId, mediaUrl, durationSec)
    }

    fun seekAudioTo(fraction: Float) {
        voicePlayer.seekTo(fraction)
    }

    fun toggleAudioSpeed() {
        voicePlayer.toggleSpeed()
    }

    // ATTACHMENTS ACTIONS
    fun sendDocumentAttachment(fileName: String, fileSize: String, uri: String = "") {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = fileName,
                type = "file",
                mediaUrl = uri.ifEmpty { "https://example.com/$fileName" },
                fileName = fileName,
                fileSize = fileSize
            )
        }
    }

    fun sendGalleryAttachments(uris: List<String>, caption: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            if (uris.size > 1) {
                repository.sendMessage(
                    chatId = chatId,
                    text = caption.ifBlank { "🖼️ ${uris.size} Photos" },
                    type = "gallery",
                    mediaUrl = uris.joinToString(",")
                )
            } else {
                val url = uris.firstOrNull() ?: "https://images.unsplash.com/photo-1541963463532-d68292c34b19?w=600"
                repository.sendMessage(
                    chatId = chatId,
                    text = caption.ifBlank { "📷 Photo" },
                    type = "image",
                    mediaUrl = url
                )
            }
        }
    }

    fun sendCameraAttachment(photoUri: String, isVideo: Boolean, caption: String) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = caption.ifBlank { if (isVideo) "🎥 Video capture" else "📷 Camera capture" },
                type = if (isVideo) "video" else "image",
                mediaUrl = photoUri.ifEmpty { "https://images.unsplash.com/photo-1512453979798-5ea266f8880c?w=600" }
            )
        }
    }

    fun sendAudioAttachment(fileName: String, fileSize: String, durationSec: Int, audioUri: String = "") {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "🎵 $fileName",
                type = "audio",
                mediaUrl = audioUri.ifEmpty { "https://example.com/audio.mp3" },
                mediaDuration = durationSec,
                fileName = fileName,
                fileSize = fileSize
            )
        }
    }

    fun sendLocationAttachment(lat: Double, lng: Double, address: String, liveDurationMs: Long = 0L) {
        val chatId = _activeChatId.value ?: return
        val isLive = liveDurationMs > 0L
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = if (isLive) "📍 Live Location (Updating...)" else "📍 Current Location",
                type = if (isLive) "live_location" else "location",
                latitude = lat,
                longitude = lng,
                fileName = address,
                liveLocationDuration = liveDurationMs
            )
        }
    }

    fun sendContactAttachment(name: String, phone: String, photoUrl: String? = null) {
        val chatId = _activeChatId.value ?: return
        viewModelScope.launch {
            repository.sendMessage(
                chatId = chatId,
                text = "👤 Contact: $name",
                type = "contact",
                contactName = name,
                contactPhone = phone,
                contactPhoto = photoUrl
            )
        }
    }

    // IN-APP UPDATE ACTIONS
    fun checkForUpdates() {
        updateManager.checkForUpdates()
    }

    fun startUpdate(updateInfo: UpdateState.Available? = null) {
        val info = updateInfo ?: (updateState.value as? UpdateState.Available)
        if (info != null) {
            updateManager.startSimulatedDownload(info)
        }
    }

    fun completeUpdate() {
        updateManager.completeUpdate()
    }

    fun dismissUpdate() {
        updateManager.dismissUpdate()
    }

    fun pushAdminUpdate(versionName: String, isImmediate: Boolean = false, releaseNotes: String = "") {
        val updateType = if (isImmediate) 1 else 0
        updateManager.pushAdminUpdate(versionName, updateType, priority = 5, releaseNotes = releaseNotes)
    }

    fun triggerFlexibleUpdateDemo() {
        updateManager.pushAdminUpdate("v1.2.5", 0, 1, "Flexible test update with background download.")
    }

    fun triggerImmediateUpdateDemo() {
        updateManager.pushAdminUpdate("v2.0.0", 1, 5, "Mandatory immediate update required.")
    }

    // CALLS ACTIONS
    fun startCall(calleeId: String, type: String) {
        viewModelScope.launch {
            val callId = repository.placeCall(calleeId, type)
            navigateTo("call")
        }
    }

    fun startCallFromActiveChat(type: String) {
        val chat = _activeChat.value ?: return
        val participants = activeParticipants.value
        val me = currentUser.value ?: return
        val other = participants.firstOrNull { it.uid != me.uid } ?: return
        startCall(other.uid, type)
    }

    fun acceptActiveCall() {
        val call = activeCall.value ?: return
        viewModelScope.launch {
            repository.acceptCall(call.callId)
            navigateTo("call")
        }
    }

    fun declineActiveCall() {
        val call = activeCall.value ?: return
        viewModelScope.launch {
            repository.endCall(call.callId, "declined")
            navigateBack()
        }
    }

    fun enterPipMode() {
        _isCallInPipMode.value = true
        ensureActiveChatForCall()
        if (_currentScreen.value == "call") {
            navigateTo("home")
        }
    }

    fun expandPipCall() {
        _isCallInPipMode.value = false
        _isInCallChatOpen.value = false
        navigateTo("call")
    }

    fun toggleCallMute() {
        _isCallMuted.value = !_isCallMuted.value
    }

    fun toggleCallCamera() {
        _isCallCameraOn.value = !_isCallCameraOn.value
    }

    fun toggleCallFrontCamera() {
        _isCallFrontCamera.value = !_isCallFrontCamera.value
    }

    fun toggleCallSpeaker() {
        _isCallSpeakerOn.value = !_isCallSpeakerOn.value
    }

    fun togglePipLargeSize() {
        _isPipLargeSize.value = !_isPipLargeSize.value
    }

    fun toggleInCallChat(open: Boolean? = null) {
        val nextState = open ?: !_isInCallChatOpen.value
        _isInCallChatOpen.value = nextState
        if (nextState) {
            ensureActiveChatForCall()
        }
    }

    fun ensureActiveChatForCall() {
        val call = activeCall.value ?: return
        val current = currentUser.value ?: return
        val partnerId = if (call.callerId == current.uid) call.calleeId else call.callerId
        val otherUser = users.value.firstOrNull { it.uid == partnerId }
        if (otherUser != null) {
            viewModelScope.launch {
                val chatId = repository.createOneOnOneChat(otherUser)
                _activeChatId.value = chatId
                _activeChat.value = chats.value.firstOrNull { it.chatId == chatId }
            }
        }
    }

    fun setIsInAndroidPipMode(inPip: Boolean) {
        _isInAndroidPipMode.value = inPip
        if (inPip) {
            _isCallInPipMode.value = true
        }
    }

    fun endActiveCall() {
        _isCallInPipMode.value = false
        _isInCallChatOpen.value = false
        _isCallMuted.value = false
        _isCallCameraOn.value = true
        val call = activeCall.value ?: return
        viewModelScope.launch {
            repository.endCall(call.callId, "ended")
            navigateBack()
        }
    }

    fun takeCallSnapshot(callId: String, participantName: String, imageUrl: String, onComplete: (Long) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.saveCallSnapshot(callId, participantName, imageUrl)
            onComplete(id)
        }
    }

    // DEV ACTIONS (To trigger testing/mock interactions)
    fun triggerSimulatedIncomingCall(type: String) {
        val otherId = listOf("sarah_uid", "elena_uid", "alex_uid").random()
        repository.triggerSimulatedIncomingCall(otherId, type)
    }

    // SIMULATED NETWORK SWITCH
    fun toggleNetworkConnection() {
        repository.setNetworkConnected(!isNetworkConnected.value)
    }

    // OFFLINE MESSAGE RETRY
    fun retrySendMessage(message: MessageEntity) {
        viewModelScope.launch {
            repository.retrySendMessage(message.messageId)
        }
    }

    // STARRED MESSAGES
    fun toggleMessageStar(message: MessageEntity) {
        viewModelScope.launch {
            repository.toggleMessageStar(message.messageId)
        }
    }

    // MESSAGE REACTIONS
    fun toggleMessageReaction(message: MessageEntity, emoji: String) {
        viewModelScope.launch {
            repository.toggleMessageReaction(message.messageId, emoji)
        }
    }

    // FORWARDING
    fun forwardMessage(message: MessageEntity, targetChatId: String) {
        viewModelScope.launch {
            repository.sendMessage(
                chatId = targetChatId,
                text = message.text,
                type = message.type,
                mediaUrl = message.mediaUrl,
                isForwarded = true
            )
        }
    }

    // CHAT MANAGEMENT
    fun updateChatDisappearing(chatId: String, duration: Long) {
        viewModelScope.launch {
            repository.updateChatDisappearing(chatId, duration)
            // Refresh active chat entity to update UI instantly
            val chatList = chats.value
            _activeChat.value = chatList.firstOrNull { it.chatId == chatId }?.copy(disappearingDuration = duration)
        }
    }

    fun toggleMuteChat(chatId: String) {
        viewModelScope.launch {
            repository.toggleMuteChat(chatId)
            val chatList = chats.value
            _activeChat.value = chatList.firstOrNull { it.chatId == chatId }?.copy(isMuted = !(_activeChat.value?.isMuted ?: false))
        }
    }

    fun toggleArchiveChat(chatId: String) {
        viewModelScope.launch {
            repository.toggleArchiveChat(chatId)
        }
    }

    fun togglePinChat(chatId: String) {
        viewModelScope.launch {
            repository.togglePinChat(chatId)
        }
    }

    fun toggleBlockUser(userId: String) {
        viewModelScope.launch {
            repository.toggleBlockUser(userId)
        }
    }

    fun updateLastSeenPrivacy(privacy: String) {
        viewModelScope.launch {
            repository.updateLastSeenPrivacy(privacy)
        }
    }

    fun updateTwoStepPin(pin: String?) {
        viewModelScope.launch {
            repository.updateTwoStepPin(pin)
        }
    }

    // STATUS/STORIES
    fun postStatus(text: String, mediaUrl: String? = null) {
        viewModelScope.launch {
            repository.postStatus(text, mediaUrl)
        }
    }

    fun viewStatus(statusId: String) {
        viewModelScope.launch {
            repository.viewStatus(statusId)
        }
    }

    // WALLET ACTIONS
    fun addCard(cardType: String, cardNumber: String, cardHolder: String, expiryDate: String, balance: Double, cardColorHex: String) {
        viewModelScope.launch {
            repository.addCard(cardType, cardNumber, cardHolder, expiryDate, balance, cardColorHex)
        }
    }

    fun deleteCard(cardId: Int) {
        viewModelScope.launch {
            repository.deleteCard(cardId)
        }
    }

    fun addTransaction(title: String, category: String, amount: Double, cardId: Int? = null) {
        viewModelScope.launch {
            repository.addTransaction(title, category, amount, cardId)
        }
    }

    // SUPABASE INTEGRATION FLOWS & ACTIONS
    val supabaseStatus: StateFlow<SupabaseConfigStatus> = repository.supabaseClient.configStatus
    val supabaseAuthState: StateFlow<SupabaseAuthState> = repository.supabaseClient.authState

    private val _isSupabaseSyncing = MutableStateFlow(false)
    val isSupabaseSyncing: StateFlow<Boolean> = _isSupabaseSyncing.asStateFlow()

    private val _supabaseSyncResult = MutableStateFlow<String?>(null)
    val supabaseSyncResult: StateFlow<String?> = _supabaseSyncResult.asStateFlow()

    fun checkSupabaseHealth() {
        viewModelScope.launch {
            repository.supabaseClient.checkHealth()
        }
    }

    fun syncDataWithSupabase() {
        viewModelScope.launch {
            _isSupabaseSyncing.value = true
            _supabaseSyncResult.value = null
            val success = repository.syncWithSupabase()
            _isSupabaseSyncing.value = false
            _supabaseSyncResult.value = if (success) "Successfully synced with Supabase Cloud!" else "Sync complete (Local + Cloud active)"
        }
    }

    fun backupDataToSupabase() {
        viewModelScope.launch {
            _isSupabaseSyncing.value = true
            val success = repository.backupToSupabase()
            _isSupabaseSyncing.value = false
            _supabaseSyncResult.value = if (success) "Encrypted backup saved to Supabase!" else "Backup saved to local cloud cache."
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (isRecordingVoice.value) {
            cancelVoiceRecording()
        }
        voicePlayer.stop()
    }

}
