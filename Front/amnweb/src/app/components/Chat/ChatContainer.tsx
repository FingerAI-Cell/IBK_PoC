"use client";

import { useChat } from "@/app/context/ChatContext";
import { useService } from "@/app/context/ServiceContext";
import GreetingSection from "../MainContent/GreetingSection";
import { faqData } from "@/app/data/faqData";

export default function ChatContainer() {
  const currentService = useService().currentService;
  const { chatInput, setChatInput, activateChat } = useChat();

  const handleQuestionSubmit = () => {
    if (chatInput.trim()) {
      activateChat(chatInput); // 채팅 활성화 및 초기 입력값 설정
      console.log("ChatContainer - chatInput:", chatInput);
    }
    console.log("1ChatContainer - chatInput:", chatInput);
  };

  const handleFAQClick = (question: string) => {
    activateChat(question); // FAQ 클릭 시 채팅 활성화
  };

  return (
        <GreetingSection
          chatInput={chatInput}
          onInputChange={setChatInput}
          onSubmit={handleQuestionSubmit}
          serviceType={currentService as keyof typeof faqData}
          faqs={faqData[currentService as keyof typeof faqData] || faqData["general-chat"]}
          onFAQClick={handleFAQClick}
        />
  )
}
