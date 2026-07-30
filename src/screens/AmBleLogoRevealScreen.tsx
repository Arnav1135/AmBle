import React, { useEffect } from 'react';
import { useChat } from '../context/ChatContext';
import { Sparkles, Radio } from 'lucide-react';

export const AmBleLogoRevealScreen: React.FC = () => {
  const { navigateTo, isLoggedIn } = useChat();

  useEffect(() => {
    const timer = setTimeout(() => {
      if (isLoggedIn) {
        navigateTo('home');
      } else {
        navigateTo('auth');
      }
    }, 2200);

    return () => clearTimeout(timer);
  }, [isLoggedIn, navigateTo]);

  return (
    <div className="min-h-screen w-full bg-gradient-to-br from-[#0F172A] via-[#1E293B] to-[#090D16] flex flex-col items-center justify-center relative overflow-hidden text-white p-6">
      {/* Background glowing blurred circles */}
      <div className="absolute top-1/4 left-1/4 w-72 h-72 bg-blue-500/20 rounded-full blur-3xl animate-pulse" />
      <div className="absolute bottom-1/4 right-1/4 w-72 h-72 bg-cyan-400/20 rounded-full blur-3xl animate-pulse delay-700" />

      {/* Main Logo Container */}
      <div className="relative z-10 flex flex-col items-center text-center max-w-sm animate-fade-in">
        <div className="relative mb-6">
          <div className="w-24 h-24 rounded-3xl bg-gradient-to-tr from-blue-600 via-indigo-500 to-cyan-400 p-1 shadow-2xl shadow-cyan-500/30 flex items-center justify-center">
            <div className="w-full h-full bg-[#0F172A] rounded-[22px] flex items-center justify-center">
              <span className="text-4xl font-extrabold tracking-tighter bg-gradient-to-r from-blue-400 via-cyan-300 to-indigo-300 bg-clip-text text-transparent">
                Am
              </span>
            </div>
          </div>
          <div className="absolute -top-2 -right-2 p-1.5 bg-cyan-400 rounded-full text-slate-900 shadow-lg animate-bounce">
            <Sparkles className="w-4 h-4" />
          </div>
        </div>

        <h1 className="text-3xl font-extrabold tracking-tight text-white mb-2">
          AmBle
        </h1>
        <p className="text-xs text-slate-300 font-medium leading-relaxed px-4">
          High-Fidelity Chat, VoIP & WebRTC Live Calling Engine
        </p>

        {/* Antigravity Runtime Tag */}
        <div className="mt-8 px-3.5 py-1.5 rounded-full bg-white/5 border border-cyan-400/30 flex items-center gap-2 backdrop-blur-md">
          <Radio className="w-3.5 h-3.5 text-cyan-400 animate-spin" />
          <span className="text-[11px] font-mono text-cyan-300">
            Powered by Antigravity Agent Runtime v3.5
          </span>
        </div>
      </div>

      {/* Loading bar at bottom */}
      <div className="absolute bottom-12 w-48 h-1.5 bg-white/10 rounded-full overflow-hidden">
        <div className="h-full bg-gradient-to-r from-blue-500 to-cyan-400 rounded-full w-full animate-pulse" />
      </div>
    </div>
  );
};
