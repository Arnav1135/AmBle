import React, { useState } from 'react';
import { Sparkles, Download, X } from 'lucide-react';

export const InAppUpdateBanner: React.FC = () => {
  const [dismissed, setDismissed] = useState(false);

  if (dismissed) return null;

  return (
    <div className="w-full mb-3 rounded-2xl bg-gradient-to-r from-blue-600 via-indigo-600 to-sky-500 p-3 text-white shadow-md flex items-center justify-between gap-3">
      <div className="flex items-center gap-2.5">
        <div className="p-2 rounded-xl bg-white/20 backdrop-blur-sm">
          <Sparkles className="w-4 h-4 text-white" />
        </div>
        <div>
          <p className="text-xs font-bold leading-tight">AmBle v1.1.0 Ready</p>
          <p className="text-[11px] text-blue-100">WebRTC connection optimization & Gemini Live API enhancement.</p>
        </div>
      </div>

      <div className="flex items-center gap-1.5 shrink-0">
        <button
          onClick={() => alert("AmBle is running the latest build v1.1.0-Web!")}
          className="px-3 py-1.5 rounded-xl bg-white text-blue-700 text-xs font-bold shadow-sm hover:bg-blue-50 transition-colors flex items-center gap-1"
        >
          <Download className="w-3 h-3" /> Update
        </button>
        <button
          onClick={() => setDismissed(true)}
          className="p-1 hover:bg-white/20 rounded-lg text-white/80"
        >
          <X className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
