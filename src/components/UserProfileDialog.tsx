import React, { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { User, Shield, Lock, Eye, LogOut, X, Phone, Mail, CheckCircle, Database } from 'lucide-react';

interface Props {
  onDismiss: () => void;
  onOpenSupabase: () => void;
}

export const UserProfileDialog: React.FC<Props> = ({ onDismiss, onOpenSupabase }) => {
  const { currentUser, signOut, setUserOnlineStatus } = useChat();
  const [lastSeenPrivacy, setLastSeenPrivacy] = useState(currentUser?.lastSeenPrivacy || 'everyone');
  const [pin, setPin] = useState(currentUser?.twoStepPin || '');
  const [showPin, setShowPin] = useState(false);

  if (!currentUser) return null;

  return (
    <div className="fixed inset-0 z-50 bg-black/50 backdrop-blur-sm flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white rounded-3xl p-6 shadow-2xl border border-slate-100 max-h-[90vh] overflow-y-auto">
        <div className="flex items-center justify-between mb-4">
          <h3 className="text-base font-bold text-slate-800">Account & Profile</h3>
          <button onClick={onDismiss} className="p-1.5 hover:bg-slate-100 rounded-full text-slate-400">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* User Card */}
        <div className="flex items-center gap-3 p-3.5 bg-blue-50/70 rounded-2xl mb-4 border border-blue-100">
          <img
            src={currentUser.photoUrl}
            alt={currentUser.name}
            className="w-14 h-14 rounded-full object-cover border-2 border-white shadow-sm"
          />
          <div className="flex-1 overflow-hidden">
            <div className="flex items-center gap-1.5">
              <h4 className="text-sm font-bold text-slate-900 truncate">{currentUser.name}</h4>
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 shrink-0" />
            </div>
            <p className="text-xs text-slate-500 truncate flex items-center gap-1 mt-0.5">
              <Mail className="w-3 h-3 text-blue-500" /> {currentUser.email}
            </p>
            {currentUser.phoneNumber && (
              <p className="text-xs text-slate-500 truncate flex items-center gap-1 mt-0.5">
                <Phone className="w-3 h-3 text-emerald-500" /> {currentUser.phoneNumber}
              </p>
            )}
          </div>
        </div>

        {/* Status Bio */}
        <div className="mb-4">
          <label className="text-xs font-bold text-slate-500 uppercase tracking-wider block mb-1">Status Quote</label>
          <div className="p-3 bg-slate-50 rounded-2xl text-xs text-slate-700 italic border border-slate-100">
            "{currentUser.status}"
          </div>
        </div>

        {/* Privacy Settings */}
        <div className="space-y-3 mb-6">
          <h4 className="text-xs font-bold text-slate-500 uppercase tracking-wider">Privacy & Security</h4>
          
          <div className="flex items-center justify-between p-3 bg-slate-50 rounded-2xl text-xs">
            <div className="flex items-center gap-2">
              <Eye className="w-4 h-4 text-blue-600" />
              <span className="font-semibold text-slate-700">Last Seen Visibility</span>
            </div>
            <select
              value={lastSeenPrivacy}
              onChange={(e) => setLastSeenPrivacy(e.target.value as any)}
              className="bg-white border border-slate-200 rounded-xl px-2 py-1 font-medium text-slate-700 focus:outline-none"
            >
              <option value="everyone">Everyone</option>
              <option value="contacts">Contacts</option>
              <option value="nobody">Nobody</option>
            </select>
          </div>

          <div className="p-3 bg-slate-50 rounded-2xl text-xs space-y-2">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <Lock className="w-4 h-4 text-purple-600" />
                <span className="font-semibold text-slate-700">Two-Step Verification PIN</span>
              </div>
              <button
                onClick={() => setShowPin(!showPin)}
                className="text-[11px] text-blue-600 font-bold hover:underline"
              >
                {showPin ? 'Hide' : 'Configure'}
              </button>
            </div>
            {showPin && (
              <input
                type="password"
                maxLength={6}
                placeholder="Enter 6-digit PIN"
                value={pin}
                onChange={(e) => setPin(e.target.value)}
                className="w-full bg-white border border-slate-200 rounded-xl px-3 py-1.5 text-xs text-slate-800 font-mono focus:outline-none"
              />
            )}
          </div>

          {currentUser.isAdmin && (
            <button
              onClick={onOpenSupabase}
              className="w-full p-3 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200 rounded-2xl text-xs font-bold text-emerald-800 flex items-center justify-between transition-colors"
            >
              <span className="flex items-center gap-2">
                <Database className="w-4 h-4 text-emerald-600" /> Admin Supabase Integration
              </span>
              <span className="text-[10px] bg-emerald-600 text-white px-2 py-0.5 rounded-lg">Active</span>
            </button>
          )}
        </div>

        {/* Action Buttons */}
        <div className="flex gap-2">
          <button
            onClick={signOut}
            className="flex-1 py-2.5 bg-red-50 hover:bg-red-100 text-red-600 border border-red-200 rounded-2xl text-xs font-bold transition-all flex items-center justify-center gap-1.5"
          >
            <LogOut className="w-4 h-4" /> Sign Out
          </button>
          <button
            onClick={onDismiss}
            className="py-2.5 px-5 bg-slate-100 hover:bg-slate-200 text-slate-700 rounded-2xl text-xs font-semibold"
          >
            Close
          </button>
        </div>
      </div>
    </div>
  );
};
