import React from 'react';
import { Sparkles } from 'lucide-react';

interface Props {
  onClick?: () => void;
  className?: string;
}

export const AntigravityHeaderBadge: React.FC<Props> = ({ onClick, className = '' }) => {
  return (
    <div
      onClick={onClick}
      className={`inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-[#0F172A]/90 border border-cyan-400/40 text-white cursor-pointer hover:bg-[#0F172A] transition-all shadow-sm ${className}`}
    >
      <span className="w-2 h-2 rounded-full bg-[#00E676] animate-pulse" />
      <Sparkles className="w-3 h-3 text-[#00E5FF]" />
      <span className="text-[10px] font-extrabold tracking-wider text-white">
        ANTIGRAVITY ACTIVE
      </span>
    </div>
  );
};
