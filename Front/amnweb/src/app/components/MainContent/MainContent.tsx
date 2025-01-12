"use client";

import { useState, useEffect } from "react";
import styles from "./MainContent.module.css";
import ChatBox from "../ChatBox/ChatBox";
import GreetingSection from "./GreetingSection";
import MeetingList from "../MeetingList/MeetingList";
import AdminDashboard from "../AdminDashboard/AdminDashboard";
import { faqs } from "../../data/faqData";
import { useService } from "../../context/ServiceContext";
import InvestmentReport from "../InvestmentReport/InvestmentReport";
import { serviceConfig } from "../../config/serviceConfig";
import { CopilotKit } from "@copilotkit/react-core";

export default function MainContent() {
  const { currentService, pageState, setPageState } = useService();
  const [chatInput, setChatInput] = useState("");
  const [isTransitioning, setIsTransitioning] = useState(false);

  useEffect(() => {
    setChatInput("");
    setIsTransitioning(false);
  }, [currentService, pageState]);

  const handleQuestionSubmit = () => {
    if (chatInput.trim()) {
      setIsTransitioning(true);
      // 약간의 지연 후 페이지 상태 변경
      setTimeout(() => {
        setPageState('chat');
      }, 100);
    }
  };

  const handleFAQClick = (question: string) => {
    setChatInput(question);
    setIsTransitioning(true);
    // 약간의 지연 후 페이지 상태 변경
    setTimeout(() => {
      setPageState('chat');
    }, 100);
  };

  const renderChatService = () => {
    const config = serviceConfig[currentService];
    
    if (pageState === 'select') {
      return (
        <GreetingSection
          chatInput={chatInput}
          onInputChange={setChatInput}
          onSubmit={handleQuestionSubmit}
          serviceType={currentService}
          faqs={faqs}
          onFAQClick={handleFAQClick}
        />
      );
    }

    // pageState가 'chat'일 때
    return (
      <CopilotKit
        runtimeUrl={config.apiEndpoint}
        agent={config.agent}
        showDevConsole={false}
      >
        <ChatBox 
          initialInput={isTransitioning ? chatInput : ""}
          serviceName={currentService}
          agent={config.agent}
          useCopilot={config.useCopilot}
        />
      </CopilotKit>
    );
  };

  return (
    <div className={styles.container}>
      {pageState === 'admin' ? (
        <AdminDashboard />
      ) : currentService === "meeting-minutes" ? (
        <MeetingList />
      ) : currentService === "investment-report" ? (
        <InvestmentReport />
      ) : (
        // 채팅 서비스 렌더링 (general-chat, branch-manual, overseas-loan, financial-statements)
        renderChatService()
      )}
    </div>
  );
}