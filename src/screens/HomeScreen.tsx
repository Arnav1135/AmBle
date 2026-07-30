import React, { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { AntigravityHeaderBadge } from '../components/AntigravityHeaderBadge';
import { AntigravityIntegrationCard } from '../components/AntigravityIntegrationCard';
import { InAppUpdateBanner } from '../components/InAppUpdateBanner';
import { UserProfileDialog } from '../components/UserProfileDialog';
import { SupabaseIntegrationDialog } from '../components/SupabaseIntegrationDialog';

import {
  MessageSquare,
  Phone,
  CircleDashed,
  Wallet,
  Search,
  Plus,
  Mic,
  Video,
  User,
  Radio,
  Clock,
  Sparkles,
  CreditCard,
  PlusCircle,
  X,
  Send,
  MoreVertical,
  CheckCheck,
  Zap,
  ArrowUpRight,
  ArrowDownLeft,
  ChevronRight,
  Database
} from 'lucide-react';

export const HomeScreen: React.FC = () => {
  const {
    currentUser,
    chats,
    users,
    calls,
    statuses,
    cards,
    transactions,
    selectChat,
    openOneOnOneChatWithUser,
    openChatByPhoneNumber,
    createGroupChat,
    placeCall,
    triggerSimulatedIncomingCall,
    postStatus,
    viewStatus,
    addCard,
    deleteCard,
    addTransaction,
    navigateTo,
    isNetworkConnected,
    setNetworkConnected
  } = useChat();

  const [activeTab, setActiveTab] = useState<'chats' | 'calls' | 'status' | 'wallet'>('chats');
  const [searchQuery, setSearchQuery] = useState('');
  const [showProfileModal, setShowProfileModal] = useState(false);
  const [showSupabaseModal, setShowSupabaseModal] = useState(false);
  const [showNewChatModal, setShowNewChatModal] = useState(false);
  const [showNewGroupModal, setShowNewGroupModal] = useState(false);
  const [showNewStatusModal, setShowNewStatusModal] = useState(false);
  const [showAddCardModal, setShowAddCardModal] = useState(false);
  const [showAddTxModal, setShowAddTxModal] = useState(false);

  // New Group State
  const [groupName, setGroupName] = useState('');
  const [selectedMembers, setSelectedMembers] = useState<string[]>([]);

  // New Status State
  const [statusText, setStatusText] = useState('');
  const [statusMediaUrl, setStatusMediaUrl] = useState('');

  // New Card State
  const [cardType, setCardType] = useState<'Visa' | 'MasterCard' | 'Amex'>('Visa');
  const [cardNumber, setCardNumber] = useState('');
  const [cardHolder, setCardHolder] = useState(currentUser?.name || '');
  const [expiryDate, setExpiryDate] = useState('12/28');
  const [balance, setBalance] = useState('500.00');

  // New Transaction State
  const [txTitle, setTxTitle] = useState('');
  const [txCategory, setTxCategory] = useState<'Flight' | 'Shopping' | 'Salary' | 'Food' | 'Transfer' | 'Bills'>('Shopping');
  const [txAmount, setTxAmount] = useState('50.00');

  // Filtered chats
  const filteredChats = chats.filter(c => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return c.groupName.toLowerCase().includes(q) || c.lastMessage.toLowerCase().includes(q);
  });

  // Filtered users for search / new chat
  const filteredUsers = users.filter(u => {
    if (!searchQuery) return true;
    const q = searchQuery.toLowerCase();
    return u.name.toLowerCase().includes(q) || u.phoneNumber?.includes(q) || u.email.toLowerCase().includes(q);
  });

  const handleStartPhoneChat = () => {
    if (!searchQuery) return;
    openChatByPhoneNumber(searchQuery);
    setSearchQuery('');
  };

  const handleCreateGroup = (e: React.FormEvent) => {
    e.preventDefault();
    if (!groupName) return;
    createGroupChat(groupName, "https://images.unsplash.com/photo-1522071820081-009f0129c71c?w=150", selectedMembers);
    setShowNewGroupModal(false);
    setGroupName('');
    setSelectedMembers([]);
  };

  const handlePostStatus = (e: React.FormEvent) => {
    e.preventDefault();
    if (!statusText) return;
    postStatus(statusText, statusMediaUrl);
    setShowNewStatusModal(false);
    setStatusText('');
    setStatusMediaUrl('');
  };

  const handleAddCard = (e: React.FormEvent) => {
    e.preventDefault();
    if (!cardNumber) return;
    const colorHex = cardType === 'Visa' ? '#1A51A6' : cardType === 'MasterCard' ? '#FF5F00' : '#006FCF';
    addCard(cardType, cardNumber, cardHolder, expiryDate, parseFloat(balance) || 0, colorHex);
    setShowAddCardModal(false);
    setCardNumber('');
  };

  const handleAddTransaction = (e: React.FormEvent) => {
    e.preventDefault();
    if (!txTitle) return;
    addTransaction(txTitle, txCategory, parseFloat(txAmount) || 0);
    setShowAddTxModal(false);
    setTxTitle('');
  };

  return (
    <div className="min-h-screen w-full bg-[#EAF2FB] flex flex-col max-w-lg mx-auto relative shadow-2xl overflow-hidden border-x border-slate-200">
      {/* Top Header */}
      <header className="p-4 bg-white/80 backdrop-blur-md sticky top-0 z-30 border-b border-slate-200/80">
        <div className="flex items-center justify-between mb-3">
          <div className="flex items-center gap-2.5">
            <div
              onClick={() => setShowProfileModal(true)}
              className="relative cursor-pointer group"
            >
              <img
                src={currentUser?.photoUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"}
                alt="Profile"
                className="w-10 h-10 rounded-full object-cover border-2 border-blue-500 shadow-sm group-hover:scale-105 transition-transform"
              />
              <span className="absolute bottom-0 right-0 w-3 h-3 bg-emerald-500 rounded-full border-2 border-white" />
            </div>
            <div>
              <h1 className="text-lg font-extrabold text-[#1B2A5E] leading-none">AmBle</h1>
              <p className="text-[11px] text-slate-500 font-medium">Hello, {currentUser?.name.split(' ')[0]}</p>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <AntigravityHeaderBadge onClick={() => alert("Antigravity Core Engine active with WebRTC P2P mesh!")} />
            <button
              onClick={() => setNetworkConnected(!isNetworkConnected)}
              className={`p-2 rounded-xl text-xs font-bold flex items-center gap-1 transition-all ${
                isNetworkConnected ? 'bg-emerald-50 text-emerald-700 border border-emerald-200' : 'bg-red-50 text-red-600 border border-red-200'
              }`}
              title={isNetworkConnected ? "Online mode" : "Simulate Offline mode"}
            >
              <Radio className="w-3.5 h-3.5" />
              {isNetworkConnected ? 'Online' : 'Offline'}
            </button>
          </div>
        </div>

        {/* Gemini Live Voice Mode Prominent Banner */}
        <div
          onClick={() => navigateTo('gemini_live_voice')}
          className="w-full p-3 rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 border border-cyan-400/40 text-white cursor-pointer hover:border-cyan-400 transition-all shadow-md group flex items-center justify-between"
        >
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-2xl bg-gradient-to-tr from-cyan-400 to-indigo-500 p-0.5 shadow-lg shadow-cyan-500/20">
              <div className="w-full h-full bg-[#0F172A] rounded-[14px] flex items-center justify-center text-cyan-300">
                <Mic className="w-4 h-4 animate-pulse" />
              </div>
            </div>
            <div>
              <div className="flex items-center gap-1.5">
                <span className="text-xs font-bold text-white">Gemini Live Voice</span>
                <span className="px-1.5 py-0.5 rounded-full bg-emerald-500/20 text-emerald-400 text-[9px] font-black tracking-wider">
                  LIVE API
                </span>
              </div>
              <p className="text-[10px] text-slate-300">
                Real-time voice stream • gemini-3.1-flash-live-preview
              </p>
            </div>
          </div>
          <ChevronRight className="w-4 h-4 text-cyan-400 group-hover:translate-x-1 transition-transform" />
        </div>

        {/* Search Bar */}
        <div className="mt-3 relative">
          <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
          <input
            type="text"
            placeholder="Search chats, contacts or phone number (+1555...)"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full bg-slate-100/90 border border-slate-200/80 rounded-2xl pl-10 pr-10 py-2 text-xs text-slate-800 placeholder-slate-400 font-medium focus:outline-none focus:border-blue-500"
          />
          {searchQuery && (
            <button
              onClick={() => setSearchQuery('')}
              className="absolute right-3 top-2.5 text-slate-400 hover:text-slate-600"
            >
              <X className="w-4 h-4" />
            </button>
          )}
        </div>
      </header>

      {/* Main Content Body */}
      <main className="flex-1 overflow-y-auto p-4 pb-28 space-y-4">
        <InAppUpdateBanner />

        {/* CHATS TAB */}
        {activeTab === 'chats' && (
          <div className="space-y-4">
            {/* Phone search direct match banner */}
            {searchQuery && searchQuery.startsWith('+') && (
              <div
                onClick={handleStartPhoneChat}
                className="p-3 bg-blue-50 border border-blue-200 rounded-2xl text-xs font-bold text-blue-800 flex items-center justify-between cursor-pointer hover:bg-blue-100 transition-colors"
              >
                <span>Direct lookup & start chat with {searchQuery}</span>
                <Plus className="w-4 h-4 text-blue-600" />
              </div>
            )}

            <div className="flex items-center justify-between">
              <h2 className="text-xs font-extrabold text-slate-500 uppercase tracking-wider">
                Conversations ({filteredChats.length})
              </h2>
              <div className="flex gap-2">
                <button
                  onClick={() => setShowNewGroupModal(true)}
                  className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"
                >
                  <PlusCircle className="w-3.5 h-3.5" /> Group
                </button>
                <button
                  onClick={() => setShowNewChatModal(true)}
                  className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"
                >
                  <Plus className="w-3.5 h-3.5" /> New Chat
                </button>
              </div>
            </div>

            {filteredChats.length === 0 ? (
              <div className="text-center py-12 text-slate-400">
                <MessageSquare className="w-10 h-10 mx-auto mb-2 opacity-40" />
                <p className="text-xs font-semibold">No chats found</p>
                <p className="text-[11px] mt-0.5">Start a conversation or search by phone number.</p>
              </div>
            ) : (
              <div className="space-y-2">
                {filteredChats.map(chat => (
                  <div
                    key={chat.chatId}
                    onClick={() => selectChat(chat.chatId)}
                    className={`p-3 rounded-2xl bg-white border transition-all cursor-pointer hover:shadow-md flex items-center gap-3 ${
                      chat.isPinned ? 'border-blue-200 bg-blue-50/30' : 'border-slate-200/80'
                    }`}
                  >
                    <div className="relative shrink-0">
                      <img
                        src={chat.groupPhoto}
                        alt={chat.groupName}
                        className="w-12 h-12 rounded-full object-cover border border-slate-200"
                      />
                      {chat.isPinned && (
                        <span className="absolute -top-1 -right-1 text-[10px] bg-blue-600 text-white p-0.5 rounded-full">
                          📌
                        </span>
                      )}
                    </div>

                    <div className="flex-1 overflow-hidden">
                      <div className="flex items-center justify-between mb-0.5">
                        <h3 className="text-xs font-bold text-slate-900 truncate">{chat.groupName}</h3>
                        <span className="text-[10px] text-slate-400 font-medium shrink-0">
                          {new Date(chat.lastMessageTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </span>
                      </div>
                      <p className="text-xs text-slate-500 truncate">{chat.lastMessage}</p>
                    </div>

                    {chat.unreadCount > 0 && (
                      <span className="w-5 h-5 rounded-full bg-blue-600 text-white text-[10px] font-extrabold flex items-center justify-center shrink-0 shadow-sm">
                        {chat.unreadCount}
                      </span>
                    )}
                  </div>
                ))}
              </div>
            )}

            <AntigravityIntegrationCard />
          </div>
        )}

        {/* CALLS TAB */}
        {activeTab === 'calls' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-extrabold text-slate-500 uppercase tracking-wider">
                Call History ({calls.length})
              </h2>
              <button
                onClick={() => triggerSimulatedIncomingCall(users[0]?.uid, 'video')}
                className="px-2.5 py-1 bg-emerald-100 hover:bg-emerald-200 text-emerald-800 rounded-xl text-xs font-bold transition-colors flex items-center gap-1"
              >
                <Phone className="w-3.5 h-3.5" /> Simulate Incoming
              </button>
            </div>

            {/* STUN / TURN Server Diagnostics Node Switcher */}
            <div className="p-3 bg-slate-900 text-white rounded-2xl text-xs space-y-2 border border-cyan-400/30">
              <div className="flex items-center justify-between">
                <span className="font-bold text-cyan-300 flex items-center gap-1">
                  <Radio className="w-3.5 h-3.5" /> WebRTC Coturn Node
                </span>
                <span className="text-[10px] text-emerald-400 font-mono">stun.l.google.com:19302</span>
              </div>
              <p className="text-[11px] text-slate-300">
                P2P NAT Traversal enabled. Direct UDP audio/video streaming ready.
              </p>
            </div>

            <div className="space-y-2">
              {calls.map(call => {
                const partner = users.find(u => u.uid === (call.callerId === currentUser?.uid ? call.calleeId : call.callerId)) || users[0];
                return (
                  <div
                    key={call.callId}
                    className="p-3 bg-white border border-slate-200/80 rounded-2xl flex items-center justify-between"
                  >
                    <div className="flex items-center gap-3">
                      <img
                        src={partner.photoUrl}
                        alt={partner.name}
                        className="w-11 h-11 rounded-full object-cover border border-slate-200"
                      />
                      <div>
                        <h4 className="text-xs font-bold text-slate-900">{partner.name}</h4>
                        <p className="text-[10px] text-slate-500 flex items-center gap-1 mt-0.5">
                          {call.type === 'video' ? <Video className="w-3 h-3 text-blue-500" /> : <Phone className="w-3 h-3 text-emerald-500" />}
                          <span>{call.type.toUpperCase()} • {call.duration ? `${call.duration}s` : 'Ended'}</span>
                          <span>• {new Date(call.startedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                        </p>
                      </div>
                    </div>

                    <div className="flex gap-1">
                      <button
                        onClick={() => placeCall(partner.uid, 'voice')}
                        className="p-2 bg-slate-100 hover:bg-slate-200 rounded-xl text-slate-700"
                        title="Voice Call"
                      >
                        <Phone className="w-4 h-4 text-emerald-600" />
                      </button>
                      <button
                        onClick={() => placeCall(partner.uid, 'video')}
                        className="p-2 bg-slate-100 hover:bg-slate-200 rounded-xl text-slate-700"
                        title="Video Call"
                      >
                        <Video className="w-4 h-4 text-blue-600" />
                      </button>
                    </div>
                  </div>
                );
              })}
            </div>
          </div>
        )}

        {/* STATUS TAB */}
        {activeTab === 'status' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-extrabold text-slate-500 uppercase tracking-wider">
                Status Stories
              </h2>
              <button
                onClick={() => setShowNewStatusModal(true)}
                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition-all shadow-sm flex items-center gap-1"
              >
                <Plus className="w-4 h-4" /> Add Story
              </button>
            </div>

            {/* My Status header */}
            <div className="p-3 bg-white border border-slate-200/80 rounded-2xl flex items-center gap-3">
              <div className="relative cursor-pointer" onClick={() => setShowNewStatusModal(true)}>
                <img
                  src={currentUser?.photoUrl}
                  alt="My Status"
                  className="w-12 h-12 rounded-full object-cover border-2 border-blue-500"
                />
                <span className="absolute bottom-0 right-0 p-0.5 bg-blue-600 text-white rounded-full">
                  <Plus className="w-3 h-3" />
                </span>
              </div>
              <div className="flex-1">
                <h4 className="text-xs font-bold text-slate-900">My Status</h4>
                <p className="text-[11px] text-slate-500">Tap to post a status story (expires in 24h)</p>
              </div>
            </div>

            <h3 className="text-[11px] font-bold text-slate-400 uppercase tracking-wider pt-2">Recent Updates</h3>

            <div className="space-y-3">
              {statuses.map(st => (
                <div
                  key={st.statusId}
                  onClick={() => {
                    viewStatus(st.statusId);
                    alert(`Viewing Story by ${st.name}: "${st.text}"\nViews: ${st.viewsCount}`);
                  }}
                  className="p-3 bg-white border border-slate-200/80 rounded-2xl cursor-pointer hover:shadow-md transition-all flex items-start gap-3"
                >
                  <img
                    src={st.userPhoto}
                    alt={st.name}
                    className="w-11 h-11 rounded-full object-cover border-2 border-emerald-500 p-0.5"
                  />
                  <div className="flex-1">
                    <div className="flex items-center justify-between">
                      <h4 className="text-xs font-bold text-slate-900">{st.name}</h4>
                      <span className="text-[10px] text-slate-400 font-medium">
                        {new Date(st.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </span>
                    </div>
                    <p className="text-xs text-slate-700 mt-1">{st.text}</p>
                    {st.mediaUrl && (
                      <img
                        src={st.mediaUrl}
                        alt="Story Media"
                        className="mt-2 w-full h-32 object-cover rounded-xl border border-slate-100"
                      />
                    )}
                    <p className="text-[10px] text-emerald-600 font-bold mt-1.5 flex items-center gap-1">
                      👁️ {st.viewsCount} views
                    </p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* WALLET TAB */}
        {activeTab === 'wallet' && (
          <div className="space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-xs font-extrabold text-slate-500 uppercase tracking-wider">
                Cards & Payment Wallet
              </h2>
              <button
                onClick={() => setShowAddCardModal(true)}
                className="px-3 py-1.5 bg-blue-600 hover:bg-blue-700 text-white rounded-xl text-xs font-bold transition-all shadow-sm flex items-center gap-1"
              >
                <CreditCard className="w-4 h-4" /> Add Card
              </button>
            </div>

            {/* Render Cards */}
            <div className="space-y-3">
              {cards.map(card => (
                <div
                  key={card.id}
                  style={{ backgroundColor: card.cardColorHex }}
                  className="p-5 rounded-3xl text-white shadow-xl relative overflow-hidden flex flex-col justify-between h-44"
                >
                  <div className="flex justify-between items-start">
                    <div>
                      <p className="text-[10px] uppercase font-bold text-white/70">Balance</p>
                      <p className="text-2xl font-extrabold tracking-tight">${card.balance.toFixed(2)}</p>
                    </div>
                    <span className="text-sm font-black italic tracking-widest bg-white/20 px-2.5 py-1 rounded-xl">
                      {card.cardType}
                    </span>
                  </div>

                  <div>
                    <p className="text-base font-mono tracking-widest mb-1">{card.cardNumber}</p>
                    <div className="flex justify-between text-[11px] font-semibold text-white/80">
                      <span>{card.cardHolder}</span>
                      <span>EXP: {card.expiryDate}</span>
                    </div>
                  </div>
                </div>
              ))}
            </div>

            {/* Transactions Header */}
            <div className="flex items-center justify-between pt-2">
              <h3 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Recent Transactions</h3>
              <button
                onClick={() => setShowAddTxModal(true)}
                className="text-xs font-bold text-blue-600 hover:underline flex items-center gap-1"
              >
                <Plus className="w-3.5 h-3.5" /> Add Tx
              </button>
            </div>

            <div className="space-y-2">
              {transactions.map(tx => (
                <div key={tx.id} className="p-3 bg-white border border-slate-200/80 rounded-2xl flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <div className={`p-2 rounded-xl text-xs font-bold ${tx.amount < 0 ? 'bg-red-50 text-red-600' : 'bg-emerald-50 text-emerald-600'}`}>
                      {tx.amount < 0 ? <ArrowUpRight className="w-4 h-4" /> : <ArrowDownLeft className="w-4 h-4" />}
                    </div>
                    <div>
                      <h4 className="text-xs font-bold text-slate-900">{tx.title}</h4>
                      <p className="text-[10px] text-slate-400">{tx.category} • {tx.dateText}</p>
                    </div>
                  </div>
                  <span className={`text-xs font-extrabold ${tx.amount < 0 ? 'text-slate-900' : 'text-emerald-600'}`}>
                    {tx.amount < 0 ? `-$${Math.abs(tx.amount).toFixed(2)}` : `+$${tx.amount.toFixed(2)}`}
                  </span>
                </div>
              ))}
            </div>
          </div>
        )}
      </main>

      {/* MODALS */}
      {showProfileModal && (
        <UserProfileDialog
          onDismiss={() => setShowProfileModal(false)}
          onOpenSupabase={() => {
            setShowProfileModal(false);
            setShowSupabaseModal(true);
          }}
        />
      )}

      {showSupabaseModal && (
        <SupabaseIntegrationDialog onDismiss={() => setShowSupabaseModal(false)} />
      )}

      {/* New Chat Modal */}
      {showNewChatModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl border border-slate-100 max-h-[80vh] overflow-y-auto">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-800">Start New Chat</h3>
              <button onClick={() => setShowNewChatModal(false)} className="p-1 hover:bg-slate-100 rounded-full">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>
            <div className="space-y-2">
              {filteredUsers.map(u => (
                <div
                  key={u.uid}
                  onClick={() => {
                    openOneOnOneChatWithUser(u);
                    setShowNewChatModal(false);
                  }}
                  className="p-2.5 hover:bg-slate-50 rounded-2xl cursor-pointer flex items-center gap-3 transition-colors"
                >
                  <img src={u.photoUrl} alt={u.name} className="w-10 h-10 rounded-full object-cover" />
                  <div className="overflow-hidden">
                    <h4 className="text-xs font-bold text-slate-900 truncate">{u.name}</h4>
                    <p className="text-[10px] text-slate-500 truncate">{u.phoneNumber || u.email}</p>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* New Group Modal */}
      {showNewGroupModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl border border-slate-100">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-800">Create New Group</h3>
              <button onClick={() => setShowNewGroupModal(false)} className="p-1 hover:bg-slate-100 rounded-full">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>
            <form onSubmit={handleCreateGroup} className="space-y-3">
              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Group Name</label>
                <input
                  type="text"
                  placeholder="e.g. AmBle Core Team"
                  value={groupName}
                  onChange={(e) => setGroupName(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Select Members</label>
                <div className="max-h-40 overflow-y-auto space-y-1">
                  {users.map(u => (
                    <div
                      key={u.uid}
                      onClick={() => {
                        setSelectedMembers(prev => prev.includes(u.uid) ? prev.filter(id => id !== u.uid) : [...prev, u.uid]);
                      }}
                      className={`p-2 rounded-xl text-xs cursor-pointer flex items-center justify-between ${
                        selectedMembers.includes(u.uid) ? 'bg-blue-50 border border-blue-200 text-blue-900 font-bold' : 'hover:bg-slate-50'
                      }`}
                    >
                      <span>{u.name}</span>
                      {selectedMembers.includes(u.uid) && <span className="text-blue-600">✓</span>}
                    </div>
                  ))}
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 text-white rounded-2xl text-xs font-bold shadow-md hover:bg-blue-700"
              >
                Create Group
              </button>
            </form>
          </div>
        </div>
      )}

      {/* New Status Story Modal */}
      {showNewStatusModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-800">Post Status Story</h3>
              <button onClick={() => setShowNewStatusModal(false)} className="p-1 hover:bg-slate-100 rounded-full">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>
            <form onSubmit={handlePostStatus} className="space-y-3">
              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Text Caption</label>
                <textarea
                  placeholder="What's on your mind?"
                  value={statusText}
                  onChange={(e) => setStatusText(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl p-3 text-xs focus:outline-none h-20"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Media Image URL (Optional)</label>
                <input
                  type="url"
                  placeholder="https://..."
                  value={statusMediaUrl}
                  onChange={(e) => setStatusMediaUrl(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs focus:outline-none"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 text-white rounded-2xl text-xs font-bold shadow-md hover:bg-blue-700"
              >
                Post Story
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Add Card Modal */}
      {showAddCardModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-800">Add Payment Card</h3>
              <button onClick={() => setShowAddCardModal(false)} className="p-1 hover:bg-slate-100 rounded-full">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>
            <form onSubmit={handleAddCard} className="space-y-3">
              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Card Network</label>
                <select
                  value={cardType}
                  onChange={(e) => setCardType(e.target.value as any)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                >
                  <option value="Visa">Visa</option>
                  <option value="MasterCard">MasterCard</option>
                  <option value="Amex">American Express</option>
                </select>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Card Number</label>
                <input
                  type="text"
                  placeholder="4850 **** **** 1234"
                  value={cardNumber}
                  onChange={(e) => setCardNumber(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                />
              </div>

              <div className="flex gap-2">
                <div className="flex-1">
                  <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Expiry</label>
                  <input
                    type="text"
                    placeholder="12/28"
                    value={expiryDate}
                    onChange={(e) => setExpiryDate(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                  />
                </div>
                <div className="flex-1">
                  <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Initial Balance ($)</label>
                  <input
                    type="number"
                    placeholder="500.00"
                    value={balance}
                    onChange={(e) => setBalance(e.target.value)}
                    className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 text-white rounded-2xl text-xs font-bold shadow-md hover:bg-blue-700"
              >
                Save Card
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Add Transaction Modal */}
      {showAddTxModal && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-md bg-white rounded-3xl p-5 shadow-2xl">
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-sm font-bold text-slate-800">Add Transaction</h3>
              <button onClick={() => setShowAddTxModal(false)} className="p-1 hover:bg-slate-100 rounded-full">
                <X className="w-5 h-5 text-slate-400" />
              </button>
            </div>
            <form onSubmit={handleAddTransaction} className="space-y-3">
              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Title</label>
                <input
                  type="text"
                  placeholder="e.g. Flight Ticket"
                  value={txTitle}
                  onChange={(e) => setTxTitle(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Category</label>
                <select
                  value={txCategory}
                  onChange={(e) => setTxCategory(e.target.value as any)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                >
                  <option value="Flight">Flight</option>
                  <option value="Shopping">Shopping</option>
                  <option value="Salary">Salary</option>
                  <option value="Food">Food</option>
                  <option value="Transfer">Transfer</option>
                  <option value="Bills">Bills</option>
                </select>
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-500 uppercase block mb-1">Amount ($ - positive or negative)</label>
                <input
                  type="number"
                  placeholder="-50.00 or 2500.00"
                  value={txAmount}
                  onChange={(e) => setTxAmount(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-3 py-2 text-xs"
                />
              </div>

              <button
                type="submit"
                className="w-full py-2.5 bg-blue-600 text-white rounded-2xl text-xs font-bold shadow-md hover:bg-blue-700"
              >
                Log Transaction
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Bottom Navigation Bar */}
      <nav className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-white/90 backdrop-blur-md border-t border-slate-200/80 px-6 py-2.5 flex items-center justify-around z-40">
        <button
          onClick={() => setActiveTab('chats')}
          className={`flex flex-col items-center gap-1 text-[10px] font-bold transition-all ${
            activeTab === 'chats' ? 'text-blue-600 scale-105' : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          <MessageSquare className="w-5 h-5" />
          <span>Chats</span>
        </button>

        <button
          onClick={() => setActiveTab('calls')}
          className={`flex flex-col items-center gap-1 text-[10px] font-bold transition-all ${
            activeTab === 'calls' ? 'text-blue-600 scale-105' : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          <Phone className="w-5 h-5" />
          <span>Calls</span>
        </button>

        <button
          onClick={() => setActiveTab('status')}
          className={`flex flex-col items-center gap-1 text-[10px] font-bold transition-all ${
            activeTab === 'status' ? 'text-blue-600 scale-105' : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          <CircleDashed className="w-5 h-5" />
          <span>Status</span>
        </button>

        <button
          onClick={() => setActiveTab('wallet')}
          className={`flex flex-col items-center gap-1 text-[10px] font-bold transition-all ${
            activeTab === 'wallet' ? 'text-blue-600 scale-105' : 'text-slate-400 hover:text-slate-600'
          }`}
        >
          <Wallet className="w-5 h-5" />
          <span>Wallet</span>
        </button>
      </nav>
    </div>
  );
};
