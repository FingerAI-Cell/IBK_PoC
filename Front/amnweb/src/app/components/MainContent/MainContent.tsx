"use client";
// 업데이트

import { useState, useEffect } from "react";
import styles from "./MainContent.module.css";
import ChatBox from "../ChatBox/ChatBox";
import GreetingSection from "./GreetingSection";
import FAQSection from "./FAQSection";
import { faqs } from "../../data/faqData";
import { useService } from "../../context/ServiceContext";
import { serviceConfig } from "../../config/serviceConfig";

export default function MainContent() {
  const { currentService, setCurrentService } = useService();
  const [isChatting, setIsChatting] = useState(false);
  const [chatInput, setChatInput] = useState("");

  const currentConfig = serviceConfig[currentService] || serviceConfig["general-chat"];

  const sendApiRequest = async (message: string) => {
    const response = await fetch(currentConfig.apiEndpoint, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ message }),
    });

    if (!response.ok) {
      throw new Error("API 요청 실패");
    }

    const data = await response.json();
    return data.reply;
  };

  useEffect(() => {
    setIsChatting(false);
    setChatInput("");
  }, [currentService]);

  const handleQuestionSubmit = () => {
    if (chatInput.trim()) {
      setIsChatting(true);
    }
  };

  const handleFAQClick = (question: string) => {
    setChatInput(question);
    setIsChatting(true);
  };

  const handleReset = () => {
    setCurrentService("general-chat");
    setIsChatting(false);
    setChatInput("");
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.serviceTitle}>{currentConfig.title}</h2>
      {!isChatting ? (
        <>
          <GreetingSection
            chatInput={chatInput}
            onInputChange={setChatInput}
            onSubmit={handleQuestionSubmit}
            placeholder={currentConfig.defaultMessage}
          />
          <FAQSection faqs={faqs} onFAQClick={handleFAQClick} />
        </>
      ) : (
        <ChatBox
          sendApiRequest={sendApiRequest}
          initialInput={chatInput}
          onReset={handleReset}
          serviceName={currentConfig.title}
          showReset={false}
        />
      )}
    </div>
  );
}
