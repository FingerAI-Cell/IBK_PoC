"use client";

import "./globals.css";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import { ServiceProvider, useService } from "./context/ServiceContext";
import MainContent from "./components/MainContent/MainContent";
import ChatBox from "./components/ChatBox/ChatBox";
import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";
import { Chat } from "./chatbot/chat";

function LayoutContent({ children }: { children: React.ReactNode }) {
  const { currentService, pageState, setCurrentService } = useService();

  const renderContent = () => {
    if (currentService === "general-chat") {
      return (
        <CopilotKit
          runtimeUrl="/api/onelineai/olaf"
          agent="olaf_ibk_poc_agent"
          showDevConsole={false}
        >
          <ChatBox 
            serviceName="general-chat"
            initialInput=""
            agent="olaf_ibk_poc_agent"
            useCopilot={true}
            runtimeUrl="/api/onelineai/olaf"
          />
        </CopilotKit>
      );
    }

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

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <ServiceProvider>
      <html lang="en">
        <LayoutContent>{children}</LayoutContent>
      </html>
    </ServiceProvider>
  );
}