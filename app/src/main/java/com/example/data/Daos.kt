package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY name ASC")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    suspend fun getUserById(uid: String): UserEntity?

    @Query("SELECT * FROM users WHERE phoneNumber = :phoneNumber LIMIT 1")
    suspend fun getUserByPhone(phoneNumber: String): UserEntity?

    @Query("SELECT * FROM users WHERE uid = :uid LIMIT 1")
    fun getUserFlow(uid: String): Flow<UserEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<UserEntity>)

    @Query("UPDATE users SET isOnline = :isOnline, lastSeen = :lastSeen WHERE uid = :uid")
    suspend fun updateUserOnline(uid: String, isOnline: Boolean, lastSeen: Long)

    @Query("UPDATE users SET isTyping = :isTyping WHERE uid = :uid")
    suspend fun updateUserTyping(uid: String, isTyping: Boolean)

    @Query("UPDATE users SET lastSeenPrivacy = :privacy WHERE uid = :uid")
    suspend fun updateLastSeenPrivacy(uid: String, privacy: String)

    @Query("UPDATE users SET twoStepPin = :pin WHERE uid = :uid")
    suspend fun updateTwoStepPin(uid: String, pin: String?)

    @Query("UPDATE users SET blockedUids = :blockedUids WHERE uid = :uid")
    suspend fun updateBlockedUids(uid: String, blockedUids: String)

    @Query("UPDATE users SET archivedChatIds = :archivedChatIds WHERE uid = :uid")
    suspend fun updateArchivedChats(uid: String, archivedChatIds: String)

    @Query("UPDATE users SET mutedChatIds = :mutedChatIds WHERE uid = :uid")
    suspend fun updateMutedChats(uid: String, mutedChatIds: String)
}

@Dao
interface ChatDao {
    @Query("SELECT * FROM chats ORDER BY isPinned DESC, lastMessageTime DESC")
    fun getAllChats(): Flow<List<ChatEntity>>

    @Query("SELECT * FROM chats WHERE chatId = :chatId LIMIT 1")
    suspend fun getChatById(chatId: String): ChatEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ChatEntity)

    @Query("UPDATE chats SET lastMessage = :lastMessage, lastMessageTime = :lastMessageTime WHERE chatId = :chatId")
    suspend fun updateLastMessage(chatId: String, lastMessage: String, lastMessageTime: Long)

    @Query("UPDATE chats SET unreadCount = :unreadCount WHERE chatId = :chatId")
    suspend fun updateUnreadCount(chatId: String, unreadCount: Int)

    @Query("UPDATE chats SET disappearingDuration = :duration WHERE chatId = :chatId")
    suspend fun updateDisappearingDuration(chatId: String, duration: Long)

    @Query("UPDATE chats SET isMuted = :isMuted WHERE chatId = :chatId")
    suspend fun updateChatMute(chatId: String, isMuted: Boolean)

    @Query("UPDATE chats SET isPinned = :isPinned WHERE chatId = :chatId")
    suspend fun updateChatPin(chatId: String, isPinned: Boolean)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipant(participant: ChatParticipantEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<ChatParticipantEntity>)

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN chat_participants cp ON u.uid = cp.userId 
        WHERE cp.chatId = :chatId
    """)
    fun getParticipantsForChat(chatId: String): Flow<List<UserEntity>>

    @Query("""
        SELECT u.* FROM users u 
        INNER JOIN chat_participants cp ON u.uid = cp.userId 
        WHERE cp.chatId = :chatId
    """)
    suspend fun getParticipantsForChatList(chatId: String): List<UserEntity>
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE chatId = :chatId ORDER BY timestamp ASC")
    fun getMessagesForChat(chatId: String): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Query("UPDATE messages SET status = :status WHERE messageId = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE messages SET text = :text, isEdited = 1 WHERE messageId = :messageId")
    suspend fun editMessage(messageId: String, text: String)

    @Query("UPDATE messages SET text = 'This message was deleted', isDeleted = 1 WHERE messageId = :messageId")
    suspend fun deleteMessageForEveryone(messageId: String)

    @Query("DELETE FROM messages WHERE messageId = :messageId")
    suspend fun deleteMessageForMe(messageId: String)

    @Query("SELECT * FROM messages WHERE messageId = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("UPDATE messages SET isStarred = :isStarred WHERE messageId = :messageId")
    suspend fun updateMessageStarred(messageId: String, isStarred: Boolean)

    @Query("SELECT * FROM messages WHERE isStarred = 1 ORDER BY timestamp DESC")
    fun getStarredMessages(): Flow<List<MessageEntity>>

    @Query("UPDATE messages SET reactions = :reactions WHERE messageId = :messageId")
    suspend fun updateMessageReactions(messageId: String, reactions: String)

    @Query("UPDATE messages SET sendFailed = :sendFailed WHERE messageId = :messageId")
    suspend fun updateMessageSendFailed(messageId: String, sendFailed: Boolean)

    @Query("DELETE FROM messages WHERE chatId = :chatId AND :now - timestamp > :expiry")
    suspend fun deleteExpiredMessages(chatId: String, now: Long, expiry: Long)

    @Query("DELETE FROM messages WHERE timestamp < :cutoffTime AND (isStarred IS NULL OR isStarred = 0)")
    suspend fun deleteMessagesOlderThan(cutoffTime: Long): Int

    @Query("SELECT * FROM messages WHERE status = 'retrying' OR status = 'failed' OR sendFailed = 1")
    suspend fun getPendingRetryingMessages(): List<MessageEntity>

    @Query("SELECT * FROM messages")
    suspend fun getAllMessagesList(): List<MessageEntity>
}

@Dao
interface CallDao {
    @Query("SELECT * FROM calls ORDER BY startedAt DESC")
    fun getAllCalls(): Flow<List<CallEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: CallEntity)

    @Query("UPDATE calls SET status = :status, endedAt = :endedAt, duration = :duration WHERE callId = :callId")
    suspend fun updateCallStatus(callId: String, status: String, endedAt: Long, duration: Long)
}

@Dao
interface StatusDao {
    @Query("SELECT * FROM statuses ORDER BY timestamp DESC")
    fun getAllStatuses(): Flow<List<StatusEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStatus(status: StatusEntity)

    @Query("UPDATE statuses SET viewers = :viewers, viewsCount = :viewsCount WHERE statusId = :statusId")
    suspend fun updateStatusViewers(statusId: String, viewers: String, viewsCount: Int)

    @Query("DELETE FROM statuses WHERE :now - timestamp > 86400000")
    suspend fun deleteExpiredStatuses(now: Long)
}

@Dao
interface CardDao {
    @Query("SELECT * FROM payment_cards ORDER BY id ASC")
    fun getAllCards(): Flow<List<CardEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Query("UPDATE payment_cards SET balance = :balance WHERE id = :id")
    suspend fun updateCardBalance(id: Int, balance: Double)

    @Query("DELETE FROM payment_cards WHERE id = :id")
    suspend fun deleteCardById(id: Int)
}

@Dao
interface TransactionDao {
    @Query("SELECT * FROM payment_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Query("DELETE FROM payment_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Int)
}

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE ownerUid = :ownerUid ORDER BY contactName ASC")
    fun getContactsForUser(ownerUid: String): Flow<List<ContactEntity>>

    @Query("SELECT * FROM contacts WHERE ownerUid = :ownerUid ORDER BY contactName ASC")
    suspend fun getContactsForUserList(ownerUid: String): List<ContactEntity>

    @Query("SELECT * FROM contacts WHERE ownerUid = :ownerUid AND contactPhoneNumber = :phoneNumber LIMIT 1")
    suspend fun getContactByPhoneNumber(ownerUid: String, phoneNumber: String): ContactEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContact(contact: ContactEntity)

    @Query("DELETE FROM contacts WHERE ownerUid = :ownerUid AND contactPhoneNumber = :phoneNumber")
    suspend fun deleteContactByPhone(ownerUid: String, phoneNumber: String)

    @Delete
    suspend fun deleteContact(contact: ContactEntity)
}

@Dao
interface CallSnapshotDao {
    @Query("SELECT * FROM call_snapshots ORDER BY timestamp DESC")
    fun getAllSnapshots(): Flow<List<CallSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: CallSnapshotEntity): Long

    @Query("DELETE FROM call_snapshots WHERE id = :id")
    suspend fun deleteSnapshot(id: Int)
}


