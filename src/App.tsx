import React from 'react';
import { ChatProvider, useChat } from './context/ChatContext';
import { FloatingPipCallOverlay } from './components/FloatingPipCallOverlay';
import { AmBleLogoRevealScreen } from './screens/AmBleLogoRevealScreen';
import { AuthScreens } from './screens/AuthScreens';
import { HomeScreen } from './screens/HomeScreen';
import { ChatDetailScreen } from './screens/ChatDetailScreen';
import { CallScreen } from './screens/CallScreen';
import { GeminiLiveVoiceScreen } from './screens/GeminiLiveVoiceScreen';

const MainAppRouter: React.FC = () => {
  const { currentScreen, isLoggedIn } = useChat();

  const renderScreen = () => {
    if (currentScreen === 'logo_reveal') {
      return <AmBleLogoRevealScreen />;
    }

    if (!isLoggedIn || currentScreen === 'auth') {
      return <AuthScreens />;
    }

    switch (currentScreen) {
      case 'home':
        return <HomeScreen />;
      case 'chat_detail':
        return <ChatDetailScreen />;
      case 'call':
        return <CallScreen />;
      case 'gemini_live_voice':
        return <GeminiLiveVoiceScreen />;
      default:
        return <HomeScreen />;
    }
  };

  return (
    <div className="min-h-screen bg-[#EAF2FB] font-[#1B2A5E] antialiased relative">
      {renderScreen()}
      <FloatingPipCallOverlay />
    </div>
  );
};

export function App() {
  return (
    <ChatProvider>
      <MainAppRouter />
    </ChatProvider>
  );
}

export default App;
