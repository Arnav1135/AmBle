import React, { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { Database, RefreshCw, CheckCircle2, X } from 'lucide-react';

interface Props {
  onDismiss: () => void;
}

export const SupabaseIntegrationDialog: React.FC<Props> = ({ onDismiss }) => {
  const { supabaseStatus, syncWithSupabase } = useChat();
  const [syncing, setSyncing] = useState(false);
  const [msg, setMsg] = useState('');

  const handleSync = async () => {
    setSyncing(true);
    setMsg('');
    await syncWithSupabase();
    setTimeout(() => {
      setSyncing(false);
      setMsg('Supabase cloud database successfully synchronized!');
    }, 1000);
  };

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-emerald-100 animate-fade-in">
        <div className="flex items-center justify-between mb-4">
          <div className="flex items-center gap-2.5">
            <div className="w-10 h-10 rounded-2xl bg-emerald-500/10 border border-emerald-500/20 flex items-center justify-center text-emerald-600">
              <Database className="w-5 h-5" />
            </div>
            <div>
              <h3 className="text-base font-bold text-slate-800">Supabase Cloud Sync</h3>
              <p className="text-xs text-slate-500">Real-time database & auth bridge</p>
            </div>
          </div>
          <button onClick={onDismiss} className="p-1.5 hover:bg-slate-100 rounded-full text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="space-y-3 mb-6">
          <div className="p-3 rounded-2xl bg-emerald-50 border border-emerald-200 flex items-center justify-between text-xs">
            <span className="font-semibold text-emerald-800">Connection Status</span>
            <span className="flex items-center gap-1 font-bold text-emerald-600">
              <CheckCircle2 className="w-4 h-4" /> Connected
            </span>
          </div>

          <div className="p-3 rounded-2xl bg-slate-50 text-xs space-y-1.5 text-slate-600 font-mono">
            <p>• Endpoint: https://amble-supabase.co</p>
            <p>• Table Sync: users, chats, messages, calls</p>
            <p>• Last Sync: {new Date(supabaseStatus.lastSynced).toLocaleTimeString()}</p>
          </div>

          {msg && (
            <p className="text-xs font-semibold text-emerald-600 text-center bg-emerald-50 py-1.5 rounded-xl">
              {msg}
            </p>
          )}
        </div>

        <div className="flex gap-2">
          <button
            onClick={handleSync}
            disabled={syncing}
            className="flex-1 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-2xl text-xs font-bold transition-all shadow-md flex items-center justify-center gap-2"
          >
            <RefreshCw className={`w-4 h-4 ${syncing ? 'animate-spin' : ''}`} />
            {syncing ? 'Syncing...' : 'Sync Now'}
          </button>
          <button
            onClick={onDismiss}
            className="py-2.5 px-4 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-2xl text-xs font-semibold"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
