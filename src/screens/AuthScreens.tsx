import React, { useState } from 'react';
import { useChat } from '../context/ChatContext';
import { Mail, Phone, Lock, User, Sparkles, ArrowRight, CheckSquare, Square } from 'lucide-react';

const avatarPresets = [
  "https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?w=150",
  "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
  "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
  "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150"
];

export const AuthScreens: React.FC = () => {
  const { signIn, signUp, signInWithPhone, signUpWithPhone } = useChat();
  const [authMode, setAuthMode] = useState<'signin' | 'signup'>('signin');
  const [loginMethod, setLoginMethod] = useState<'email' | 'phone'>('email');

  // Form states
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('+15550100');
  const [name, setName] = useState('');
  const [statusQuote, setStatusQuote] = useState('');
  const [selectedAvatar, setSelectedAvatar] = useState(avatarPresets[0]);
  const [rememberLogin, setRememberLogin] = useState(true);
  const [loading, setLoading] = useState(false);
  const [errorMsg, setErrorMsg] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setErrorMsg('');

    try {
      if (loginMethod === 'email') {
        if (authMode === 'signin') {
          if (!email) throw new Error('Please enter email address');
          await signIn(email, rememberLogin);
        } else {
          if (!name || !email) throw new Error('Please enter name and email');
          await signUp(name, email, statusQuote, selectedAvatar, rememberLogin);
        }
      } else {
        if (!phoneNumber) throw new Error('Please enter phone number');
        if (authMode === 'signin') {
          await signInWithPhone(phoneNumber, rememberLogin);
        } else {
          if (!name) throw new Error('Please enter your name');
          await signUpWithPhone(name, phoneNumber, statusQuote, selectedAvatar, rememberLogin);
        }
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Authentication failed. Please check credentials.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="min-h-screen w-full bg-gradient-to-br from-[#EAF2FB] via-[#D6E8FA] to-[#C2DDFA] flex items-center justify-center p-4">
      <div className="w-full max-w-md bg-white/90 backdrop-blur-xl rounded-3xl p-7 shadow-2xl border border-white/80 animate-fade-in">
        {/* Logo & Header */}
        <div className="text-center mb-6">
          <div className="inline-flex p-3 rounded-2xl bg-blue-600/10 text-blue-600 mb-2">
            <Sparkles className="w-7 h-7" />
          </div>
          <h2 className="text-2xl font-extrabold text-[#1B2A5E]">Welcome to AmBle</h2>
          <p className="text-xs text-slate-500 mt-1">High-fidelity chat, VoIP & WebRTC live calling</p>
        </div>

        {/* Login Method Tabs */}
        <div className="flex p-1 bg-slate-100 rounded-2xl mb-5 text-xs font-bold text-slate-600">
          <button
            type="button"
            onClick={() => setLoginMethod('email')}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMethod === 'email' ? 'bg-white text-blue-600 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <Mail className="w-3.5 h-3.5" /> Email Auth
          </button>
          <button
            type="button"
            onClick={() => setLoginMethod('phone')}
            className={`flex-1 py-2 rounded-xl transition-all flex items-center justify-center gap-1.5 ${
              loginMethod === 'phone' ? 'bg-white text-blue-600 shadow-sm' : 'hover:text-slate-900'
            }`}
          >
            <Phone className="w-3.5 h-3.5" /> Phone Number
          </button>
        </div>

        {errorMsg && (
          <div className="mb-4 p-3 bg-red-50 border border-red-200 text-red-600 text-xs rounded-xl font-medium">
            {errorMsg}
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-3">
          {authMode === 'signup' && (
            <div>
              <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">Full Name</label>
              <div className="relative">
                <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="text"
                  placeholder="e.g. Alex Mercer"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
          )}

          {loginMethod === 'email' ? (
            <div>
              <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">Email Address</label>
              <div className="relative">
                <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="email"
                  placeholder="alex.mercer@chatwave.io"
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
          ) : (
            <div>
              <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">Phone Number (+Country Code)</label>
              <div className="relative">
                <Phone className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
                <input
                  type="tel"
                  placeholder="+15550100"
                  value={phoneNumber}
                  onChange={(e) => setPhoneNumber(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:border-blue-500"
                />
              </div>
            </div>
          )}

          <div>
            <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">Password</label>
            <div className="relative">
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-3" />
              <input
                type="password"
                placeholder="••••••••"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full bg-slate-50 border border-slate-200 rounded-2xl pl-10 pr-4 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:border-blue-500"
              />
            </div>
          </div>

          {authMode === 'signup' && (
            <>
              <div>
                <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">Status Quote</label>
                <input
                  type="text"
                  placeholder="Hey there! I am using AmBle."
                  value={statusQuote}
                  onChange={(e) => setStatusQuote(e.target.value)}
                  className="w-full bg-slate-50 border border-slate-200 rounded-2xl px-4 py-2.5 text-xs text-slate-800 font-medium focus:outline-none focus:border-blue-500"
                />
              </div>

              <div>
                <label className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1.5">Choose Avatar Preset</label>
                <div className="flex gap-2">
                  {avatarPresets.map((imgUrl, idx) => (
                    <img
                      key={idx}
                      src={imgUrl}
                      alt="Avatar"
                      onClick={() => setSelectedAvatar(imgUrl)}
                      className={`w-11 h-11 rounded-full object-cover cursor-pointer border-2 transition-all ${
                        selectedAvatar === imgUrl ? 'border-blue-600 scale-105 shadow-md' : 'border-transparent opacity-60 hover:opacity-100'
                      }`}
                    />
                  ))}
                </div>
              </div>
            </>
          )}

          <div
            onClick={() => setRememberLogin(!rememberLogin)}
            className="flex items-center gap-2 pt-1 cursor-pointer text-xs font-semibold text-slate-600 select-none"
          >
            {rememberLogin ? (
              <CheckSquare className="w-4 h-4 text-blue-600 shrink-0" />
            ) : (
              <Square className="w-4 h-4 text-slate-400 shrink-0" />
            )}
            <span>Remember my login session</span>
          </div>

          <button
            type="submit"
            disabled={loading}
            className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white rounded-2xl text-xs font-bold transition-all shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 mt-4"
          >
            {loading ? 'Processing...' : authMode === 'signin' ? 'Sign In' : 'Create Account'}
            {!loading && <ArrowRight className="w-4 h-4" />}
          </button>
        </form>

        <div className="mt-5 text-center pt-4 border-t border-slate-100 text-xs text-slate-500 font-medium">
          {authMode === 'signin' ? (
            <p>
              Don't have an account?{' '}
              <button
                type="button"
                onClick={() => setAuthMode('signup')}
                className="text-blue-600 font-bold hover:underline"
              >
                Sign Up
              </button>
            </p>
          ) : (
            <p>
              Already registered?{' '}
              <button
                type="button"
                onClick={() => setAuthMode('signin')}
                className="text-blue-600 font-bold hover:underline"
              >
                Sign In
              </button>
            </p>
          )}
        </div>
      </div>
    </div>
  );
};
