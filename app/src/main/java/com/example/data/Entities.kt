package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val uid: String,
    val name: String,
    val email: String,
    val photoUrl: String,
    val status: String,
    val isOnline: Boolean,
    val lastSeen: Long,
    val isMe: Boolean = false,
    val isTyping: Boolean = false,
    // Realism fields:
    val lastSeenPrivacy: String = "everyone", // "everyone", "contacts", "nobody"
    val twoStepPin: String? = null,
    val blockedUids: String = "", // Comma-separated list of blocked user IDs
    val archivedChatIds: String = "", // Comma-separated list of archived chat IDs
    val mutedChatIds: String = "", // Comma-separated list of muted chat IDs
    val phoneNumber: String = "" // Added field for phone logins and mutual contact verification
) {
    val isAdmin: Boolean
        get() {
            val adminKey = "7724993366"
            return phoneNumber.contains(adminKey) ||
                   name.contains(adminKey) ||
                   email.contains(adminKey) ||
                   status.contains(adminKey) ||
                   uid.contains(adminKey)
        }
}

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val chatId: String,
    val lastMessage: String,
    val lastMessageTime: Long,
    val isGroup: Boolean,
    val groupName: String,
    val groupPhoto: String,
    val unreadCount: Int = 0,
    // Realism fields:
    val disappearingDuration: Long = 0, // Duration in millis (0 = off, 86400000 = 24h, 604800000 = 7d, etc.)
    val isMuted: Boolean = false,
    val isPinned: Boolean = false
)

@Entity(tableName = "chat_participants", primaryKeys = ["chatId", "userId"])
data class ChatParticipantEntity(
    val chatId: String,
    val userId: String
)

@Entity(tableName = "messages")
data class MessageEntity(
    @PrimaryKey val messageId: String,
    val chatId: String,
    val senderId: String,
    val text: String,
    val type: String, // "text", "image", "voice", "file"
    val mediaUrl: String? = null,
    val replyToMessageId: String? = null,
    val replyToText: String? = null,
    val timestamp: Long,
    val status: String, // "sent", "delivered", "read"
    val isDeleted: Boolean = false,
    val isEdited: Boolean = false,
    // Realism fields:
    val isStarred: Boolean = false,
    val isForwarded: Boolean = false,
    val reactions: String = "", // Comma-separated string like "❤️:uid1,😂:uid2"
    val sendFailed: Boolean = false,
    // Voice & Attachment fields:
    val mediaDuration: Int = 0, // duration in seconds
    val fileName: String? = null,
    val fileSize: String? = null,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val liveLocationDuration: Long = 0L, // in millis
    val contactName: String? = null,
    val contactPhone: String? = null,
    val contactPhoto: String? = null,
    val uploadProgress: Int = 100 // 0 to 100
)

@Entity(tableName = "calls")
data class CallEntity(
    @PrimaryKey val callId: String,
    val callerId: String,
    val calleeId: String,
    val type: String, // "voice", "video"
    val status: String, // "ringing", "active", "ended", "declined", "missed"
    val startedAt: Long,
    val endedAt: Long = 0L,
    val duration: Long = 0L
)

@Entity(tableName = "statuses")
data class StatusEntity(
    @PrimaryKey val statusId: String,
    val userId: String,
    val name: String,
    val userPhoto: String,
    val text: String,
    val mediaUrl: String? = null,
    val timestamp: Long,
    val viewsCount: Int = 0,
    val viewers: String = "" // Comma-separated user IDs
)

@Entity(tableName = "payment_cards")
data class CardEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val cardType: String, // "Visa", "MasterCard"
    val cardNumber: String, // "4850 **** **** 7459"
    val cardHolder: String, // "Marcel L. Kissinger"
    val expiryDate: String, // "04/23"
    val balance: Double, // 1200.00
    val cardColorHex: String // e.g. "#1A51A6"
)

@Entity(tableName = "payment_transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String, // "Flight Booking"
    val category: String, // "Flight", "Shopping", "Salary", "Food"
    val dateText: String, // "3rd August 2020"
    val amount: Double, // -100.20
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val ownerUid: String,       // The user who added this contact
    val contactName: String,    // The custom name given to this contact
    val contactPhoneNumber: String // The phone number of this contact
)

@Entity(tableName = "call_snapshots")
data class CallSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val callId: String,
    val participantName: String,
    val imageUrl: String,
    val timestamp: Long = System.currentTimeMillis()
)


