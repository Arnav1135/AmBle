package com.example.data

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.util.UUID

class ChatRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val userDao = database.userDao()
    private val chatDao = database.chatDao()
    private val messageDao = database.messageDao()
    private val callDao = database.callDao()
    private val cardDao = database.cardDao()
    private val transactionDao = database.transactionDao()
    private val contactDao = database.contactDao()
    private val callSnapshotDao = database.callSnapshotDao()

    private val repositoryScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Currently logged in user
    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    // Real-time connection state & auto-retry engine
    private val _isNetworkConnected = MutableStateFlow(true)
    val isNetworkConnected: StateFlow<Boolean> = _isNetworkConnected.asStateFlow()

    fun setNetworkConnected(connected: Boolean) {
        _isNetworkConnected.value = connected
        if (connected) {
            repositoryScope.launch {
                retryPendingMessages()
            }
        }
    }

    suspend fun retryPendingMessages() {
        val pending = messageDao.getPendingRetryingMessages()
        for (msg in pending) {
            val updatedMsg = msg.copy(status = "sent", sendFailed = false, timestamp = System.currentTimeMillis())
            messageDao.insertMessage(updatedMsg)
            delay(300)
            messageDao.updateMessageStatus(msg.messageId, "delivered")
            triggerSimulationResponse(msg.chatId, updatedMsg)
        }
    }

    // Public flows
    val allUsers: Flow<List<UserEntity>> = userDao.getAllUsers().map { list ->
        list.filter { !it.isMe }
    }
    val allChats: Flow<List<ChatEntity>> = chatDao.getAllChats()
    val allCalls: Flow<List<CallEntity>> = callDao.getAllCalls()
    val allCards: Flow<List<CardEntity>> = cardDao.getAllCards()
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()
    val allCallSnapshots: Flow<List<CallSnapshotEntity>> = callSnapshotDao.getAllSnapshots()
    private val prefs = context.getSharedPreferences("amble_user_session", Context.MODE_PRIVATE)

    fun isRememberLoginEnabled(): Boolean {
        return prefs.getBoolean("remember_login", true)
    }

    fun setRememberLoginEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("remember_login", enabled).apply()
    }

    fun getSavedEmail(): String {
        return prefs.getString("saved_email", "") ?: ""
    }

    fun getSavedPhone(): String {
        return prefs.getString("saved_phone", "") ?: ""
    }

    fun saveUserSession(user: UserEntity, rememberLogin: Boolean = true) {
        val existingToken = prefs.getString("session_token", null)
        val token = existingToken ?: "token_${user.uid}_${System.currentTimeMillis()}"
        prefs.edit()
            .putBoolean("is_logged_in", true)
            .putBoolean("remember_login", rememberLogin)
            .putString("saved_user_uid", user.uid)
            .putString("saved_email", user.email)
            .putString("saved_phone", user.phoneNumber ?: "")
            .putString("saved_name", user.name)
            .putString("saved_avatar", user.photoUrl)
            .putString("saved_status", user.status)
            .putString("session_token", token)
            .apply()
    }

    fun clearUserSession() {
        prefs.edit()
            .putBoolean("is_logged_in", false)
            .remove("saved_user_uid")
            .remove("session_token")
            .remove("saved_email")
            .remove("saved_phone")
            .remove("saved_name")
            .remove("saved_avatar")
            .remove("saved_status")
            .apply()
    }

    init {
        // Initialize with default mock contacts and check if someone is already logged in
        repositoryScope.launch {
            initializeMockDataIfNeeded()
            loadCurrentUser()
            initializeMockWalletIfNeeded()
        }
    }

    private suspend fun loadCurrentUser() {
        val isLoggedIn = prefs.getBoolean("is_logged_in", true)
        val rememberLogin = prefs.getBoolean("remember_login", true)
        val savedUid = prefs.getString("saved_user_uid", null)

        val users = userDao.getAllUsers().firstOrNull() ?: emptyList()
        val me = if (savedUid != null) {
            users.firstOrNull { it.uid == savedUid } ?: users.firstOrNull { it.isMe }
        } else {
            users.firstOrNull { it.isMe }
        }

        if (me != null && isLoggedIn && rememberLogin) {
            val activeMe = me.copy(isMe = true, isOnline = true)
            userDao.insertUser(activeMe)
            _currentUser.value = activeMe
            userDao.updateUserOnline(me.uid, true, System.currentTimeMillis())
            saveUserSession(activeMe, rememberLogin = true)
        } else if (me != null && isLoggedIn && !rememberLogin) {
            val activeMe = me.copy(isMe = true, isOnline = true)
            userDao.insertUser(activeMe)
            _currentUser.value = activeMe
        }
    }

    private suspend fun initializeMockDataIfNeeded() {
        val count = userDao.getAllUsers().first().size
        if (count == 0) {
            val defaultMe = UserEntity(
                uid = "alex_mercer_me",
                name = "Alex Mercer",
                email = "alex.mercer@chatwave.io",
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                status = "Connecting the world with AmBle ✨",
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isMe = true,
                phoneNumber = "+15550100"
            )
            userDao.insertUser(defaultMe)
            _currentUser.value = defaultMe
            saveUserSession(defaultMe, rememberLogin = true)

            // Generate standard mock contacts
            val mockUsers = listOf(
                UserEntity(
                    uid = "sarah_uid",
                    name = "Sarah Jenkins",
                    email = "sarah.j@chatwave.io",
                    photoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
                    status = "Productivity is key 🚀 | Coding is art",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isMe = false,
                    phoneNumber = "+15550101"
                ),
                UserEntity(
                    uid = "alex_uid",
                    name = "Alex Rivera",
                    email = "alex.r@chatwave.io",
                    photoUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
                    status = "Tech Lead @ ChatWave | Coffee enthusiast ☕",
                    isOnline = false,
                    lastSeen = System.currentTimeMillis() - 1800000, // 30 mins ago
                    isMe = false,
                    phoneNumber = "+15550102"
                ),
                UserEntity(
                    uid = "elena_uid",
                    name = "Elena Petrova",
                    email = "elena.p@chatwave.io",
                    photoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
                    status = "Designing the future ✨ | UI/UX",
                    isOnline = true,
                    lastSeen = System.currentTimeMillis(),
                    isMe = false,
                    phoneNumber = "+15550103"
                )
            )
            userDao.insertUsers(mockUsers)

            // Create some default chats for them
            mockUsers.forEach { mockUser ->
                createOneOnOneChat(mockUser)
            }

            // Create a default group chat
            createGroupChat(
                name = "AmBle Core Team",
                photoUrl = "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
                memberIds = listOf("sarah_uid", "alex_uid", "elena_uid")
            )
        }
    }

    // AUTH ACTIONS
    suspend fun signUp(name: String, email: String, status: String, avatarPreset: String, rememberLogin: Boolean = true, password: String = "password123"): Boolean {
        val users = userDao.getAllUsers().first()
        users.forEach {
            if (it.isMe) {
                userDao.insertUser(it.copy(isMe = false))
            }
        }

        var uid = UUID.randomUUID().toString()

        // Try Supabase Auth if configured
        if (supabaseClient.isConfigured) {
            val res = supabaseClient.signUp(email, password)
            res.onSuccess { auth ->
                uid = auth.userId
            }
        }

        val me = UserEntity(
            uid = uid,
            name = name,
            email = email,
            photoUrl = avatarPreset,
            status = status.ifEmpty { "Hey there! I am using AmBle." },
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            isMe = true
        )
        userDao.insertUser(me)
        _currentUser.value = me

        if (supabaseClient.isConfigured) {
            supabaseClient.syncUserToSupabase(me)
        }

        if (rememberLogin) {
            saveUserSession(me, rememberLogin = true)
        } else {
            prefs.edit().putBoolean("is_logged_in", true).putBoolean("remember_login", false).apply()
        }
        initializeMockDataIfNeeded()
        return true
    }

    suspend fun signIn(email: String, rememberLogin: Boolean = true, password: String = "password123"): Boolean {
        val users = userDao.getAllUsers().first()
        users.forEach {
            if (it.isMe && !it.email.equals(email, ignoreCase = true)) {
                userDao.insertUser(it.copy(isMe = false))
            }
        }

        var remoteUid: String? = null
        // Try Supabase Auth if configured
        if (supabaseClient.isConfigured) {
            val res = supabaseClient.signIn(email, password)
            res.onSuccess { auth ->
                remoteUid = auth.userId
            }
        }

        val existing = users.firstOrNull { it.email.equals(email, ignoreCase = true) }
        val me = if (existing != null) {
            existing.copy(
                uid = remoteUid ?: existing.uid,
                isMe = true,
                isOnline = true,
                lastSeen = System.currentTimeMillis()
            )
        } else {
            UserEntity(
                uid = remoteUid ?: UUID.randomUUID().toString(),
                name = email.substringBefore("@").replaceFirstChar { it.uppercase() },
                email = email,
                photoUrl = "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
                status = "Hey there! I am using AmBle.",
                isOnline = true,
                lastSeen = System.currentTimeMillis(),
                isMe = true,
                phoneNumber = "+15550001"
            )
        }

        userDao.insertUser(me)
        _currentUser.value = me
        userDao.updateUserOnline(me.uid, true, System.currentTimeMillis())

        if (supabaseClient.isConfigured) {
            supabaseClient.syncUserToSupabase(me)
        }

        if (rememberLogin) {
            saveUserSession(me, rememberLogin = true)
        } else {
            prefs.edit().putBoolean("is_logged_in", true).putBoolean("remember_login", false).apply()
        }
        initializeMockDataIfNeeded()
        return true
    }

    suspend fun signOut() {
        val me = _currentUser.value
        if (me != null) {
            userDao.updateUserOnline(me.uid, false, System.currentTimeMillis())
            userDao.insertUser(me.copy(isMe = false, isOnline = false))
        }
        clearUserSession()
        _currentUser.value = null
    }

    suspend fun setUserOnlineStatus(isOnline: Boolean) {
        val me = _currentUser.value ?: return
        val updated = me.copy(isOnline = isOnline, lastSeen = System.currentTimeMillis())
        _currentUser.value = updated
        userDao.insertUser(updated)
        userDao.updateUserOnline(me.uid, isOnline, System.currentTimeMillis())
        if (supabaseClient.isConfigured) {
            supabaseClient.syncUserToSupabase(updated)
        }
    }

    suspend fun updateUserTyping(uid: String, isTyping: Boolean) {
        userDao.updateUserTyping(uid, isTyping)
    }

    // PHONE AUTHENTICATION ACTIONS
    suspend fun getUserByPhone(phoneNumber: String): UserEntity? {
        return userDao.getUserByPhone(phoneNumber)
    }

    suspend fun insertUser(user: UserEntity) {
        userDao.insertUser(user)
    }

    suspend fun signInWithPhone(phoneNumber: String, rememberLogin: Boolean = true): Boolean {
        val user = userDao.getUserByPhone(phoneNumber)
        if (user != null) {
            val updatedMe = user.copy(isMe = true, isOnline = true, lastSeen = System.currentTimeMillis())
            userDao.insertUser(updatedMe)
            _currentUser.value = updatedMe
            if (rememberLogin) {
                saveUserSession(updatedMe, rememberLogin = true)
            } else {
                prefs.edit().putBoolean("is_logged_in", true).putBoolean("remember_login", false).apply()
            }
            initializeMockDataIfNeeded()
            return true
        }
        return false
    }

    suspend fun signUpWithPhone(name: String, phoneNumber: String, status: String, avatarPreset: String, rememberLogin: Boolean = true): Boolean {
        // Clear previous isMe users to avoid conflicts
        val users = userDao.getAllUsers().first()
        users.forEach {
            if (it.isMe) {
                userDao.insertUser(it.copy(isMe = false))
            }
        }

        val uid = UUID.randomUUID().toString()
        val me = UserEntity(
            uid = uid,
            name = name,
            email = "${phoneNumber.replace("+", "")}@phone.AmBle.io",
            photoUrl = avatarPreset,
            status = status.ifEmpty { "Hey there! I am using AmBle." },
            isOnline = true,
            lastSeen = System.currentTimeMillis(),
            isMe = true,
            phoneNumber = phoneNumber
        )
        userDao.insertUser(me)
        _currentUser.value = me
        if (rememberLogin) {
            saveUserSession(me, rememberLogin = true)
        } else {
            prefs.edit().putBoolean("is_logged_in", true).putBoolean("remember_login", false).apply()
        }
        initializeMockDataIfNeeded()
        return true
    }

    // CONTACT ADDRESS BOOK ACTIONS
    fun getContacts(): Flow<List<ContactEntity>> {
        val me = _currentUser.value ?: return flowOf(emptyList())
        return contactDao.getContactsForUser(me.uid)
    }

    suspend fun getContactsList(): List<ContactEntity> {
        val me = _currentUser.value ?: return emptyList()
        return contactDao.getContactsForUserList(me.uid)
    }

    suspend fun addContact(name: String, phoneNumber: String): Boolean {
        val me = _currentUser.value ?: return false
        val contact = ContactEntity(
            ownerUid = me.uid,
            contactName = name,
            contactPhoneNumber = phoneNumber
        )
        contactDao.insertContact(contact)
        return true
    }

    suspend fun deleteContact(phoneNumber: String) {
        val me = _currentUser.value ?: return
        contactDao.deleteContactByPhone(me.uid, phoneNumber)
    }

    suspend fun hasOtherUserAddedMe(otherUid: String, otherPhone: String): Boolean {
        val me = _currentUser.value ?: return false
        if (me.phoneNumber.isEmpty()) return false
        val check = contactDao.getContactByPhoneNumber(otherUid, me.phoneNumber)
        return check != null
    }

    suspend fun simulateOtherUserAddingMe(otherUid: String, otherName: String, otherPhone: String, shouldAdd: Boolean) {
        val me = _currentUser.value ?: return
        if (me.phoneNumber.isEmpty()) return
        if (shouldAdd) {
            val contact = ContactEntity(
                ownerUid = otherUid,
                contactName = me.name,
                contactPhoneNumber = me.phoneNumber
            )
            contactDao.insertContact(contact)
        } else {
            contactDao.deleteContactByPhone(otherUid, me.phoneNumber)
        }
    }

    // CHATS & PARTICIPANTS
    fun getMessages(chatId: String): Flow<List<MessageEntity>> = messageDao.getMessagesForChat(chatId)

    fun getParticipants(chatId: String): Flow<List<UserEntity>> = chatDao.getParticipantsForChat(chatId)

    suspend fun createOneOnOneChat(otherUser: UserEntity): String {
        val me = _currentUser.value ?: return ""
        val chatId = if (me.uid < otherUser.uid) "${me.uid}_${otherUser.uid}" else "${otherUser.uid}_${me.uid}"
        
        val existing = chatDao.getChatById(chatId)
        if (existing == null) {
            val chat = ChatEntity(
                chatId = chatId,
                lastMessage = "No messages yet",
                lastMessageTime = System.currentTimeMillis(),
                isGroup = false,
                groupName = otherUser.name,
                groupPhoto = otherUser.photoUrl
            )
            chatDao.insertChat(chat)
            chatDao.insertParticipant(ChatParticipantEntity(chatId, me.uid))
            chatDao.insertParticipant(ChatParticipantEntity(chatId, otherUser.uid))
        }
        return chatId
    }

    suspend fun createGroupChat(name: String, photoUrl: String, memberIds: List<String>): String {
        val me = _currentUser.value ?: return ""
        val chatId = "group_${UUID.randomUUID()}"
        val chat = ChatEntity(
            chatId = chatId,
            lastMessage = "Group created",
            lastMessageTime = System.currentTimeMillis(),
            isGroup = true,
            groupName = name,
            groupPhoto = photoUrl.ifEmpty { "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150" }
        )
        chatDao.insertChat(chat)
        
        // Add Me
        chatDao.insertParticipant(ChatParticipantEntity(chatId, me.uid))
        // Add other members
        memberIds.forEach { memberId ->
            chatDao.insertParticipant(ChatParticipantEntity(chatId, memberId))
        }

        // Send a system message
        sendMessage(
            chatId = chatId,
            text = "${me.name} created the group \"$name\"",
            type = "text"
        )
        return chatId
    }

    // MESSAGING
    suspend fun sendMessage(
        chatId: String,
        text: String,
        type: String,
        mediaUrl: String? = null,
        mediaDuration: Int = 0,
        fileName: String? = null,
        fileSize: String? = null,
        latitude: Double = 0.0,
        longitude: Double = 0.0,
        liveLocationDuration: Long = 0L,
        contactName: String? = null,
        contactPhone: String? = null,
        contactPhoto: String? = null,
        replyToId: String? = null,
        replyToText: String? = null,
        isForwarded: Boolean = false
    ) {
        val me = _currentUser.value ?: return
        val messageId = UUID.randomUUID().toString()
        val isOffline = !_isNetworkConnected.value

        val message = MessageEntity(
            messageId = messageId,
            chatId = chatId,
            senderId = me.uid,
            text = text,
            type = type,
            mediaUrl = mediaUrl,
            mediaDuration = mediaDuration,
            fileName = fileName,
            fileSize = fileSize,
            latitude = latitude,
            longitude = longitude,
            liveLocationDuration = liveLocationDuration,
            contactName = contactName,
            contactPhone = contactPhone,
            contactPhoto = contactPhoto,
            replyToMessageId = replyToId,
            replyToText = replyToText,
            timestamp = System.currentTimeMillis(),
            status = if (isOffline) "retrying" else "sent",
            isForwarded = isForwarded,
            sendFailed = isOffline,
            uploadProgress = if (type == "text") 100 else 100
        )
        messageDao.insertMessage(message)
        chatDao.updateLastMessage(chatId, if (type == "text") text else "[${type.replaceFirstChar { it.uppercase() }}]", System.currentTimeMillis())

        // Asynchronously sync message to Supabase cloud
        repositoryScope.launch {
            supabaseClient.sendMessageToSupabase(message)
        }

        if (isOffline) {
            return
        }

        // Realistic random delivery simulation delay: 200 - 600ms
        val delayTime = (200..600).random().toLong()
        delay(delayTime)
        messageDao.updateMessageStatus(messageId, "delivered")

        // Trigger simulation reply
        triggerSimulationResponse(chatId, message)

        // Handle disappearing messages schedule
        val chat = chatDao.getChatById(chatId)
        if (chat != null && chat.disappearingDuration > 0) {
            repositoryScope.launch {
                delay(chat.disappearingDuration)
                messageDao.deleteMessageForMe(messageId)
            }
        }
    }

    suspend fun deleteMessage(messageId: String, forEveryone: Boolean) {
        if (forEveryone) {
            messageDao.deleteMessageForEveryone(messageId)
        } else {
            messageDao.deleteMessageForMe(messageId)
        }
    }

    suspend fun editMessage(messageId: String, newText: String) {
        messageDao.editMessage(messageId, newText)
    }

    suspend fun markChatAsRead(chatId: String) {
        // Find all received messages in this chat and mark them as read (blue ticks)
        val me = _currentUser.value ?: return
        chatDao.updateUnreadCount(chatId, 0)
    }

    // AUDIO RECORDING FOR VOICE NOTES
    private var mediaRecorder: MediaRecorder? = null
    private var voiceNoteFile: File? = null

    fun startVoiceRecording(): Boolean {
        return try {
            val audioFile = File.createTempFile("voice_note_", ".3gp", context.cacheDir)
            voiceNoteFile = audioFile
            
            @Suppress("DEPRECATION")
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context).apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
            } else {
                MediaRecorder().apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFile.absolutePath)
                    prepare()
                    start()
                }
            }
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to start recording", e)
            false
        }
    }

    fun stopVoiceRecording(chatId: String): Boolean {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            
            val file = voiceNoteFile
            if (file != null && file.exists() && file.length() > 0) {
                repositoryScope.launch {
                    sendMessage(
                        chatId = chatId,
                        text = "Voice message",
                        type = "voice",
                        mediaUrl = file.absolutePath
                    )
                }
                true
            } else {
                false
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "Failed to stop recording", e)
            false
        }
    }

    fun cancelVoiceRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaRecorder = null
        voiceNoteFile?.delete()
        voiceNoteFile = null
    }

    // CALLS
    suspend fun placeCall(calleeId: String, type: String): String {
        val me = _currentUser.value ?: return ""
        val callId = UUID.randomUUID().toString()
        val call = CallEntity(
            callId = callId,
            callerId = me.uid,
            calleeId = calleeId,
            type = type,
            status = "ringing",
            startedAt = System.currentTimeMillis()
        )
        callDao.insertCall(call)

        // Trigger simulator call accept/decline after 3 seconds
        triggerSimulationCallAccept(callId, calleeId, type)
        return callId
    }

    suspend fun acceptCall(callId: String) {
        callDao.updateCallStatus(callId, "active", 0L, 0L)
    }

    suspend fun endCall(callId: String, status: String = "ended") {
        val calls = allCalls.first()
        val call = calls.firstOrNull { it.callId == callId } ?: return
        val endedAt = System.currentTimeMillis()
        val duration = (endedAt - call.startedAt) / 1000
        callDao.updateCallStatus(callId, status, endedAt, duration)
    }

    // SIMULATED REAL-TIME INTERACTION SERVICE
    private fun triggerSimulationResponse(chatId: String, message: MessageEntity) {
        if (message.senderId != _currentUser.value?.uid) return

        repositoryScope.launch {
            if (chatId.startsWith("group_")) {
                // Group Chat Response simulation
                delay(1500)
                val responders = listOf("sarah_uid", "elena_uid", "alex_uid")
                val responderId = responders.random()
                val responder = userDao.getUserById(responderId) ?: return@launch

                userDao.updateUserTyping(responderId, true)
                delay(2000)
                userDao.updateUserTyping(responderId, false)

                val replyText = when (message.type) {
                    "text" -> getGroupReply(message.text, responder.name)
                    "voice" -> "Listening to your voice note! Awesome voice 🎧"
                    "image" -> "That looks incredible! Love the interface visual."
                    else -> "Got your file. Thanks for sharing!"
                }

                val replyMessage = MessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = responderId,
                    text = replyText,
                    type = "text",
                    timestamp = System.currentTimeMillis(),
                    status = "read"
                )
                messageDao.insertMessage(replyMessage)
                chatDao.updateLastMessage(chatId, replyText, System.currentTimeMillis())
            } else {
                // 1-on-1 Chat Response simulation
                val parts = chatId.split("_")
                val otherUserId = parts.firstOrNull { it != _currentUser.value?.uid } ?: return@launch
                val responder = userDao.getUserById(otherUserId) ?: return@launch

                // Set user status to typing after a brief delay
                delay(1000)
                userDao.updateUserOnline(otherUserId, true, System.currentTimeMillis())
                userDao.updateUserTyping(otherUserId, true)
                
                // Read receipt ticks update: show double blue tick for sent message
                messageDao.updateMessageStatus(message.messageId, "read")

                // Typing delay proportional to text length
                val typingTime = (message.text.length * 50L + 1500L).coerceAtMost(4000L)
                delay(typingTime)

                userDao.updateUserTyping(otherUserId, false)

                val replyText = when (message.type) {
                    "text" -> {
                        val geminiRes = GeminiApiClient.generateLowLatencyResponse(message.text)
                        "${geminiRes.text}\n\n⚡ Low-Latency (${geminiRes.latencyMs}ms) [${geminiRes.modelUsed}]"
                    }
                    "voice" -> "Wow, that sounds great! 🎙️ I will listen closely and reply."
                    "image" -> "Awesome image! The dynamic colors look amazing."
                    else -> "Thanks for sending the document. I will review it shortly."
                }

                val replyMessage = MessageEntity(
                    messageId = UUID.randomUUID().toString(),
                    chatId = chatId,
                    senderId = otherUserId,
                    text = replyText,
                    type = "text",
                    timestamp = System.currentTimeMillis(),
                    status = "read"
                )
                messageDao.insertMessage(replyMessage)
                chatDao.updateLastMessage(chatId, replyText, System.currentTimeMillis())
            }
        }
    }

    private fun triggerSimulationCallAccept(callId: String, calleeId: String, type: String) {
        repositoryScope.launch {
            delay(3000)
            val callee = userDao.getUserById(calleeId) ?: return@launch
            
            // Simulation random behavior: 90% accept, 10% decline
            if (Math.random() < 0.9) {
                callDao.updateCallStatus(callId, "active", System.currentTimeMillis(), 0)
                
                // Simulate an active call session of 15 seconds then hang up
                delay(15000)
                endCall(callId, "ended")
            } else {
                endCall(callId, "declined")
            }
        }
    }

    private fun getOneOnOneReply(incoming: String, name: String): String {
        val clean = incoming.lowercase().trim()
        return when {
            clean.contains("hello") || clean.contains("hi") || clean.contains("hey") -> {
                "Hey there! Great to connect. How is your day going? 😊"
            }
            clean.contains("how are you") || clean.contains("how's it going") -> {
                "I'm doing fantastic, thanks for asking! Busy cooking up some new ideas for ChatWave. What are you working on?"
            }
            clean.contains("video") || clean.contains("call") -> {
                "Oh cool! Tap the phone or video icon on the top right anytime. I'm online and ready to test!"
            }
            clean.contains("design") || clean.contains("ui") || clean.contains("colors") -> {
                "I love Material Design 3! The rounded buttons, dynamic colors, and smooth sliding screens are so satisfying."
            }
            clean.contains("group") -> {
                "Yes! You can create group chats too. Just tap the floating pencil button on the chats screen and choose 'New Group'."
            }
            clean.contains("bye") || clean.contains("goodnight") -> {
                "Goodbye! Talk to you later. Have a wonderful time! 👋"
            }
            else -> {
                listOf(
                    "That sounds interesting! Tell me more about it.",
                    "Ah, got it! Let me know if you want to test calling or voice notes.",
                    "Haha nice! ChatWave's real-time simulation is so responsive 🚀",
                    "Indeed! Let's schedule a call tomorrow to review.",
                    "Awesome! By the way, check out the Calls history tab to see your records."
                ).random()
            }
        }
    }

    private fun getGroupReply(incoming: String, senderName: String): String {
        val clean = incoming.lowercase().trim()
        return when {
            clean.contains("hello") || clean.contains("hi") -> {
                "Hi everyone! Welcome to our team channel 🌟 - $senderName"
            }
            clean.contains("status") || clean.contains("update") -> {
                "Everything is building perfectly on my end! Testing the voice note waveform engine now. - $senderName"
            }
            else -> {
                listOf(
                    "Agree! Let's double down on this milestone. - $senderName",
                    "Can someone send the latest logo asset? 🎨 - $senderName",
                    "Looks good to me! Let's merge it. - $senderName",
                    "Fantastic progress! Let's keep moving fast. - $senderName"
                ).random()
            }
        }
    }

    // Trigger an incoming call simulated for debugging/testing
    fun triggerSimulatedIncomingCall(callerId: String, type: String) {
        repositoryScope.launch {
            val me = _currentUser.value ?: return@launch
            val callId = UUID.randomUUID().toString()
            val call = CallEntity(
                callId = callId,
                callerId = callerId,
                calleeId = me.uid,
                type = type,
                status = "ringing",
                startedAt = System.currentTimeMillis()
            )
            callDao.insertCall(call)
        }
    }

    suspend fun deleteMessagesOlderThan30Days(): Int {
        val cutoffTime = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000L
        return messageDao.deleteMessagesOlderThan(cutoffTime)
    }

    // RETRY OFFLINE MESSAGES
    suspend fun retrySendMessage(messageId: String) {
        if (!_isNetworkConnected.value) return
        val msg = messageDao.getMessageById(messageId) ?: return
        val updatedMsg = msg.copy(sendFailed = false, timestamp = System.currentTimeMillis())
        messageDao.insertMessage(updatedMsg)

        // Simulating realistic delivery delay after connection restoration
        val delayTime = (200..600).random().toLong()
        delay(delayTime)
        messageDao.updateMessageStatus(messageId, "delivered")

        // Trigger simulator response
        triggerSimulationResponse(msg.chatId, updatedMsg)

        // Handle disappearing TTL schedule
        val chat = chatDao.getChatById(msg.chatId)
        if (chat != null && chat.disappearingDuration > 0) {
            repositoryScope.launch {
                delay(chat.disappearingDuration)
                messageDao.deleteMessageForMe(messageId)
            }
        }
    }

    // STARRED MESSAGES
    fun getStarredMessages(): Flow<List<MessageEntity>> = messageDao.getStarredMessages()

    suspend fun toggleMessageStar(messageId: String) {
        val msg = messageDao.getMessageById(messageId) ?: return
        messageDao.updateMessageStarred(messageId, !msg.isStarred)
    }

    // MESSAGE REACTIONS
    suspend fun toggleMessageReaction(messageId: String, emoji: String) {
        val me = _currentUser.value ?: return
        val msg = messageDao.getMessageById(messageId) ?: return

        // Parse current reactions: "❤️:uid1,😂:uid2"
        val list = if (msg.reactions.isEmpty()) mutableListOf() else msg.reactions.split(",").toMutableList()
        val reactionKey = "$emoji:${me.uid}"
        val alreadyReacted = list.contains(reactionKey)

        if (alreadyReacted) {
            list.remove(reactionKey)
        } else {
            // Remove any other reaction by Me to mimic clean WhatsApp behavior
            list.removeAll { it.endsWith(":${me.uid}") }
            list.add(reactionKey)
        }
        messageDao.updateMessageReactions(messageId, list.joinToString(","))
    }

    // CHAT MANAGEMENT
    suspend fun updateChatDisappearing(chatId: String, duration: Long) {
        chatDao.updateDisappearingDuration(chatId, duration)
    }

    suspend fun toggleMuteChat(chatId: String) {
        val me = _currentUser.value ?: return
        val currentMuted = me.mutedChatIds.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentMuted.contains(chatId)) {
            currentMuted.remove(chatId)
        } else {
            currentMuted.add(chatId)
        }
        val joined = currentMuted.joinToString(",")
        userDao.updateMutedChats(me.uid, joined)
        _currentUser.value = me.copy(mutedChatIds = joined)
    }

    suspend fun toggleArchiveChat(chatId: String) {
        val me = _currentUser.value ?: return
        val currentArchived = me.archivedChatIds.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentArchived.contains(chatId)) {
            currentArchived.remove(chatId)
        } else {
            currentArchived.add(chatId)
        }
        val joined = currentArchived.joinToString(",")
        userDao.updateArchivedChats(me.uid, joined)
        _currentUser.value = me.copy(archivedChatIds = joined)
        
        // Remove pin if archived
        chatDao.updateChatPin(chatId, false)
    }

    suspend fun togglePinChat(chatId: String) {
        val chat = chatDao.getChatById(chatId) ?: return
        chatDao.updateChatPin(chatId, !chat.isPinned)
    }

    suspend fun toggleBlockUser(targetUid: String) {
        val me = _currentUser.value ?: return
        val currentBlocked = me.blockedUids.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (currentBlocked.contains(targetUid)) {
            currentBlocked.remove(targetUid)
        } else {
            currentBlocked.add(targetUid)
        }
        val joined = currentBlocked.joinToString(",")
        userDao.updateBlockedUids(me.uid, joined)
        _currentUser.value = me.copy(blockedUids = joined)
    }

    suspend fun updateLastSeenPrivacy(privacy: String) {
        val me = _currentUser.value ?: return
        userDao.updateLastSeenPrivacy(me.uid, privacy)
        _currentUser.value = me.copy(lastSeenPrivacy = privacy)
    }

    suspend fun updateTwoStepPin(pin: String?) {
        val me = _currentUser.value ?: return
        userDao.updateTwoStepPin(me.uid, pin)
        _currentUser.value = me.copy(twoStepPin = pin)
    }

    // STATUS STORIES
    val allStatuses: Flow<List<StatusEntity>> = database.statusDao().getAllStatuses()

    suspend fun postStatus(text: String, mediaUrl: String? = null) {
        val me = _currentUser.value ?: return
        val statusId = UUID.randomUUID().toString()
        val status = StatusEntity(
            statusId = statusId,
            userId = me.uid,
            name = me.name,
            userPhoto = me.photoUrl,
            text = text,
            mediaUrl = mediaUrl,
            timestamp = System.currentTimeMillis()
        )
        database.statusDao().insertStatus(status)
        database.statusDao().deleteExpiredStatuses(System.currentTimeMillis())
    }

    suspend fun viewStatus(statusId: String) {
        val me = _currentUser.value ?: return
        val statuses = allStatuses.first()
        val status = statuses.firstOrNull { it.statusId == statusId } ?: return
        if (status.userId == me.uid) return

        val currentViewers = status.viewers.split(",").filter { it.isNotEmpty() }.toMutableList()
        if (!currentViewers.contains(me.uid)) {
            currentViewers.add(me.uid)
            database.statusDao().updateStatusViewers(statusId, currentViewers.joinToString(","), currentViewers.size)
        }
    }

    // WALLET ACTIONS
    suspend fun initializeMockWalletIfNeeded() {
        val cards = cardDao.getAllCards().first()
        if (cards.isEmpty()) {
            cardDao.insertCard(
                CardEntity(
                    cardType = "Visa",
                    cardNumber = "4850 **** **** 7459",
                    cardHolder = "Marcel L. Kissinger",
                    expiryDate = "04/29",
                    balance = 1200.00,
                    cardColorHex = "#1A51A6"
                )
            )
            cardDao.insertCard(
                CardEntity(
                    cardType = "MasterCard",
                    cardNumber = "5234 **** **** 9102",
                    cardHolder = "Marcel L. Kissinger",
                    expiryDate = "12/28",
                    balance = 4850.50,
                    cardColorHex = "#FF5F00"
                )
            )
        }
        val txs = transactionDao.getAllTransactions().first()
        if (txs.isEmpty()) {
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Flight Booking",
                    category = "Flight",
                    dateText = "3rd August 2026",
                    amount = -100.20
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Supermarket Shopping",
                    category = "Shopping",
                    dateText = "2nd August 2026",
                    amount = -45.50
                )
            )
            transactionDao.insertTransaction(
                TransactionEntity(
                    title = "Monthly Salary",
                    category = "Salary",
                    dateText = "1st August 2026",
                    amount = 2500.00
                )
            )
        }
    }

    suspend fun addCard(cardType: String, cardNumber: String, cardHolder: String, expiryDate: String, balance: Double, cardColorHex: String) {
        cardDao.insertCard(
            CardEntity(
                cardType = cardType,
                cardNumber = cardNumber,
                cardHolder = cardHolder,
                expiryDate = expiryDate,
                balance = balance,
                cardColorHex = cardColorHex
            )
        )
    }

    suspend fun deleteCard(cardId: Int) {
        cardDao.deleteCardById(cardId)
    }

    suspend fun addTransaction(title: String, category: String, amount: Double, cardId: Int? = null) {
        val sdf = java.text.SimpleDateFormat("d MMMM yyyy", java.util.Locale.ENGLISH)
        val dateStr = sdf.format(java.util.Date())
        transactionDao.insertTransaction(
            TransactionEntity(
                title = title,
                category = category,
                dateText = dateStr,
                amount = amount
            )
        )
        if (cardId != null) {
            val cards = cardDao.getAllCards().first()
            val card = cards.firstOrNull { it.id == cardId }
            if (card != null) {
                cardDao.updateCardBalance(cardId, card.balance + amount)
            }
        }
    }

    // SUPABASE CLOUD INTEGRATION
    val supabaseClient = SupabaseClient.getInstance()

    suspend fun syncWithSupabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val isHealthy = supabaseClient.checkHealth()
                if (!isHealthy) return@withContext false

                // 1. Push current user profile to Supabase
                val me = _currentUser.value
                if (me != null) {
                    supabaseClient.syncUserToSupabase(me)
                }

                // 2. Fetch remote contacts/users from Supabase and merge with local DB
                val remoteUsers = supabaseClient.fetchRemoteUsers()
                remoteUsers.forEach { remoteUser ->
                    if (remoteUser.uid != me?.uid) {
                        userDao.insertUser(remoteUser)
                    }
                }

                // 3. Sync chats and messages for current active user
                val chats = chatDao.getAllChats().first()
                chats.forEach { chat ->
                    val remoteMsgs = supabaseClient.fetchMessagesFromSupabase(chat.chatId)
                    remoteMsgs.forEach { remoteMsg ->
                        val localMsg = messageDao.getMessageById(remoteMsg.messageId)
                        if (localMsg == null) {
                            messageDao.insertMessage(remoteMsg)
                        }
                    }
                }
                true
            } catch (e: Exception) {
                Log.e("ChatRepository", "Error syncing with Supabase", e)
                false
            }
        }
    }

    suspend fun backupToSupabase(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val me = _currentUser.value ?: return@withContext false
                val chats = chatDao.getAllChats().first()
                val messages = messageDao.getAllMessagesList()
                val contacts = contactDao.getContactsForUserList(me.uid)

                val backupJson = org.json.JSONObject().apply {
                    put("user_id", me.uid)
                    put("user_name", me.name)
                    put("user_email", me.email)
                    put("timestamp", System.currentTimeMillis())
                    put("chats_count", chats.size)
                    put("messages_count", messages.size)
                    put("contacts_count", contacts.size)
                }.toString()

                supabaseClient.saveBackupToSupabase(me.uid, backupJson)
            } catch (e: Exception) {
                Log.e("ChatRepository", "Failed to backup to Supabase", e)
                false
            }
        }
    }

    suspend fun saveCallSnapshot(callId: String, participantName: String, imageUrl: String): Long {
        return withContext(Dispatchers.IO) {
            val snapshot = CallSnapshotEntity(
                callId = callId,
                participantName = participantName,
                imageUrl = imageUrl,
                timestamp = System.currentTimeMillis()
            )
            callSnapshotDao.insertSnapshot(snapshot)
        }
    }
}
