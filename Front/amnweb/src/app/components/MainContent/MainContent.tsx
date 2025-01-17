"use client";

import { useState, useEffect } from "react";
import styles from "./MainContent.module.css";
import ChatBox from "../ChatBox/ChatBox";
import GreetingSection from "./GreetingSection";
import MeetingList from "../MeetingList/MeetingList";
import AdminDashboard from "../AdminDashboard/AdminDashboard";
import { faqData } from "../../data/faqData";
import { useService } from "../../context/ServiceContext";
import InvestmentReport from "../InvestmentReport/InvestmentReport";
import { serviceConfig } from "../../config/serviceConfig";
import { CopilotKit } from "@copilotkit/react-core";
import React from "react";
import { useGlobalKey } from "@/app/context/GlobalKeyContext";

const MemoizedCopilotKit = React.memo(function CopilotKitWrapper({
  config,
  globalKey,
}: {
  config: any; // config 타입에 맞게 정의 필요
  globalKey: string | null; // 글로벌 키
}) {
  if (!config || !globalKey) return null; // 글로벌 키가 없으면 렌더링하지 않음

  console.log("CopilotKit Rendered with key:", globalKey);

  return (
    <CopilotKit
      key={globalKey} // 키 기반으로 상태 유지
      runtimeUrl={config.apiEndpoint}
      agent={config.agent}
      showDevConsole={false}
    >
      <ChatBox
        initialInput=""
        serviceName={config.serviceName}
        agent={config.agent}
        useCopilot={config.useCopilot}
      />
    </CopilotKit>
  );
});

export default function MainContent() {
  console.log("MainContent component rendered");
  const { currentService, pageState, setPageState } = useService();
  const [chatInput, setChatInput] = useState("");
  const [isTransitioning, setIsTransitioning] = useState(false);
  const { globalKey, setGlobalKey } = useGlobalKey();
  const config = React.useMemo(() => {
    const cfg = serviceConfig[currentService];
    console.log("Config for currentService:", cfg);
    return cfg;
  }, [currentService]);

  useEffect(() => {
    setChatInput("");
    setIsTransitioning(false);
  }, [currentService, pageState]);

  useEffect(() => {
    if (pageState === "chat") {
      setIsTransitioning(false); // transition 상태 초기화
      console.log("Chat state activated with:", { currentService, chatInput });
    }
  }, [pageState]); // pageState가 변경될 때만 실행

  const handleQuestionSubmit = () => {
    if (chatInput.trim()) {
      setIsTransitioning(true);
      setPageState("chat"); // 바로 상태를 변경
    }
  };

  const handleFAQClick = (question: string) => {
    setChatInput(question);
    setIsTransitioning(true);
    setPageState("chat"); // 바로 상태를 변경
  };
  const getServiceFaqs = (serviceType: string) => {
    return faqData[serviceType as keyof typeof faqData] || faqData['general-chat'];
  };

  // 키 설정 로직
  useEffect(() => {
    if (currentService && !globalKey) {
      setGlobalKey(`copilot-${currentService}`); // 현재 서비스 기반 키 생성
    }
  }, [currentService, globalKey, setGlobalKey]);

  // 키 제거 또는 초기화 (예: 특정 조건에서)
  const handleKeyReset = () => {
    setGlobalKey(null); // 키 제거
  };

  // Chat 서비스 렌더링 함수
  const renderChatService = () => {
    if (!config || (pageState !== "chat" && pageState !== "select")) {
      return null;
    }

    if (pageState === "select") {
      return (
        <GreetingSection
          chatInput={chatInput}
          onInputChange={setChatInput}
          onSubmit={handleQuestionSubmit}
          serviceType={currentService}
          faqs={faqData[currentService as keyof typeof faqData] || faqData["general-chat"]}
          onFAQClick={handleFAQClick}
        />
      );
    }

    // MemoizedCopilotKit 사용
    return <MemoizedCopilotKit config={config} globalKey={globalKey} />;
  };

  return (
    <div className={styles.container}>
      {pageState === "admin" ? (
        <AdminDashboard />
      ) : currentService === "meeting-minutes" ? (
        <MeetingList />
      ) : currentService === "investment-report" ? (
        <InvestmentReport />
      ) : (
        renderChatService()
      )}
      <button onClick={handleKeyReset}>Reset Key</button>
    </div>
  );
}