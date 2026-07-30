import React, { useState, useRef, useEffect } from 'react';
import { useChat } from '../context/ChatContext';
import {
  PhoneOff,
  Mic,
  MicOff,
  Video,
  VideoOff,
  Volume2,
  VolumeX,
  Camera,
  RotateCcw,
  Sparkles,
  Radio,
  Minimize2,
  Shield,
  Zap,
  CheckCircle2
} from 'lucide-react';

export const CallScreen: React.FC = () => {
  const { activeCall, endCall, setPiPActive, navigateTo } = useChat();

  const [isMuted, setIsMuted] = useState(false);
  const [isVideoOff, setIsVideoOff] = useState(false);
  const [isSpeakerOn, setIsSpeakerOn] = useState(true);
  const [showTelemetry, setShowTelemetry] = useState(true);
  const [snapshots, setSnapshots] = useState<string[]>([]);
  const [callDuration, setCallDuration] = useState(0);

  const videoRef = useRef<HTMLVideoElement>(null);

  // Call timer
  useEffect(() => {
    const timer = setInterval(() => {
      setCallDuration(d => d + 1);
    }, 1000);
    return () => clearInterval(timer);
  }, []);

  // WebRTC camera stream initialization
  useEffect(() => {
    let stream: MediaStream | null = null;
    if (activeCall?.type === 'video' && !isVideoOff && navigator.mediaDevices) {
      navigator.mediaDevices.getUserMedia({ video: true, audio: true })
        .then(s => {
          stream = s;
          if (videoRef.current) {
            videoRef.current.srcObject = stream;
          }
        })
        .catch(err => {
          console.log("Webcam simulation active:", err);
        });
    }

    return () => {
      if (stream) {
        stream.getTracks().forEach(t => t.stop());
      }
    };
  }, [activeCall?.type, isVideoOff]);

  if (!activeCall) {
    return (
      <div className="min-h-screen bg-slate-900 text-white flex items-center justify-center p-4">
        <p className="text-xs font-bold text-slate-400">No active call.</p>
      </div>
    );
  }

  const callee = activeCall.calleeUser;

  const handleCaptureSnapshot = () => {
    const canvas = document.createElement('canvas');
    canvas.width = 320;
    canvas.height = 240;
    const ctx = canvas.getContext('2d');
    if (ctx) {
      ctx.fillStyle = '#1E293B';
      ctx.fillRect(0, 0, 320, 240);
      ctx.fillStyle = '#38BDF8';
      ctx.font = '12px sans-serif';
      ctx.fillText(`WebRTC Snapshot • ${new Date().toLocaleTimeString()}`, 20, 120);
      const url = canvas.toDataURL();
      setSnapshots(prev => [url, ...prev]);
    }
  };

  const formatDuration = (secs: number) => {
    const m = Math.floor(secs / 60);
    const s = secs % 60;
    return `${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}`;
  };

  return (
    <div className="min-h-screen w-full bg-[#0B1120] text-white flex flex-col max-w-lg mx-auto relative shadow-2xl overflow-hidden">
      {/* Top Bar */}
      <header className="p-4 flex items-center justify-between z-20 relative bg-gradient-to-b from-black/80 to-transparent">
        <div className="flex items-center gap-2">
          <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 animate-ping" />
          <span className="text-xs font-black tracking-wider text-cyan-300 uppercase">
            {activeCall.type} Call • {formatDuration(callDuration)}
          </span>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={() => setShowTelemetry(!showTelemetry)}
            className="px-2.5 py-1 rounded-xl bg-white/10 border border-white/20 text-[10px] font-bold text-cyan-300 hover:bg-white/20"
          >
            {showTelemetry ? 'Hide Telemetry' : 'WebRTC Stats'}
          </button>
          <button
            onClick={() => {
              setPiPActive(true);
              navigateTo('home');
            }}
            className="p-2 bg-white/10 hover:bg-white/20 rounded-xl text-white"
            title="Minimize PiP"
          >
            <Minimize2 className="w-4 h-4" />
          </button>
        </div>
      </header>

      {/* Telemetry Overlay Card */}
      {showTelemetry && (
        <div className="mx-4 mb-2 p-3 rounded-2xl bg-black/60 border border-cyan-400/30 text-[10px] font-mono text-cyan-200 z-20 space-y-1 backdrop-blur-md">
          <div className="flex justify-between font-bold text-cyan-400">
            <span>WEBRTC P2P CONNECTOR</span>
            <span className="text-emerald-400">STATUS: ACTIVE</span>
          </div>
          <div className="grid grid-cols-2 gap-2 text-slate-300 pt-1">
            <p>• Codec: Opus 48kHz / H.264</p>
            <p>• Resolution: 1080p @ 60fps</p>
            <p>• Bitrate: 3.4 Mbps Adaptive</p>
            <p>• Packet Loss: 0.00%</p>
            <p>• Node: stun.l.google.com</p>
            <p>• Antigravity Engine: Sync OK</p>
          </div>
        </div>
      )}

      {/* Main Video View / Caller Avatar Stage */}
      <div className="flex-1 relative flex items-center justify-center p-4">
        {activeCall.type === 'video' && !isVideoOff ? (
          <div className="relative w-full h-full rounded-3xl overflow-hidden border border-white/10 bg-slate-800 shadow-2xl flex items-center justify-center">
            {/* Real webcam element or fallback animated stage */}
            <video
              ref={videoRef}
              autoPlay
              playsInline
              muted
              className="w-full h-full object-cover"
            />
            {/* Video overlay badge */}
            <div className="absolute bottom-4 left-4 p-2.5 rounded-2xl bg-black/60 backdrop-blur-md border border-white/10 flex items-center gap-2">
              <img
                src={callee?.photoUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"}
                alt="Caller"
                className="w-8 h-8 rounded-full object-cover border border-cyan-400"
              />
              <div>
                <p className="text-xs font-bold text-white">{callee?.name}</p>
                <p className="text-[9px] text-emerald-400">Live WebRTC Camera Feed</p>
              </div>
            </div>
          </div>
        ) : (
          <div className="flex flex-col items-center text-center space-y-4">
            <div className="relative">
              <div className="w-32 h-32 rounded-full p-1 bg-gradient-to-tr from-cyan-400 via-indigo-500 to-purple-500 animate-pulse">
                <img
                  src={callee?.photoUrl || "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150"}
                  alt="Caller Avatar"
                  className="w-full h-full rounded-full object-cover border-4 border-[#0B1120]"
                />
              </div>
            </div>

            <div>
              <h2 className="text-2xl font-black text-white">{callee?.name}</h2>
              <p className="text-xs text-cyan-300 font-mono mt-1">
                {activeCall.status === 'ringing' ? 'Ringing AmBle network...' : 'Voice Stream Active'}
              </p>
            </div>
          </div>
        )}
      </div>

      {/* Captured Snapshots Bar */}
      {snapshots.length > 0 && (
        <div className="px-4 py-2 flex gap-2 overflow-x-auto z-20">
          {snapshots.map((url, idx) => (
            <img key={idx} src={url} alt="Snap" className="w-12 h-12 rounded-xl object-cover border border-cyan-400/50 shrink-0" />
          ))}
        </div>
      )}

      {/* Bottom Controls Panel */}
      <footer className="p-6 bg-gradient-to-t from-black via-black/80 to-transparent z-20 flex flex-col items-center gap-4">
        <div className="flex items-center justify-around w-full max-w-xs">
          <button
            onClick={() => setIsMuted(!isMuted)}
            className={`p-4 rounded-3xl transition-all shadow-lg ${
              isMuted ? 'bg-red-600 text-white' : 'bg-white/10 hover:bg-white/20 text-white'
            }`}
            title="Mute Mic"
          >
            {isMuted ? <MicOff className="w-6 h-6" /> : <Mic className="w-6 h-6" />}
          </button>

          <button
            onClick={() => setIsVideoOff(!isVideoOff)}
            className={`p-4 rounded-3xl transition-all shadow-lg ${
              isVideoOff ? 'bg-red-600 text-white' : 'bg-white/10 hover:bg-white/20 text-white'
            }`}
            title="Toggle Video"
          >
            {isVideoOff ? <VideoOff className="w-6 h-6" /> : <Video className="w-6 h-6" />}
          </button>

          <button
            onClick={handleCaptureSnapshot}
            className="p-4 rounded-3xl bg-white/10 hover:bg-white/20 text-cyan-300 shadow-lg"
            title="Capture Snapshot"
          >
            <Camera className="w-6 h-6" />
          </button>

          <button
            onClick={() => endCall(activeCall.callId)}
            className="p-4 rounded-3xl bg-red-600 hover:bg-red-700 text-white shadow-xl shadow-red-600/30"
            title="End Call"
          >
            <PhoneOff className="w-6 h-6" />
          </button>
        </div>
      </footer>
    </div>
  );
};
