import React, { useState, useRef, useEffect } from 'react';
import { useChat } from '../context/ChatContext';
import { MessageEntity } from '../types';
import {
  ArrowLeft,
  Phone,
  Video,
  MoreVertical,
  Send,
  Mic,
  Paperclip,
  Smile,
  Image,
  FileText,
  MapPin,
  User,
  X,
  CheckCheck,
  AlertCircle,
  Play,
  Pause,
  Star,
  Trash2,
  Reply,
  Edit2,
  Clock,
  Shield,
  Volume2,
  VolumeX,
  Pin
} from 'lucide-react';

const reactionEmojis = ["❤️", "😂", "👍", "🔥", "😮", "🙏"];

export const ChatDetailScreen: React.FC = () => {
  const {
    selectedChatId,
    chats,
    users,
    messages,
    currentUser,
    sendMessage,
    retrySendMessage,
    deleteMessage,
    editMessage,
    toggleMessageStar,
    toggleMessageReaction,
    placeCall,
    updateChatDisappearing,
    toggleMuteChat,
    togglePinChat,
    toggleBlockUser,
    navigateTo
  } = useChat();

  const [inputMsg, setInputMsg] = useState('');
  const [showAttachmentMenu, setShowAttachmentMenu] = useState(false);
  const [showOptionsMenu, setShowOptionsMenu] = useState(false);
  const [showDisappearingMenu, setShowDisappearingMenu] = useState(false);
  const [replyingTo, setReplyingTo] = useState<MessageEntity | null>(null);
  const [editingMsg, setEditingMsg] = useState<MessageEntity | null>(null);
  const [activeReactionMsgId, setActiveReactionMsgId] = useState<string | null>(null);
  const [playingVoiceId, setPlayingVoiceId] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingTime, setRecordingTime] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  const currentChat = chats.find(c => c.chatId === selectedChatId);
  const chatMessages = selectedChatId ? (messages[selectedChatId] || []) : [];

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [chatMessages.length]);

  // Recording timer
  useEffect(() => {
    let interval: any;
    if (isRecording) {
      interval = setInterval(() => {
        setRecordingTime(t => t + 1);
      }, 1000);
    } else {
      setRecordingTime(0);
    }
    return () => clearInterval(interval);
  }, [isRecording]);

  if (!selectedChatId || !currentChat) {
    return (
      <div className="min-h-screen bg-slate-100 flex items-center justify-center p-4">
        <p className="text-xs text-slate-500 font-bold">No chat selected.</p>
      </div>
    );
  }

  const partnerUser = users.find(u => selectedChatId.includes(u.uid)) || users[0];

  const handleSend = async () => {
    if (!inputMsg.trim()) return;

    if (editingMsg) {
      editMessage(editingMsg.messageId, selectedChatId, inputMsg.trim());
      setEditingMsg(null);
      setInputMsg('');
      return;
    }

    const textToSend = inputMsg.trim();
    setInputMsg('');

    await sendMessage(selectedChatId, textToSend, 'text', {
      replyToId: replyingTo?.messageId,
      replyToText: replyingTo?.text
    });

    setReplyingTo(null);
  };

  const handleSendVoiceNote = async () => {
    setIsRecording(false);
    await sendMessage(selectedChatId, "Voice Note (0:05)", 'voice', {
      mediaUrl: "https://actions.google.com/sounds/v1/ambiences/rain_heavy.ogg",
      mediaDuration: 5
    });
  };

  const handleSendImageSample = async () => {
    setShowAttachmentMenu(false);
    await sendMessage(selectedChatId, "Image Attachment", 'image', {
      mediaUrl: "https://images.unsplash.com/photo-1518770660439-4636190af475?w=500"
    });
  };

  const handleSendFileSample = async () => {
    setShowAttachmentMenu(false);
    await sendMessage(selectedChatId, "Document.pdf", 'file', {
      fileName: "AmBle_WebRTC_Architecture_v3.pdf",
      fileSize: "2.4 MB"
    });
  };

  const handleSendLocationSample = async () => {
    setShowAttachmentMenu(false);
    await sendMessage(selectedChatId, "Current Live Location", 'location', {
      latitude: 37.7749,
      longitude: -122.4194
    });
  };

  return (
    <div className="min-h-screen w-full bg-[#EAF2FB] flex flex-col max-w-lg mx-auto relative shadow-2xl overflow-hidden border-x border-slate-200">
      {/* Top Header Bar */}
      <header className="p-3 bg-white/90 backdrop-blur-md sticky top-0 z-30 border-b border-slate-200 flex items-center justify-between">
        <div className="flex items-center gap-2 overflow-hidden">
          <button onClick={() => navigateTo('home')} className="p-1 hover:bg-slate-100 rounded-full text-slate-600">
            <ArrowLeft className="w-5 h-5" />
          </button>

          <img
            src={currentChat.groupPhoto}
            alt={currentChat.groupName}
            className="w-10 h-10 rounded-full object-cover border border-slate-200 shrink-0"
          />

          <div className="overflow-hidden">
            <h3 className="text-xs font-bold text-slate-900 truncate">{currentChat.groupName}</h3>
            <p className="text-[10px] text-emerald-600 font-semibold truncate flex items-center gap-1">
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
              Online • ⚡ Low-Latency Active
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1 shrink-0">
          <button
            onClick={() => placeCall(partnerUser.uid, 'voice')}
            className="p-2 hover:bg-slate-100 rounded-xl text-emerald-600"
            title="Voice Call"
          >
            <Phone className="w-4 h-4" />
          </button>
          <button
            onClick={() => placeCall(partnerUser.uid, 'video')}
            className="p-2 hover:bg-slate-100 rounded-xl text-blue-600"
            title="Video Call"
          >
            <Video className="w-4 h-4" />
          </button>
          <button
            onClick={() => setShowOptionsMenu(!showOptionsMenu)}
            className="p-2 hover:bg-slate-100 rounded-xl text-slate-600"
          >
            <MoreVertical className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Options Dropdown Menu */}
      {showOptionsMenu && (
        <div className="absolute top-14 right-4 z-40 bg-white rounded-2xl p-2 shadow-2xl border border-slate-200 text-xs w-48 space-y-1">
          <button
            onClick={() => {
              toggleMuteChat(selectedChatId);
              setShowOptionsMenu(false);
            }}
            className="w-full text-left p-2 hover:bg-slate-50 rounded-xl flex items-center gap-2 text-slate-700"
          >
            {currentChat.isMuted ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
            {currentChat.isMuted ? 'Unmute Notifications' : 'Mute Notifications'}
          </button>

          <button
            onClick={() => {
              togglePinChat(selectedChatId);
              setShowOptionsMenu(false);
            }}
            className="w-full text-left p-2 hover:bg-slate-50 rounded-xl flex items-center gap-2 text-slate-700"
          >
            <Pin className="w-4 h-4" />
            {currentChat.isPinned ? 'Unpin Chat' : 'Pin to Top'}
          </button>

          <button
            onClick={() => {
              setShowDisappearingMenu(true);
              setShowOptionsMenu(false);
            }}
            className="w-full text-left p-2 hover:bg-slate-50 rounded-xl flex items-center gap-2 text-slate-700"
          >
            <Clock className="w-4 h-4 text-purple-600" /> Disappearing Messages
          </button>

          <button
            onClick={() => {
              toggleBlockUser(partnerUser.uid);
              setShowOptionsMenu(false);
              alert(`User ${partnerUser.name} blocked status toggled.`);
            }}
            className="w-full text-left p-2 hover:bg-red-50 text-red-600 rounded-xl flex items-center gap-2 font-semibold"
          >
            <Shield className="w-4 h-4" /> Block User
          </button>
        </div>
      )}

      {/* Disappearing Messages Duration Modal */}
      {showDisappearingMenu && (
        <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
          <div className="w-full max-w-xs bg-white rounded-3xl p-5 shadow-2xl text-xs space-y-3">
            <h3 className="font-bold text-slate-900 text-sm">Disappearing Messages</h3>
            <p className="text-slate-500">New messages in this chat will disappear after the selected duration.</p>
            <div className="space-y-1 pt-1">
              {[
                { label: 'Off', duration: 0 },
                { label: '24 Hours', duration: 86400000 },
                { label: '7 Days', duration: 604800000 }
              ].map(opt => (
                <div
                  key={opt.label}
                  onClick={() => {
                    updateChatDisappearing(selectedChatId, opt.duration);
                    setShowDisappearingMenu(false);
                  }}
                  className={`p-2.5 rounded-xl cursor-pointer flex items-center justify-between font-bold ${
                    currentChat.disappearingDuration === opt.duration ? 'bg-blue-50 text-blue-600' : 'hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <span>{opt.label}</span>
                  {currentChat.disappearingDuration === opt.duration && <span>✓</span>}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* Message Feed Scroll Area */}
      <main className="flex-1 overflow-y-auto p-4 space-y-3 pb-32">
        <div className="text-center my-2">
          <span className="px-3 py-1 rounded-full bg-slate-200/80 text-[10px] font-extrabold text-slate-600 uppercase tracking-wider">
            Today • End-to-End Encrypted
          </span>
        </div>

        {chatMessages.map(msg => {
          const isMe = msg.senderId === currentUser?.uid || msg.senderId === 'alex_mercer_me';
          const isVoice = msg.type === 'voice';

          return (
            <div
              key={msg.messageId}
              className={`flex flex-col ${isMe ? 'items-end' : 'items-start'} group relative`}
            >
              {/* Reply Reference Preview Bar */}
              {msg.replyToText && (
                <div className={`text-[10px] p-2 rounded-t-xl max-w-[260px] border-l-2 mb-1 truncate ${
                  isMe ? 'bg-blue-700/80 text-blue-100 border-white' : 'bg-slate-200 text-slate-700 border-blue-500'
                }`}>
                  Replying to: "{msg.replyToText}"
                </div>
              )}

              {/* Message Bubble Card */}
              <div
                className={`p-3 rounded-2xl max-w-[280px] shadow-sm relative ${
                  isMe ? 'bg-blue-600 text-white rounded-br-none' : 'bg-white text-slate-900 rounded-bl-none border border-slate-200/80'
                }`}
              >
                {/* Voice Note Player */}
                {isVoice ? (
                  <div className="flex items-center gap-2.5">
                    <button
                      onClick={() => setPlayingVoiceId(playingVoiceId === msg.messageId ? null : msg.messageId)}
                      className={`p-2 rounded-full ${isMe ? 'bg-white/20 text-white' : 'bg-blue-600 text-white'}`}
                    >
                      {playingVoiceId === msg.messageId ? <Pause className="w-4 h-4" /> : <Play className="w-4 h-4" />}
                    </button>
                    <div className="flex-1">
                      <div className="h-4 flex items-center gap-1">
                        {[0.4, 0.8, 1.0, 0.5, 0.7, 0.3, 0.9, 0.6, 0.4].map((h, i) => (
                          <span
                            key={i}
                            style={{ height: `${h * 100}%` }}
                            className={`w-1 rounded-full ${isMe ? 'bg-white' : 'bg-blue-500'} ${playingVoiceId === msg.messageId ? 'animate-pulse' : ''}`}
                          />
                        ))}
                      </div>
                      <span className="text-[9px] opacity-80 mt-1 block">Voice Note • 0:05</span>
                    </div>
                  </div>
                ) : msg.type === 'image' && msg.mediaUrl ? (
                  <div>
                    <img src={msg.mediaUrl} alt="Attachment" className="rounded-xl w-full h-36 object-cover mb-1.5" />
                    <p className="text-xs">{msg.text}</p>
                  </div>
                ) : msg.type === 'file' ? (
                  <div className="flex items-center gap-2 p-2 rounded-xl bg-black/10">
                    <FileText className="w-6 h-6 shrink-0" />
                    <div className="overflow-hidden">
                      <p className="text-xs font-bold truncate">{msg.fileName || 'Document.pdf'}</p>
                      <p className="text-[10px] opacity-80">{msg.fileSize || '1.2 MB'}</p>
                    </div>
                  </div>
                ) : msg.type === 'location' ? (
                  <div className="flex items-center gap-2 p-2 rounded-xl bg-black/10">
                    <MapPin className="w-6 h-6 shrink-0 text-red-400" />
                    <div>
                      <p className="text-xs font-bold">Live Location Shared</p>
                      <p className="text-[10px] opacity-80">Lat: {msg.latitude}, Lon: {msg.longitude}</p>
                    </div>
                  </div>
                ) : (
                  <p className="text-xs leading-relaxed whitespace-pre-wrap">{msg.text}</p>
                )}

                {/* Status Ticks & Timestamp */}
                <div className={`flex items-center justify-end gap-1 mt-1 text-[9px] ${isMe ? 'text-blue-100' : 'text-slate-400'}`}>
                  <span>{new Date(msg.timestamp).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}</span>
                  {msg.isStarred && <Star className="w-2.5 h-2.5 text-amber-300 fill-amber-300" />}
                  {isMe && (
                    <span>
                      {msg.status === 'failed' ? (
                        <button onClick={() => retrySendMessage(msg.messageId, selectedChatId)} className="text-red-300 font-bold flex items-center gap-0.5">
                          <AlertCircle className="w-3 h-3" /> Retry
                        </button>
                      ) : (
                        <CheckCheck className={`w-3 h-3 ${msg.status === 'read' ? 'text-cyan-300' : ''}`} />
                      )}
                    </span>
                  )}
                </div>

                {/* Reaction badge */}
                {msg.reactions && (
                  <div className="absolute -bottom-2 right-2 bg-white text-slate-800 text-[10px] px-1.5 py-0.5 rounded-full shadow-md border border-slate-200">
                    {msg.reactions.split(',').map(r => r.split(':')[0]).join(' ')}
                  </div>
                )}
              </div>

              {/* Action Toolbar on Hover/Click */}
              <div className="flex items-center gap-1.5 mt-1 opacity-0 group-hover:opacity-100 transition-opacity">
                <button
                  onClick={() => setActiveReactionMsgId(activeReactionMsgId === msg.messageId ? null : msg.messageId)}
                  className="p-1 hover:bg-slate-200 rounded-lg text-slate-500 text-[11px]"
                  title="React"
                >
                  😊
                </button>
                <button
                  onClick={() => setReplyingTo(msg)}
                  className="p-1 hover:bg-slate-200 rounded-lg text-slate-500"
                  title="Reply"
                >
                  <Reply className="w-3 h-3" />
                </button>
                <button
                  onClick={() => toggleMessageStar(msg.messageId, selectedChatId)}
                  className="p-1 hover:bg-slate-200 rounded-lg text-slate-500"
                  title="Star"
                >
                  <Star className="w-3 h-3" />
                </button>
                {isMe && (
                  <button
                    onClick={() => {
                      setEditingMsg(msg);
                      setInputMsg(msg.text);
                    }}
                    className="p-1 hover:bg-slate-200 rounded-lg text-slate-500"
                    title="Edit"
                  >
                    <Edit2 className="w-3 h-3" />
                  </button>
                )}
                <button
                  onClick={() => deleteMessage(msg.messageId, selectedChatId, true)}
                  className="p-1 hover:bg-red-100 text-red-600 rounded-lg"
                  title="Delete"
                >
                  <Trash2 className="w-3 h-3" />
                </button>
              </div>

              {/* Reaction Emoji Picker */}
              {activeReactionMsgId === msg.messageId && (
                <div className="flex gap-1.5 bg-white p-1.5 rounded-2xl shadow-xl border border-slate-200 mt-1 z-20">
                  {reactionEmojis.map(emoji => (
                    <button
                      key={emoji}
                      onClick={() => {
                        toggleMessageReaction(msg.messageId, selectedChatId, emoji);
                        setActiveReactionMsgId(null);
                      }}
                      className="hover:scale-125 transition-transform text-sm"
                    >
                      {emoji}
                    </button>
                  ))}
                </div>
              )}
            </div>
          );
        })}
        <div ref={messagesEndRef} />
      </main>

      {/* Attachment Menu Popup */}
      {showAttachmentMenu && (
        <div className="absolute bottom-20 left-4 z-40 bg-white rounded-3xl p-3 shadow-2xl border border-slate-200 grid grid-cols-3 gap-2 w-64 animate-fade-in">
          <button
            onClick={handleSendImageSample}
            className="flex flex-col items-center p-3 rounded-2xl hover:bg-blue-50 text-blue-600 font-bold text-[10px]"
          >
            <Image className="w-6 h-6 mb-1" /> Image
          </button>
          <button
            onClick={handleSendFileSample}
            className="flex flex-col items-center p-3 rounded-2xl hover:bg-purple-50 text-purple-600 font-bold text-[10px]"
          >
            <FileText className="w-6 h-6 mb-1" /> Document
          </button>
          <button
            onClick={handleSendLocationSample}
            className="flex flex-col items-center p-3 rounded-2xl hover:bg-emerald-50 text-emerald-600 font-bold text-[10px]"
          >
            <MapPin className="w-6 h-6 mb-1" /> Location
          </button>
        </div>
      )}

      {/* Bottom Input Field Container */}
      <footer className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-white/95 backdrop-blur-md p-3 border-t border-slate-200 z-30">
        {replyingTo && (
          <div className="flex items-center justify-between p-2 bg-slate-100 rounded-xl mb-2 text-xs border-l-4 border-blue-600">
            <div className="overflow-hidden">
              <span className="font-bold text-blue-600 text-[10px]">Replying to message</span>
              <p className="text-slate-600 truncate">{replyingTo.text}</p>
            </div>
            <button onClick={() => setReplyingTo(null)} className="text-slate-400 hover:text-slate-600">
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowAttachmentMenu(!showAttachmentMenu)}
            className="p-2.5 hover:bg-slate-100 rounded-2xl text-slate-500 shrink-0"
          >
            <Paperclip className="w-5 h-5" />
          </button>

          <input
            type="text"
            placeholder={isRecording ? `Recording... ${recordingTime}s` : "Type a low-latency message..."}
            value={inputMsg}
            onChange={(e) => setInputMsg(e.target.value)}
            onKeyDown={(e) => e.key === 'Enter' && handleSend()}
            disabled={isRecording}
            className="flex-1 bg-slate-100 border border-slate-200/80 rounded-2xl px-4 py-2.5 text-xs text-slate-900 focus:outline-none focus:border-blue-500 font-medium"
          />

          {inputMsg.trim() ? (
            <button
              onClick={handleSend}
              className="p-2.5 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl shadow-md transition-all shrink-0"
            >
              <Send className="w-5 h-5" />
            </button>
          ) : (
            <button
              onClick={() => {
                if (isRecording) {
                  handleSendVoiceNote();
                } else {
                  setIsRecording(true);
                }
              }}
              className={`p-2.5 rounded-2xl text-white shadow-md transition-all shrink-0 ${
                isRecording ? 'bg-red-600 animate-pulse' : 'bg-blue-600 hover:bg-blue-700'
              }`}
            >
              <Mic className="w-5 h-5" />
            </button>
          )}
        </div>
      </footer>
    </div>
  );
};
