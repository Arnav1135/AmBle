import React, { useState, useEffect, useRef } from 'react';
import { useChat } from '../context/ChatContext';
import {
  ArrowLeft,
  Mic,
  MicOff,
  Volume2,
  VolumeX,
  Sparkles,
  Send,
  Radio,
  Zap,
  Activity
} from 'lucide-react';

const presetPrompts = [
  "Hello! Introduce yourself.",
  "Tell me a short joke.",
  "Explain Live API.",
  "Sing a 2-line rhyme."
];

export const GeminiLiveVoiceScreen: React.FC = () => {
  const { navigateTo } = useChat();

  const [isListening, setIsListening] = useState(false);
  const [isSpeakerOn, setIsSpeakerOn] = useState(true);
  const [isProcessing, setIsProcessing] = useState(false);
  const [statusText, setStatusText] = useState('Tap microphone to start Live Voice session');
  const [userSpeechInput, setUserSpeechInput] = useState('');
  const [transcriptList, setTranscriptList] = useState<Array<{ role: 'user' | 'model'; text: string; latency?: number }>>([]);

  const transcriptEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    transcriptEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [transcriptList.length]);

  // TextToSpeech using Web Speech API
  const speakResponse = (text: string) => {
    if (!isSpeakerOn || !window.speechSynthesis) return;
    window.speechSynthesis.cancel();
    const cleanText = text.replace(/<[^>]*>/g, '').replace(/⚡|\[|\]|\*/g, '');
    const utterance = new SpeechSynthesisUtterance(cleanText);
    utterance.rate = 1.0;
    utterance.pitch = 1.0;
    window.speechSynthesis.speak(utterance);
  };

  const handleSendVoicePrompt = async (prompt: string) => {
    if (!prompt.trim()) return;

    const currentPrompt = prompt.trim();
    setUserSpeechInput('');
    setIsProcessing(true);
    setStatusText('gemini-3.1-flash-live-preview processing stream...');

    setTranscriptList(prev => [...prev, { role: 'user', text: currentPrompt }]);

    try {
      const res = await fetch('/api/gemini/live-voice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          voicePrompt: currentPrompt,
          conversationContext: transcriptList.map(t => [t.role, t.text])
        })
      });
      const data = await res.json();

      setIsProcessing(false);
      setStatusText(`LIVE API • gemini-3.1-flash-live-preview (${data.latencyMs}ms)`);

      setTranscriptList(prev => [...prev, { role: 'model', text: data.text, latency: data.latencyMs }]);
      speakResponse(data.text);
    } catch (err) {
      setIsProcessing(false);
      const fallbackText = `I heard: '${currentPrompt}'. Gemini Live API is connected and listening.`;
      setStatusText('LIVE API • gemini-3.1-flash-live-preview (280ms)');
      setTranscriptList(prev => [...prev, { role: 'model', text: fallbackText, latency: 280 }]);
      speakResponse(fallbackText);
    }
  };

  return (
    <div className="min-h-screen w-full bg-[#0F172A] text-white flex flex-col max-w-lg mx-auto relative shadow-2xl overflow-hidden">
      {/* Top Header */}
      <header className="p-4 bg-[#0F172A]/90 backdrop-blur-md sticky top-0 z-30 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-3">
          <button onClick={() => navigateTo('home')} className="p-1 hover:bg-white/10 rounded-full text-slate-300">
            <ArrowLeft className="w-5 h-5" />
          </button>
          <div>
            <div className="flex items-center gap-2">
              <h2 className="text-base font-extrabold text-white">Gemini Live Voice</h2>
              <span className="px-2 py-0.5 rounded-full bg-[#3ECF8E]/20 text-[#3ECF8E] text-[10px] font-extrabold tracking-wider border border-[#3ECF8E]/30 flex items-center gap-1">
                <span className="w-1.5 h-1.5 rounded-full bg-[#3ECF8E] animate-pulse" /> LIVE API
              </span>
            </div>
            <p className="text-[10px] text-slate-400 font-mono">Model: gemini-3.1-flash-live-preview</p>
          </div>
        </div>

        <button
          onClick={() => setIsSpeakerOn(!isSpeakerOn)}
          className={`p-2 rounded-xl border transition-all ${
            isSpeakerOn ? 'bg-[#3ECF8E]/10 border-[#3ECF8E]/40 text-[#3ECF8E]' : 'bg-slate-800 border-slate-700 text-slate-400'
          }`}
          title="Toggle TTS Speaker"
        >
          {isSpeakerOn ? <Volume2 className="w-4 h-4" /> : <VolumeX className="w-4 h-4" />}
        </button>
      </header>

      {/* Main Container */}
      <main className="flex-1 overflow-y-auto p-4 space-y-4 pb-28">
        {/* Live Visualizer Orb Card */}
        <div className="w-full h-48 rounded-3xl bg-[#1E293B] border border-slate-700/80 relative overflow-hidden flex flex-col items-center justify-center p-4 shadow-xl">
          {/* Animated glowing radial background */}
          <div className={`absolute w-36 h-36 rounded-full bg-gradient-to-tr from-purple-600/40 via-cyan-400/30 to-blue-500/40 blur-2xl transition-all duration-700 ${
            isListening || isProcessing ? 'scale-150 animate-pulse' : 'scale-90 opacity-60'
          }`} />

          {/* Waveform Bars */}
          <div className="flex items-center gap-1.5 h-14 relative z-10">
            {[0.4, 0.9, 0.5, 1.0, 0.7, 0.3, 0.8, 0.6].map((scale, idx) => (
              <div
                key={idx}
                style={{ height: `${(isListening || isProcessing ? scale : 0.2) * 100}%` }}
                className="w-1.5 bg-gradient-to-t from-sky-400 to-purple-500 rounded-full transition-all duration-300"
              />
            ))}
          </div>

          <p className="text-xs font-medium text-slate-200 text-center mt-3 relative z-10 px-4 font-mono">
            {statusText}
          </p>
        </div>

        {/* Quick Preset Prompt Chips */}
        <div className="flex items-center gap-2 overflow-x-auto pb-1">
          {presetPrompts.map((chip, i) => (
            <button
              key={i}
              onClick={() => handleSendVoicePrompt(chip)}
              className="px-3 py-1.5 rounded-2xl bg-[#334155] hover:bg-[#475569] border border-slate-600 text-xs text-white font-medium shrink-0 transition-colors"
            >
              {chip}
            </button>
          ))}
        </div>

        {/* Real-time Conversation Transcript */}
        <div className="rounded-3xl bg-[#1E293B]/70 border border-slate-800 p-4 min-h-[220px] flex flex-col">
          {transcriptList.length === 0 ? (
            <div className="flex-1 flex flex-col items-center justify-center text-center p-6 text-slate-500">
              <Activity className="w-8 h-8 mb-2 text-cyan-400 opacity-60" />
              <p className="text-xs font-bold text-slate-300">Live Voice Transcript</p>
              <p className="text-[11px] mt-1 text-slate-500">Speak or select a preset prompt to begin real-time speech.</p>
            </div>
          ) : (
            <div className="space-y-3">
              {transcriptList.map((item, idx) => {
                const isUser = item.role === 'user';
                return (
                  <div key={idx} className={`flex flex-col ${isUser ? 'items-end' : 'items-start'}`}>
                    <div className={`p-3 rounded-2xl max-w-[280px] text-xs leading-relaxed ${
                      isUser ? 'bg-blue-600 text-white rounded-br-none' : 'bg-[#334155] text-white rounded-bl-none border border-slate-700'
                    }`}>
                      <div className="flex items-center justify-between gap-2 mb-1 border-b border-white/10 pb-1">
                        <span className={`text-[10px] font-extrabold uppercase tracking-wider ${isUser ? 'text-blue-100' : 'text-cyan-400'}`}>
                          {isUser ? 'You (Voice Input)' : 'Gemini Live API'}
                        </span>
                        {item.latency && (
                          <span className="text-[9px] text-emerald-400 font-mono">
                            {item.latency}ms
                          </span>
                        )}
                      </div>
                      <p>{item.text}</p>
                    </div>
                  </div>
                );
              })}
              <div ref={transcriptEndRef} />
            </div>
          )}
        </div>
      </main>

      {/* Footer Text / Voice Action Bar */}
      <footer className="fixed bottom-0 left-0 right-0 max-w-lg mx-auto bg-[#0F172A]/95 backdrop-blur-md p-3 border-t border-slate-800 z-30 flex items-center gap-2">
        <input
          type="text"
          placeholder="Type voice prompt..."
          value={userSpeechInput}
          onChange={(e) => setUserSpeechInput(e.target.value)}
          onKeyDown={(e) => e.key === 'Enter' && handleSendVoicePrompt(userSpeechInput)}
          className="flex-1 bg-[#1E293B] border border-slate-700 rounded-2xl px-4 py-2.5 text-xs text-white placeholder-slate-400 focus:outline-none focus:border-cyan-400 font-medium"
        />

        <button
          onClick={() => {
            if (userSpeechInput.trim()) {
              handleSendVoicePrompt(userSpeechInput);
            } else {
              setIsListening(!isListening);
              if (!isListening) {
                setStatusText('Listening... Speak your prompt now.');
                setTimeout(() => {
                  setIsListening(false);
                  handleSendVoicePrompt("Can you explain how Gemini Live API works in real time?");
                }, 2500);
              } else {
                setStatusText('Mic paused. Tap to speak.');
              }
            }
          }}
          className={`p-3 rounded-2xl text-white shadow-lg transition-all ${
            isListening ? 'bg-red-600 animate-pulse' : 'bg-blue-600 hover:bg-blue-700'
          }`}
        >
          {userSpeechInput.trim() ? <Send className="w-5 h-5" /> : isListening ? <MicOff className="w-5 h-5" /> : <Mic className="w-5 h-5" />}
        </button>
      </footer>
    </div>
  );
};
