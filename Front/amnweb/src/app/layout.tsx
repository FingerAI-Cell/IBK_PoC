"use client";

import "./globals.css";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import { ServiceProvider, useService } from "./context/ServiceContext";
import MainContent from "./components/MainContent/MainContent";

function LayoutContent() {
  const { currentService, setCurrentService, isSidebarOpen, toggleSidebar } = useService();

  return (
    <body className="flex min-h-screen bg-gray-100">
      {/* 토글 버튼 */}
      <button 
        className="fixed top-4 left-4 z-40 xl:hidden"
        onClick={toggleSidebar}
      >
        <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
        </svg>
      </button>

      {/* 사이드바 */}
      <div className={`
        ${isSidebarOpen ? 'block' : 'hidden'} 
        xl:block
      `}>
        <Sidebar 
          currentService={currentService}
          selectService={setCurrentService}
        />
      </div>

      {/* 메인 컨테이너 (헤더 + 컨텐츠) */}
      <div className="flex-grow flex flex-col">
        <Header />
        <main className="p-6 mt-16">
          <MainContent />
        </main>
      </div>
    </body>
  );
}

export default function RootLayout() {
  return (
    <ServiceProvider>
      <html lang="en">
        <LayoutContent />
      </html>
    </ServiceProvider>
  );
}