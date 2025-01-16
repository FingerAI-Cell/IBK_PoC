"use client";

import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import MainContent from "./components/MainContent/MainContent";
import { useService } from "./context/ServiceContext";

export default function ClientLayout() {
  const { currentService, setCurrentService, isSidebarOpen, toggleSidebar } = useService();

  return (
    <>
      <button 
        className="fixed top-4 left-4 z-40 block xl:hidden"
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
        <Header />
        <main className="p-6 mt-16">
          <MainContent />
        </main>
      </div>
    </>
  );
} 