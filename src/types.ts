export interface UserEntity {
  uid: string;
  name: string;
  email: string;
  photoUrl: string;
  status: string;
  isOnline: boolean;
  lastSeen: number;
  isMe?: boolean;
  isTyping?: boolean;
  lastSeenPrivacy?: 'everyone' | 'contacts' | 'nobody';
  twoStepPin?: string | null;
  blockedUids?: string;
  archivedChatIds?: string;
  mutedChatIds?: string;
  phoneNumber?: string;
  isAdmin?: boolean;
}

export interface ChatEntity {
  chatId: string;
  lastMessage: string;
  lastMessageTime: number;
  isGroup: boolean;
  groupName: string;
  groupPhoto: string;
  unreadCount: number;
  disappearingDuration?: number; // 0, 86400000 (24h), 604800000 (7d)
  isMuted?: boolean;
  isPinned?: boolean;
}

export interface MessageEntity {
  messageId: string;
  chatId: string;
  senderId: string;
  text: string;
  type: 'text' | 'image' | 'voice' | 'file' | 'location' | 'contact';
  mediaUrl?: string | null;
  replyToMessageId?: string | null;
  replyToText?: string | null;
  timestamp: number;
  status: 'sent' | 'delivered' | 'read' | 'failed';
  isDeleted?: boolean;
  isEdited?: boolean;
  isStarred?: boolean;
  isForwarded?: boolean;
  reactions?: string; // e.g. "❤️:uid1,😂:uid2"
  sendFailed?: boolean;
  mediaDuration?: number; // duration in seconds
  fileName?: string | null;
  fileSize?: string | null;
  latitude?: number;
  longitude?: number;
  liveLocationDuration?: number;
  contactName?: string | null;
  contactPhone?: string | null;
  contactPhoto?: string | null;
  uploadProgress?: number;
}

export interface CallEntity {
  callId: string;
  callerId: string;
  calleeId: string;
  type: 'voice' | 'video';
  status: 'ringing' | 'active' | 'ended' | 'declined' | 'missed';
  startedAt: number;
  endedAt?: number;
  duration?: number;
}

export interface StatusEntity {
  statusId: string;
  userId: string;
  name: string;
  userPhoto: string;
  text: string;
  mediaUrl?: string | null;
  timestamp: number;
  viewsCount: number;
  viewers: string; // Comma separated user IDs
}

export interface CardEntity {
  id: number;
  cardType: 'Visa' | 'MasterCard' | 'Amex';
  cardNumber: string;
  cardHolder: string;
  expiryDate: string;
  balance: number;
  cardColorHex: string;
}

export interface TransactionEntity {
  id: number;
  title: string;
  category: 'Flight' | 'Shopping' | 'Salary' | 'Food' | 'Transfer' | 'Bills';
  dateText: string;
  amount: number;
  timestamp: number;
}

export interface ContactEntity {
  id: number;
  ownerUid: string;
  contactName: string;
  contactPhoneNumber: string;
}

export interface CallSnapshotEntity {
  id: number;
  callId: string;
  participantName: string;
  imageUrl: string;
  timestamp: number;
}
