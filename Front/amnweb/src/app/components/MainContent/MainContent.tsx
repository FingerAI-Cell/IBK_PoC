"use client";

import { useState, useEffect } from "react";
import styles from "./MainContent.module.css";
import ChatBox from "../ChatBox/ChatBox";
import GreetingSection from "./GreetingSection";
import FAQSection from "./FAQSection";
import MeetingList from "../MeetingList/MeetingList";
import AdminDashboard from "../AdminDashboard/AdminDashboard";
import { faqs } from "../../data/faqData";
import { useService } from "../../context/ServiceContext";
import InvestmentReport from "../InvestmentReport/InvestmentReport";

export default function MainContent() {
  const { currentService, pageState, setPageState} = useService();
  const [isChatting, setIsChatting] = useState(false);
  const [chatInput, setChatInput] = useState("");

  useEffect(() => {
    setIsChatting(false);
    setChatInput("");
  }, [currentService]);

  const handleQuestionSubmit = () => {
    if (chatInput.trim()) {
      setIsChatting(true);
      setPageState('chat');
    }
  };

  const handleFAQClick = (question: string) => {
    setChatInput(question);
    setIsChatting(true);
    setPageState('chat');
  };

  return (
    <div className={styles.container}>
      {pageState === 'admin' ? (
        <AdminDashboard />
      ) : currentService === "meeting-minutes" ? (
        <MeetingList />
      ) : currentService === "investment-report" ? (
        <InvestmentReport />
      ) : !isChatting ? (
        <>
          {/* 인사 및 FAQ 섹션 */}
          <GreetingSection
            chatInput={chatInput}
            onInputChange={setChatInput}
            onSubmit={handleQuestionSubmit}
            serviceType={currentService}
          />
          <FAQSection faqs={faqs} onFAQClick={handleFAQClick} />
        </>
      ) : (
        <>
          {/* 채팅 박스 및 FAQ */}
          <ChatBox
            initialInput={chatInput}
            agent="olaf_ibk_poc_agent"
            useCopilot={true}
          />
        </>
      )}
    </div>
  );
}
