import React from 'react';
import { useChat } from '../context/ChatContext';
import { PhoneOff, Maximize2, Mic, MicOff, Video } from 'lucide-react';

export const FloatingPipCallOverlay: React.FC = () => {
  const { activeCall, isPiPActive, setPiPActive, endCall, navigateTo } = useChat();

  if (!activeCall || !isPiPActive || activeCall.status !== 'active') {
    return null;
  }

  const callee = activeCall.calleeUser;

  return (
    <div className="fixed bottom-24 right-4 z-50 w-44 rounded-2xl bg-slate-900/90 border border-cyan-400/50 p-2.5 shadow-2xl backdrop-blur-md text-white animate-fade-in">
      <div className="flex items-center justify-between mb-2">
        <div className="flex items-center gap-1.5">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-ping" />
          <span className="text-[10px] font-bold text-cyan-300 uppercase">
            {activeCall.type} Call
          </span>
        </div>
        <button
          onClick={() => {
            setPiPActive(false);
            navigateTo('call');
          }}
          className="p-1 hover:bg-white/10 rounded-lg text-slate-300"
          title="Expand Call"
        >
          <Maximize2 className="w-3.5 h-3.5" />
        </button>
      </div>

      <div className="flex items-center gap-2 mb-3">
        <img
          src={callee?.photoUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"}
          alt="Call Partner"
          className="w-9 h-9 rounded-full object-cover border border-white/20"
        />
        <div className="overflow-hidden">
          <p className="text-xs font-bold truncate text-white">{callee?.name || 'AmBle User'}</p>
          <p className="text-[10px] text-emerald-400 font-mono">00:42 • 1080p</p>
        </div>
      </div>

      <div className="flex items-center justify-around gap-1 pt-1 border-t border-white/10">
        <button
          onClick={() => endCall(activeCall.callId)}
          className="flex-1 py-1 bg-red-600 hover:bg-red-700 text-white rounded-xl text-[10px] font-bold flex items-center justify-center gap-1"
        >
          <PhoneOff className="w-3 h-3" /> End
        </button>
      </div>
    </div>
  );
};
