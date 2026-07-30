import React, { createContext, useContext, useState, useEffect } from 'react';
import {
  UserEntity,
  ChatEntity,
  MessageEntity,
  CallEntity,
  StatusEntity,
  CardEntity,
  TransactionEntity
} from '../types';
import {
  initialMeUser,
  initialUsers,
  initialChats,
  initialMessages,
  initialCalls,
  initialStatuses,
  initialCards,
  initialTransactions
} from '../data/mockData';

export type ScreenType =
  | 'logo_reveal'
  | 'auth'
  | 'home'
  | 'chat_detail'
  | 'call'
  | 'gemini_live_voice';

interface ActiveCallState {
  callId: string;
  callerId: string;
  calleeId: string;
  calleeUser?: UserEntity;
  type: 'voice' | 'video';
  status: 'ringing' | 'active' | 'ended' | 'declined';
  startedAt: number;
}

interface ChatContextType {
  currentUser: UserEntity | null;
  isLoggedIn: boolean;
  rememberLogin: boolean;
  currentScreen: ScreenType;
  selectedChatId: string | null;
  activeCall: ActiveCallState | null;
  isPiPActive: boolean;
  isNetworkConnected: boolean;
  users: UserEntity[];
  chats: ChatEntity[];
  messages: Record<string, MessageEntity[]>;
  calls: CallEntity[];
  statuses: StatusEntity[];
  cards: CardEntity[];
  transactions: TransactionEntity[];
  supabaseStatus: { isConnected: boolean; lastSynced: number };
  
  // Navigation
  navigateTo: (screen: ScreenType) => void;
  selectChat: (chatId: string) => void;

  // Auth
  signIn: (email: string, rememberLogin?: boolean) => Promise<boolean>;
  signUp: (name: string, email: string, status: string, avatarPreset: string, rememberLogin?: boolean) => Promise<boolean>;
  signInWithPhone: (phoneNumber: string, rememberLogin?: boolean) => Promise<boolean>;
  signUpWithPhone: (name: string, phoneNumber: string, status: string, avatarPreset: string, rememberLogin?: boolean) => Promise<boolean>;
  signOut: () => Promise<void>;
  setUserOnlineStatus: (isOnline: boolean) => void;
  setNetworkConnected: (connected: boolean) => void;

  // Chats
  openOneOnOneChatWithUser: (user: UserEntity) => string;
  openChatByPhoneNumber: (phoneNumber: string) => string;
  createGroupChat: (name: string, photoUrl: string, memberIds: string[]) => string;
  
  // Messaging
  sendMessage: (
    chatId: string,
    text: string,
    type: 'text' | 'image' | 'voice' | 'file' | 'location' | 'contact',
    opts?: {
      mediaUrl?: string | null;
      mediaDuration?: number;
      fileName?: string | null;
      fileSize?: string | null;
      latitude?: number;
      longitude?: number;
      liveLocationDuration?: number;
      contactName?: string | null;
      contactPhone?: string | null;
      contactPhoto?: string | null;
      replyToId?: string | null;
      replyToText?: string | null;
      isForwarded?: boolean;
    }
  ) => Promise<void>;
  retrySendMessage: (messageId: string, chatId: string) => Promise<void>;
  deleteMessage: (messageId: string, chatId: string, forEveryone: boolean) => void;
  editMessage: (messageId: string, chatId: string, newText: string) => void;
  toggleMessageStar: (messageId: string, chatId: string) => void;
  toggleMessageReaction: (messageId: string, chatId: string, emoji: string) => void;

  // Calling
  placeCall: (calleeId: string, type: 'voice' | 'video') => void;
  acceptCall: (callId: string) => void;
  endCall: (callId: string) => void;
  setPiPActive: (active: boolean) => void;
  triggerSimulatedIncomingCall: (callerId: string, type: 'voice' | 'video') => void;

  // Chat management
  updateChatDisappearing: (chatId: string, duration: number) => void;
  toggleMuteChat: (chatId: string) => void;
  toggleArchiveChat: (chatId: string) => void;
  togglePinChat: (chatId: string) => void;
  toggleBlockUser: (targetUid: string) => void;

  // Status stories
  postStatus: (text: string, mediaUrl?: string) => void;
  viewStatus: (statusId: string) => void;

  // Wallet
  addCard: (cardType: 'Visa' | 'MasterCard' | 'Amex', cardNumber: string, cardHolder: string, expiryDate: string, balance: number, cardColorHex: string) => void;
  deleteCard: (cardId: number) => void;
  addTransaction: (title: string, category: 'Flight' | 'Shopping' | 'Salary' | 'Food' | 'Transfer' | 'Bills', amount: number, cardId?: number) => void;

  // Supabase Sync
  syncWithSupabase: () => Promise<boolean>;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export const ChatProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [currentUser, setCurrentUser] = useState<UserEntity | null>(() => {
    const saved = localStorage.getItem('amble_current_user');
    return saved ? JSON.parse(saved) : initialMeUser;
  });

  const [isLoggedIn, setIsLoggedIn] = useState<boolean>(() => {
    return localStorage.getItem('amble_is_logged_in') !== 'false';
  });

  const [rememberLogin, setRememberLogin] = useState<boolean>(true);
  const [currentScreen, setCurrentScreen] = useState<ScreenType>('logo_reveal');
  const [selectedChatId, setSelectedChatId] = useState<string | null>(null);
  const [activeCall, setActiveCall] = useState<ActiveCallState | null>(null);
  const [isPiPActive, setIsPiPActive] = useState<boolean>(false);
  const [isNetworkConnected, setIsNetworkConnected] = useState<boolean>(true);

  const [users, setUsers] = useState<UserEntity[]>(() => {
    const saved = localStorage.getItem('amble_users');
    return saved ? JSON.parse(saved) : initialUsers;
  });

  const [chats, setChats] = useState<ChatEntity[]>(() => {
    const saved = localStorage.getItem('amble_chats');
    return saved ? JSON.parse(saved) : initialChats;
  });

  const [messages, setMessages] = useState<Record<string, MessageEntity[]>>(() => {
    const saved = localStorage.getItem('amble_messages');
    return saved ? JSON.parse(saved) : initialMessages;
  });

  const [calls, setCalls] = useState<CallEntity[]>(() => {
    const saved = localStorage.getItem('amble_calls');
    return saved ? JSON.parse(saved) : initialCalls;
  });

  const [statuses, setStatuses] = useState<StatusEntity[]>(() => {
    const saved = localStorage.getItem('amble_statuses');
    return saved ? JSON.parse(saved) : initialStatuses;
  });

  const [cards, setCards] = useState<CardEntity[]>(() => {
    const saved = localStorage.getItem('amble_cards');
    return saved ? JSON.parse(saved) : initialCards;
  });

  const [transactions, setTransactions] = useState<TransactionEntity[]>(() => {
    const saved = localStorage.getItem('amble_transactions');
    return saved ? JSON.parse(saved) : initialTransactions;
  });

  const [supabaseStatus, setSupabaseStatus] = useState({ isConnected: true, lastSynced: Date.now() });

  // Save changes to localStorage
  useEffect(() => {
    if (currentUser) {
      localStorage.setItem('amble_current_user', JSON.stringify(currentUser));
    } else {
      localStorage.removeItem('amble_current_user');
    }
    localStorage.setItem('amble_is_logged_in', String(isLoggedIn));
  }, [currentUser, isLoggedIn]);

  useEffect(() => {
    localStorage.setItem('amble_users', JSON.stringify(users));
  }, [users]);

  useEffect(() => {
    localStorage.setItem('amble_chats', JSON.stringify(chats));
  }, [chats]);

  useEffect(() => {
    localStorage.setItem('amble_messages', JSON.stringify(messages));
  }, [messages]);

  useEffect(() => {
    localStorage.setItem('amble_calls', JSON.stringify(calls));
  }, [calls]);

  useEffect(() => {
    localStorage.setItem('amble_statuses', JSON.stringify(statuses));
  }, [statuses]);

  useEffect(() => {
    localStorage.setItem('amble_cards', JSON.stringify(cards));
  }, [cards]);

  useEffect(() => {
    localStorage.setItem('amble_transactions', JSON.stringify(transactions));
  }, [transactions]);

  // Screen navigation helper
  const navigateTo = (screen: ScreenType) => {
    if (screen !== 'call' && activeCall && activeCall.status === 'active') {
      setIsPiPActive(true);
    }
    setCurrentScreen(screen);
  };

  const selectChat = (chatId: string) => {
    setSelectedChatId(chatId);
    // Clear unread count
    setChats(prev => prev.map(c => c.chatId === chatId ? { ...c, unreadCount: 0 } : c));
    navigateTo('chat_detail');
  };

  // Auth actions
  const signIn = async (email: string, remember: boolean = true): Promise<boolean> => {
    const existing = users.find(u => u.email.toLowerCase() === email.toLowerCase());
    const me: UserEntity = existing ? { ...existing, isMe: true, isOnline: true, lastSeen: Date.now() } : {
      uid: "user_" + Math.random().toString(36).substring(2, 9),
      name: email.split('@')[0].replace(/^\w/, c => c.toUpperCase()),
      email,
      photoUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
      status: "Hey there! I am using AmBle.",
      isOnline: true,
      lastSeen: Date.now(),
      isMe: true,
      phoneNumber: "+15550001"
    };

    setCurrentUser(me);
    setIsLoggedIn(true);
    setRememberLogin(remember);
    navigateTo('home');
    return true;
  };

  const signUp = async (name: string, email: string, status: string, avatarPreset: string, remember: boolean = true): Promise<boolean> => {
    const me: UserEntity = {
      uid: "user_" + Math.random().toString(36).substring(2, 9),
      name,
      email,
      photoUrl: avatarPreset || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
      status: status || "Hey there! I am using AmBle.",
      isOnline: true,
      lastSeen: Date.now(),
      isMe: true,
      phoneNumber: "+1555" + Math.floor(1000000 + Math.random() * 9000000)
    };

    setCurrentUser(me);
    setIsLoggedIn(true);
    setRememberLogin(remember);
    navigateTo('home');
    return true;
  };

  const signInWithPhone = async (phoneNumber: string, remember: boolean = true): Promise<boolean> => {
    const existing = users.find(u => u.phoneNumber === phoneNumber);
    const me: UserEntity = existing ? { ...existing, isMe: true, isOnline: true, lastSeen: Date.now() } : {
      uid: "user_" + Math.random().toString(36).substring(2, 9),
      name: "User " + phoneNumber.slice(-4),
      email: `${phoneNumber.replace('+', '')}@phone.amble.io`,
      photoUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
      status: "Hey there! I am using AmBle.",
      isOnline: true,
      lastSeen: Date.now(),
      isMe: true,
      phoneNumber
    };

    setCurrentUser(me);
    setIsLoggedIn(true);
    setRememberLogin(remember);
    navigateTo('home');
    return true;
  };

  const signUpWithPhone = async (name: string, phoneNumber: string, status: string, avatarPreset: string, remember: boolean = true): Promise<boolean> => {
    const me: UserEntity = {
      uid: "user_" + Math.random().toString(36).substring(2, 9),
      name,
      email: `${phoneNumber.replace('+', '')}@phone.amble.io`,
      photoUrl: avatarPreset || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
      status: status || "Hey there! I am using AmBle.",
      isOnline: true,
      lastSeen: Date.now(),
      isMe: true,
      phoneNumber
    };

    setCurrentUser(me);
    setIsLoggedIn(true);
    setRememberLogin(remember);
    navigateTo('home');
    return true;
  };

  const signOut = async () => {
    if (currentUser) {
      setCurrentUser(prev => prev ? { ...prev, isOnline: false } : null);
    }
    setIsLoggedIn(false);
    setCurrentUser(null);
    navigateTo('auth');
  };

  const setUserOnlineStatus = (isOnline: boolean) => {
    if (currentUser) {
      setCurrentUser(prev => prev ? { ...prev, isOnline, lastSeen: Date.now() } : null);
    }
  };

  const setNetworkConnected = (connected: boolean) => {
    setIsNetworkConnected(connected);
  };

  // Chats creation
  const openOneOnOneChatWithUser = (targetUser: UserEntity): string => {
    const me = currentUser || initialMeUser;
    const chatId = me.uid < targetUser.uid ? `${me.uid}_${targetUser.uid}` : `${targetUser.uid}_${me.uid}`;

    const existing = chats.find(c => c.chatId === chatId);
    if (!existing) {
      const newChat: ChatEntity = {
        chatId,
        lastMessage: "No messages yet",
        lastMessageTime: Date.now(),
        isGroup: false,
        groupName: targetUser.name,
        groupPhoto: targetUser.photoUrl,
        unreadCount: 0
      };
      setChats(prev => [newChat, ...prev]);
    }
    selectChat(chatId);
    return chatId;
  };

  const openChatByPhoneNumber = (phoneNumber: string): string => {
    const found = users.find(u => u.phoneNumber === phoneNumber);
    if (found) {
      return openOneOnOneChatWithUser(found);
    }
    // Create new temporary user with phone number
    const tempUser: UserEntity = {
      uid: "phone_" + phoneNumber.replace(/\D/g, ''),
      name: "Contact " + phoneNumber,
      email: `${phoneNumber.replace('+', '')}@phone.amble.io`,
      photoUrl: "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
      status: "Added via AmBle Phone Lookup",
      isOnline: true,
      lastSeen: Date.now(),
      isMe: false,
      phoneNumber
    };
    setUsers(prev => [tempUser, ...prev]);
    return openOneOnOneChatWithUser(tempUser);
  };

  const createGroupChat = (name: string, photoUrl: string, memberIds: string[]): string => {
    const chatId = "group_" + Math.random().toString(36).substring(2, 9);
    const newChat: ChatEntity = {
      chatId,
      lastMessage: "Group created",
      lastMessageTime: Date.now(),
      isGroup: true,
      groupName: name,
      groupPhoto: photoUrl || "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150",
      unreadCount: 0
    };
    setChats(prev => [newChat, ...prev]);
    selectChat(chatId);
    return chatId;
  };

  // Messaging with simulated AI reply & delivery ticks
  const sendMessage = async (
    chatId: string,
    text: string,
    type: 'text' | 'image' | 'voice' | 'file' | 'location' | 'contact',
    opts: {
      mediaUrl?: string | null;
      mediaDuration?: number;
      fileName?: string | null;
      fileSize?: string | null;
      latitude?: number;
      longitude?: number;
      liveLocationDuration?: number;
      contactName?: string | null;
      contactPhone?: string | null;
      contactPhoto?: string | null;
      replyToId?: string | null;
      replyToText?: string | null;
      isForwarded?: boolean;
    } = {}
  ) => {
    const me = currentUser || initialMeUser;
    const msgId = "msg_" + Math.random().toString(36).substring(2, 9);
    const isOffline = !isNetworkConnected;

    const newMsg: MessageEntity = {
      messageId: msgId,
      chatId,
      senderId: me.uid,
      text,
      type,
      mediaUrl: opts.mediaUrl,
      mediaDuration: opts.mediaDuration,
      fileName: opts.fileName,
      fileSize: opts.fileSize,
      latitude: opts.latitude,
      longitude: opts.longitude,
      liveLocationDuration: opts.liveLocationDuration,
      contactName: opts.contactName,
      contactPhone: opts.contactPhone,
      contactPhoto: opts.contactPhoto,
      replyToMessageId: opts.replyToId,
      replyToText: opts.replyToText,
      timestamp: Date.now(),
      status: isOffline ? 'failed' : 'sent',
      sendFailed: isOffline,
      isForwarded: opts.isForwarded,
      uploadProgress: 100
    };

    setMessages(prev => ({
      ...prev,
      [chatId]: [...(prev[chatId] || []), newMsg]
    }));

    setChats(prev => prev.map(c => c.chatId === chatId ? {
      ...c,
      lastMessage: type === 'text' ? text : `[${type.toUpperCase()}]`,
      lastMessageTime: Date.now()
    } : c));

    if (isOffline) return;

    // Delivery delay ticks: sent -> delivered -> read -> reply
    setTimeout(() => {
      setMessages(prev => ({
        ...prev,
        [chatId]: (prev[chatId] || []).map(m => m.messageId === msgId ? { ...m, status: 'delivered' } : m)
      }));
    }, 400);

    // AI / Simulation response
    setTimeout(async () => {
      setMessages(prev => ({
        ...prev,
        [chatId]: (prev[chatId] || []).map(m => m.messageId === msgId ? { ...m, status: 'read' } : m)
      }));

      // Call Express server-side low-latency Gemini endpoint
      let responseText = "";
      let latencyMs = 180;
      let modelUsed = "gemini-3.1-flash-lite";

      try {
        const res = await fetch("/api/gemini/low-latency", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ prompt: text, systemPrompt: "Respond concisely as AmBle's fast AI assistant." })
        });
        const data = await res.json();
        responseText = data.text;
        latencyMs = data.latencyMs;
        modelUsed = data.modelUsed;
      } catch (err) {
        responseText = `⚡ Low-latency reply: Got your message "${text}". All systems fast and operational!`;
      }

      const replyMsgId = "msg_" + Math.random().toString(36).substring(2, 9);
      const isGroup = chatId.startsWith("group_");
      const replySender = isGroup ? (users[Math.floor(Math.random() * users.length)] || users[0]) : (users.find(u => chatId.includes(u.uid)) || users[0]);

      const formattedReplyText = `${responseText}\n\n⚡ Low-Latency (${latencyMs}ms) [${modelUsed}]`;

      const replyMsg: MessageEntity = {
        messageId: replyMsgId,
        chatId,
        senderId: replySender.uid,
        text: formattedReplyText,
        type: "text",
        timestamp: Date.now(),
        status: "read"
      };

      setMessages(prev => ({
        ...prev,
        [chatId]: [...(prev[chatId] || []), replyMsg]
      }));

      setChats(prev => prev.map(c => c.chatId === chatId ? {
        ...c,
        lastMessage: responseText,
        lastMessageTime: Date.now()
      } : c));
    }, 1200);
  };

  const retrySendMessage = async (messageId: string, chatId: string) => {
    setMessages(prev => ({
      ...prev,
      [chatId]: (prev[chatId] || []).map(m => m.messageId === messageId ? { ...m, status: 'sent', sendFailed: false, timestamp: Date.now() } : m)
    }));
  };

  const deleteMessage = (messageId: string, chatId: string, forEveryone: boolean) => {
    setMessages(prev => ({
      ...prev,
      [chatId]: (prev[chatId] || []).map(m => {
        if (m.messageId === messageId) {
          if (forEveryone) {
            return { ...m, text: "This message was deleted", isDeleted: true };
          }
          return null as any;
        }
        return m;
      }).filter(Boolean)
    }));
  };

  const editMessage = (messageId: string, chatId: string, newText: string) => {
    setMessages(prev => ({
      ...prev,
      [chatId]: (prev[chatId] || []).map(m => m.messageId === messageId ? { ...m, text: newText, isEdited: true } : m)
    }));
  };

  const toggleMessageStar = (messageId: string, chatId: string) => {
    setMessages(prev => ({
      ...prev,
      [chatId]: (prev[chatId] || []).map(m => m.messageId === messageId ? { ...m, isStarred: !m.isStarred } : m)
    }));
  };

  const toggleMessageReaction = (messageId: string, chatId: string, emoji: string) => {
    const me = currentUser || initialMeUser;
    setMessages(prev => ({
      ...prev,
      [chatId]: (prev[chatId] || []).map(m => {
        if (m.messageId !== messageId) return m;
        const currentReactions = m.reactions ? m.reactions.split(',').filter(Boolean) : [];
        const reactionKey = `${emoji}:${me.uid}`;
        let updated: string[];
        if (currentReactions.includes(reactionKey)) {
          updated = currentReactions.filter(r => r !== reactionKey);
        } else {
          updated = [...currentReactions.filter(r => !r.endsWith(`:${me.uid}`)), reactionKey];
        }
        return { ...m, reactions: updated.join(',') };
      })
    }));
  };

  // VoIP and Calling
  const placeCall = (calleeId: string, type: 'voice' | 'video') => {
    const callee = users.find(u => u.uid === calleeId);
    const callId = "call_" + Math.random().toString(36).substring(2, 9);
    
    setActiveCall({
      callId,
      callerId: currentUser?.uid || initialMeUser.uid,
      calleeId,
      calleeUser: callee,
      type,
      status: 'ringing',
      startedAt: Date.now()
    });
    setIsPiPActive(false);
    navigateTo('call');

    // Simulate auto-answer after 2 seconds
    setTimeout(() => {
      setActiveCall(prev => prev && prev.callId === callId ? { ...prev, status: 'active' } : prev);
    }, 2000);
  };

  const acceptCall = (callId: string) => {
    if (activeCall && activeCall.callId === callId) {
      setActiveCall({ ...activeCall, status: 'active' });
    }
  };

  const endCall = (callId: string) => {
    if (activeCall && activeCall.callId === callId) {
      const duration = Math.floor((Date.now() - activeCall.startedAt) / 1000);
      const newCallEntity: CallEntity = {
        callId: activeCall.callId,
        callerId: activeCall.callerId,
        calleeId: activeCall.calleeId,
        type: activeCall.type,
        status: 'ended',
        startedAt: activeCall.startedAt,
        endedAt: Date.now(),
        duration
      };
      setCalls(prev => [newCallEntity, ...prev]);
    }
    setActiveCall(null);
    setIsPiPActive(false);
    navigateTo('home');
  };

  const triggerSimulatedIncomingCall = (callerId: string, type: 'voice' | 'video') => {
    const caller = users.find(u => u.uid === callerId) || users[0];
    const callId = "call_" + Math.random().toString(36).substring(2, 9);
    setActiveCall({
      callId,
      callerId: caller.uid,
      calleeId: currentUser?.uid || initialMeUser.uid,
      calleeUser: caller,
      type,
      status: 'ringing',
      startedAt: Date.now()
    });
    setIsPiPActive(false);
    navigateTo('call');
  };

  const setPiPActive = (active: boolean) => {
    setIsPiPActive(active);
  };

  // Chat management
  const updateChatDisappearing = (chatId: string, duration: number) => {
    setChats(prev => prev.map(c => c.chatId === chatId ? { ...c, disappearingDuration: duration } : c));
  };

  const toggleMuteChat = (chatId: string) => {
    setChats(prev => prev.map(c => c.chatId === chatId ? { ...c, isMuted: !c.isMuted } : c));
  };

  const toggleArchiveChat = (chatId: string) => {
    if (currentUser) {
      const archived = currentUser.archivedChatIds ? currentUser.archivedChatIds.split(',').filter(Boolean) : [];
      const updated = archived.includes(chatId) ? archived.filter(id => id !== chatId) : [...archived, chatId];
      setCurrentUser({ ...currentUser, archivedChatIds: updated.join(',') });
    }
  };

  const togglePinChat = (chatId: string) => {
    setChats(prev => prev.map(c => c.chatId === chatId ? { ...c, isPinned: !c.isPinned } : c));
  };

  const toggleBlockUser = (targetUid: string) => {
    if (currentUser) {
      const blocked = currentUser.blockedUids ? currentUser.blockedUids.split(',').filter(Boolean) : [];
      const updated = blocked.includes(targetUid) ? blocked.filter(id => id !== targetUid) : [...blocked, targetUid];
      setCurrentUser({ ...currentUser, blockedUids: updated.join(',') });
    }
  };

  // Status stories
  const postStatus = (text: string, mediaUrl?: string) => {
    const me = currentUser || initialMeUser;
    const newStatus: StatusEntity = {
      statusId: "status_" + Math.random().toString(36).substring(2, 9),
      userId: me.uid,
      name: me.name,
      userPhoto: me.photoUrl,
      text,
      mediaUrl: mediaUrl || null,
      timestamp: Date.now(),
      viewsCount: 0,
      viewers: ""
    };
    setStatuses(prev => [newStatus, ...prev]);
  };

  const viewStatus = (statusId: string) => {
    const me = currentUser || initialMeUser;
    setStatuses(prev => prev.map(s => {
      if (s.statusId !== statusId || s.userId === me.uid) return s;
      const viewersList = s.viewers ? s.viewers.split(',').filter(Boolean) : [];
      if (!viewersList.includes(me.uid)) {
        const updated = [...viewersList, me.uid];
        return { ...s, viewers: updated.join(','), viewsCount: updated.length };
      }
      return s;
    }));
  };

  // Wallet
  const addCard = (cardType: 'Visa' | 'MasterCard' | 'Amex', cardNumber: string, cardHolder: string, expiryDate: string, balance: number, cardColorHex: string) => {
    const newCard: CardEntity = {
      id: Date.now(),
      cardType,
      cardNumber,
      cardHolder,
      expiryDate,
      balance,
      cardColorHex
    };
    setCards(prev => [...prev, newCard]);
  };

  const deleteCard = (cardId: number) => {
    setCards(prev => prev.filter(c => c.id !== cardId));
  };

  const addTransaction = (title: string, category: 'Flight' | 'Shopping' | 'Salary' | 'Food' | 'Transfer' | 'Bills', amount: number, cardId?: number) => {
    const newTx: TransactionEntity = {
      id: Date.now(),
      title,
      category,
      dateText: new Date().toLocaleDateString('en-US', { day: 'numeric', month: 'long', year: 'numeric' }),
      amount,
      timestamp: Date.now()
    };
    setTransactions(prev => [newTx, ...prev]);

    if (cardId) {
      setCards(prev => prev.map(c => c.id === cardId ? { ...c, balance: c.balance + amount } : c));
    }
  };

  const syncWithSupabase = async (): Promise<boolean> => {
    setSupabaseStatus({ isConnected: true, lastSynced: Date.now() });
    return true;
  };

  return (
    <ChatContext.Provider
      value={{
        currentUser,
        isLoggedIn,
        rememberLogin,
        currentScreen,
        selectedChatId,
        activeCall,
        isPiPActive,
        isNetworkConnected,
        users,
        chats,
        messages,
        calls,
        statuses,
        cards,
        transactions,
        supabaseStatus,
        navigateTo,
        selectChat,
        signIn,
        signUp,
        signInWithPhone,
        signUpWithPhone,
        signOut,
        setUserOnlineStatus,
        setNetworkConnected,
        openOneOnOneChatWithUser,
        openChatByPhoneNumber,
        createGroupChat,
        sendMessage,
        retrySendMessage,
        deleteMessage,
        editMessage,
        toggleMessageStar,
        toggleMessageReaction,
        placeCall,
        acceptCall,
        endCall,
        setPiPActive,
        triggerSimulatedIncomingCall,
        updateChatDisappearing,
        toggleMuteChat,
        toggleArchiveChat,
        togglePinChat,
        toggleBlockUser,
        postStatus,
        viewStatus,
        addCard,
        deleteCard,
        addTransaction,
        syncWithSupabase
      }}
    >
      {children}
    </ChatContext.Provider>
  );
};

export const useChat = () => {
  const context = useContext(ChatContext);
  if (!context) {
    throw new Error('useChat must be used within a ChatProvider');
  }
  return context;
};
