"use client";

import "./globals.css";
import Header from "./components/Header/Header";
import Sidebar from "./components/Sidebar/Sidebar";
import { ServiceProvider, useService } from "./context/ServiceContext";
import MainContent from "./components/MainContent/MainContent";
import ChatBox from "./components/ChatBox/ChatBox";
import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";
import { serviceConfig } from "./config/serviceConfig";
import MeetingList from "./components/MeetingList/MeetingList";
import InvestmentReport from "./components/InvestmentReport/InvestmentReport";
import AdminDashboard from "./components/AdminDashboard/AdminDashboard";

function LayoutContent({ children }: { children: React.ReactNode }) {
  const { currentService, pageState, setCurrentService } = useService();

  const renderContent = () => {
    // 관리자 페이지 처리
    if (pageState === 'admin') {
      return <AdminDashboard />;
    }

    // 채팅 서비스 설정
    const chatServices = ["general-chat", "branch-manual", "overseas-loan", "financial-statements"];
    
    if (chatServices.includes(currentService)) {
      if (pageState === 'select') {
        return <MainContent />;
      } else if (pageState === 'chat') {
        const config = serviceConfig[currentService];
        return (
          <CopilotKit
            runtimeUrl={config.apiEndpoint}
            agent="olaf_ibk_poc_agent"
            showDevConsole={false}
          >
            <ChatBox 
              serviceName={currentService}
              initialInput=""
              agent="olaf_ibk_poc_agent"
              useCopilot={true}
              runtimeUrl={config.apiEndpoint}
            />
          </CopilotKit>
        );
      }
    }

    // 다른 서비스들 처리
    switch(currentService) {
      case 'meeting-minutes':
        return <MeetingList />;
      case 'investment-report':
        return <InvestmentReport />;
      default:
        return children; // 기본적으로 children을 렌더링
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