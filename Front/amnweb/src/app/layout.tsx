"use client";

import "./globals.css";

import { useState } from "react";
import Header from "./components/Header";
import Sidebar from "./components/Sidebar";
import MainContent from "./components/MainContent";

export default function RootLayout() {
  const [sidebarOpen, setSidebarOpen] = useState(false); // 사이드바 기본 닫힘 상태
  const [currentService, setCurrentService] = useState("default"); // 현재 서비스 상태

  const resetToMain = () => setCurrentService("default"); // 메인 화면으로 전환

  return (
    <html lang="en">
      <body className="flex min-h-screen bg-gray-100">
        {/* 사이드바 */}
        <Sidebar
          isOpen={sidebarOpen}
          selectService={(service) => setCurrentService(service)}
          toggleSidebar={() => setSidebarOpen(!sidebarOpen)}
        />

        {/* 메인 콘텐츠 */}
        <div className={`flex-grow flex flex-col ${sidebarOpen ? "ml-64" : "ml-0"}`}>
          <Header toggleSidebar={() => setSidebarOpen(!sidebarOpen)} resetToMain={resetToMain} />
          <main className="p-6 mt-16">
            <MainContent currentService={currentService} />
          </main>
        </div>
      </body>
    </html>
  );
}