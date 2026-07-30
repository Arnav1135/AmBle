import React, { useState } from 'react';
import { Brain, CheckCircle2, Zap, Radio, Shield, ChevronDown, ChevronUp } from 'lucide-react';

export const AntigravityIntegrationCard: React.FC = () => {
  const [expanded, setExpanded] = useState(false);

  return (
    <div className="w-full rounded-2xl bg-[#0B1120] border border-cyan-400/30 p-4 shadow-md text-white">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <div className="w-8 h-8 rounded-full bg-gradient-to-tr from-purple-600 to-cyan-400 flex items-center justify-center">
            <Brain className="w-4 h-4 text-white" />
          </div>
          <div>
            <div className="flex items-center gap-1.5">
              <span className="text-xs font-black tracking-wider text-white">
                ANTIGRAVITY ENGINE
              </span>
              <CheckCircle2 className="w-3.5 h-3.5 text-[#00E676]" />
            </div>
            <p className="text-[10px] text-slate-400">
              Integrated Agent Runtime v3.5.0-Antigravity-Core
            </p>
          </div>
        </div>

        <button
          onClick={() => setExpanded(!expanded)}
          className="px-2.5 py-1 rounded-xl bg-cyan-400/15 border border-cyan-400/30 text-cyan-300 text-[11px] font-bold hover:bg-cyan-400/25 transition-colors flex items-center gap-1"
        >
          {expanded ? 'Hide Details' : 'Diagnostics'}
          {expanded ? <ChevronUp className="w-3 h-3" /> : <ChevronDown className="w-3 h-3" />}
        </button>
      </div>

      {/* Metric pills */}
      <div className="grid grid-cols-3 gap-2 mt-3">
        <div className="bg-white/5 rounded-xl p-2">
          <div className="flex items-center gap-1 text-[10px] text-gray-400">
            <Zap className="w-3 h-3 text-[#00E676]" />
            <span>P2P Latency</span>
          </div>
          <p className="text-xs font-bold text-white mt-0.5">18ms</p>
        </div>

        <div className="bg-white/5 rounded-xl p-2">
          <div className="flex items-center gap-1 text-[10px] text-gray-400">
            <Radio className="w-3 h-3 text-[#00E5FF]" />
            <span>Signaling</span>
          </div>
          <p className="text-xs font-bold text-[#00E676] mt-0.5">Connected</p>
        </div>

        <div className="bg-white/5 rounded-xl p-2">
          <div className="flex items-center gap-1 text-[10px] text-gray-400">
            <Shield className="w-3 h-3 text-purple-400" />
            <span>NAT Relay</span>
          </div>
          <p className="text-xs font-bold text-white mt-0.5">CGNAT Ready</p>
        </div>
      </div>

      {expanded && (
        <div className="mt-3 p-3 rounded-xl bg-black/40 border border-white/10 text-[11px] font-mono text-slate-300 leading-relaxed">
          <p className="text-[#00E5FF] font-bold text-[10px] tracking-wider mb-1">
            ANTIGRAVITY TELEMETRY & ROUTING PIPELINE
          </p>
          <ul className="space-y-1 text-[10px] list-disc list-inside text-slate-300">
            <li>Core Agent Integration: DeepMind Antigravity Agent Runtime</li>
            <li>WebSockets Handshake: wss://amble-signaling-server.onrender.com</li>
            <li>Media Codecs: H.264 / VP8 / Opus 48kHz HD Audio</li>
            <li>Bandwidth Adaptation: AI Dynamic Adaptive Bitrate</li>
            <li>Cellular NAT Traversal: TURN Relay enabled via Coturn / Managed Edge</li>
          </ul>
        </div>
      )}
    </div>
  );
};
