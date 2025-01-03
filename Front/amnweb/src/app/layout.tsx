"use client";

import "./globals.css";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import { ServiceProvider, useService } from "./context/ServiceContext";
import MainContent from "./components/MainContent/MainContent";
import ChatBox from "./components/ChatBox/ChatBox";

function LayoutContent() {
  const { currentService, pageState, setCurrentService } = useService();

  const renderContent = () => {
    switch(pageState) {
      case 'select':
        return <MainContent />;
      case 'chat':
        return <ChatBox 
          serviceName={currentService}
          sendApiRequest={async (message) => {
            const response = await fetch("/api/chat", {
              method: "POST",
              headers: { "Content-Type": "application/json" },
              body: JSON.stringify({ 
                message,
                service: currentService 
              }),
            });

            if (!response.ok) {
              throw new Error("API 요청 실패");
            }

            const data = await response.json();
            return data.reply;
          }}
          initialInput=""
        />;
      default:
        return <MainContent />;
    }
  };

  return (
    <body className="flex min-h-screen bg-gray-100">
      <Sidebar 
        currentService={currentService}
        selectService={setCurrentService}
      />
      <div className="flex-grow flex flex-col ml-64">
        <Header />
        <main className="p-6 mt-16">
          {renderContent()}
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