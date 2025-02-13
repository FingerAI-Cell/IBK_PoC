"use client";

import { ReactNode } from "react";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import MainContent from "./components/MainContent/MainContent";
import ChatBox from "./components/ChatBox/ChatBox";
import { useService } from "./context/ServiceContext";
import { useChat } from "./context/ChatContext";

interface ClientLayoutProps {
  children: ReactNode;
}

export default function ClientLayout({ children }: ClientLayoutProps) {
  const { currentService, setCurrentService, isSidebarOpen, toggleSidebar } = useService();
  const { isChatActive } = useChat();
  // ClientLayout 마운트 및 상태 확인 로그
  console.log("ClientLayout 렌더링");
  return (
    <div className="relative min-h-screen">
      <button 
        className="fixed top-2 left-4 z-40 block xl:hidden"
        onClick={toggleSidebar}
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      <div className={`
        ${isSidebarOpen ? 'block' : 'hidden'} 
        xl:block
        z-30
      `}>
        <Sidebar 
          currentService={currentService}
          selectService={setCurrentService}
        />
      </div>

      <div className="flex-grow flex flex-col">
        {/* <Header /> */}
        <main className="p-6">
          {!isChatActive && (
            <>
              <MainContent />
              {children}
            </>
          )}
          {isChatActive && (
            <div className="fixed inset-0 xl:pl-80">
              <ChatBox />
            </div>
          )}
        </main>
      </div>
    </div>
  );
} 