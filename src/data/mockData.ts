import { UserEntity, ChatEntity, MessageEntity, CallEntity, StatusEntity, CardEntity, TransactionEntity } from '../types';

export const initialMeUser: UserEntity = {
  uid: "alex_mercer_me",
  name: "Alex Mercer",
  email: "alex.mercer@chatwave.io",
  photoUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
  status: "Connecting the world with AmBle ✨",
  isOnline: true,
  lastSeen: Date.now(),
  isMe: true,
  phoneNumber: "+15550100",
  lastSeenPrivacy: "everyone",
  blockedUids: "",
  archivedChatIds: "",
  mutedChatIds: ""
};

export const initialUsers: UserEntity[] = [
  {
    uid: "sarah_uid",
    name: "Sarah Jenkins",
    email: "sarah.j@chatwave.io",
    photoUrl: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    status: "Productivity is key 🚀 | Coding is art",
    isOnline: true,
    lastSeen: Date.now(),
    isMe: false,
    phoneNumber: "+15550101"
  },
  {
    uid: "alex_uid",
    name: "Alex Rivera",
    email: "alex.r@chatwave.io",
    photoUrl: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    status: "Tech Lead @ ChatWave | Coffee enthusiast ☕",
    isOnline: false,
    lastSeen: Date.now() - 1800000,
    isMe: false,
    phoneNumber: "+15550102"
  },
  {
    uid: "elena_uid",
    name: "Elena Petrova",
    email: "elena.p@chatwave.io",
    photoUrl: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    status: "Designing the future ✨ | UI/UX",
    isOnline: true,
    lastSeen: Date.now(),
    isMe: false,
    phoneNumber: "+15550103"
  }
];

export const initialChats: ChatEntity[] = [
  {
    chatId: "alex_mercer_me_sarah_uid",
    lastMessage: "Hey! Did you check out the new WebRTC audio stream?",
    lastMessageTime: Date.now() - 300000,
    isGroup: false,
    groupName: "Sarah Jenkins",
    groupPhoto: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    unreadCount: 1,
    isPinned: true
  },
  {
    chatId: "alex_mercer_me_alex_uid",
    lastMessage: "The low-latency Gemini responses are super fast!",
    lastMessageTime: Date.now() - 3600000,
    isGroup: false,
    groupName: "Alex Rivera",
    groupPhoto: "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
    unreadCount: 0
  },
  {
    chatId: "group_amble_core",
    lastMessage: "Group created",
    lastMessageTime: Date.now() - 86400000,
    isGroup: true,
    groupName: "AmBle Core Team",
    groupPhoto: "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
    unreadCount: 0
  }
];

export const initialMessages: Record<string, MessageEntity[]> = {
  "alex_mercer_me_sarah_uid": [
    {
      messageId: "msg_1",
      chatId: "alex_mercer_me_sarah_uid",
      senderId: "sarah_uid",
      text: "Hey Alex! Welcome to AmBle.",
      type: "text",
      timestamp: Date.now() - 600000,
      status: "read"
    },
    {
      messageId: "msg_2",
      chatId: "alex_mercer_me_sarah_uid",
      senderId: "alex_mercer_me",
      text: "Thanks Sarah! Testing voice notes and low-latency AI responses now.",
      type: "text",
      timestamp: Date.now() - 450000,
      status: "read"
    },
    {
      messageId: "msg_3",
      chatId: "alex_mercer_me_sarah_uid",
      senderId: "sarah_uid",
      text: "Hey! Did you check out the new WebRTC audio stream?",
      type: "text",
      timestamp: Date.now() - 300000,
      status: "delivered"
    }
  ],
  "alex_mercer_me_alex_uid": [
    {
      messageId: "msg_a1",
      chatId: "alex_mercer_me_alex_uid",
      senderId: "alex_uid",
      text: "The low-latency Gemini responses are super fast!",
      type: "text",
      timestamp: Date.now() - 3600000,
      status: "read"
    }
  ],
  "group_amble_core": [
    {
      messageId: "msg_g1",
      chatId: "group_amble_core",
      senderId: "alex_mercer_me",
      text: "Alex Mercer created the group \"AmBle Core Team\"",
      type: "text",
      timestamp: Date.now() - 86400000,
      status: "read"
    }
  ]
};

export const initialCalls: CallEntity[] = [
  {
    callId: "call_1",
    callerId: "sarah_uid",
    calleeId: "alex_mercer_me",
    type: "video",
    status: "ended",
    startedAt: Date.now() - 7200000,
    endedAt: Date.now() - 7080000,
    duration: 120
  },
  {
    callId: "call_2",
    callerId: "alex_mercer_me",
    calleeId: "alex_uid",
    type: "voice",
    status: "ended",
    startedAt: Date.now() - 86400000,
    endedAt: Date.now() - 86160000,
    duration: 240
  }
];

export const initialStatuses: StatusEntity[] = [
  {
    statusId: "status_1",
    userId: "sarah_uid",
    name: "Sarah Jenkins",
    userPhoto: "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
    text: "Building ultra-responsive WebRTC channels on AmBle 🚀",
    mediaUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500",
    timestamp: Date.now() - 14400000,
    viewsCount: 12,
    viewers: "alex_mercer_me,alex_uid"
  },
  {
    statusId: "status_2",
    userId: "elena_uid",
    name: "Elena Petrova",
    userPhoto: "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
    text: "Glassmorphism design tokens finalized ✨",
    mediaUrl: "https://images.unsplash.com/photo-1550745165-9bc0b252726f?w=500",
    timestamp: Date.now() - 28800000,
    viewsCount: 8,
    viewers: "alex_mercer_me"
  }
];

export const initialCards: CardEntity[] = [
  {
    id: 1,
    cardType: "Visa",
    cardNumber: "4850 **** **** 7459",
    cardHolder: "Alex Mercer",
    expiryDate: "04/29",
    balance: 1200.00,
    cardColorHex: "#1A51A6"
  },
  {
    id: 2,
    cardType: "MasterCard",
    cardNumber: "5234 **** **** 9102",
    cardHolder: "Alex Mercer",
    expiryDate: "12/28",
    balance: 4850.50,
    cardColorHex: "#FF5F00"
  }
];

export const initialTransactions: TransactionEntity[] = [
  {
    id: 1,
    title: "Flight Booking",
    category: "Flight",
    dateText: "3rd August 2026",
    amount: -100.20,
    timestamp: Date.now() - 86400000
  },
  {
    id: 2,
    title: "Supermarket Shopping",
    category: "Shopping",
    dateText: "2nd August 2026",
    amount: -45.50,
    timestamp: Date.now() - 172800000
  },
  {
    id: 3,
    title: "Monthly Salary",
    category: "Salary",
    dateText: "1st August 2026",
    amount: 2500.00,
    timestamp: Date.now() - 259200000
  }
];
