"use client";

import ChatBox from "./components/ChatBox/ChatBox";
import { useService } from "./context/ServiceContext";
import { serviceConfig } from "./config/serviceConfig";

export default function ChatPage() {
  const { currentService } = useService();
  const currentConfig = serviceConfig[currentService];

  const sendApiRequest = async (message: string) => {
    const response = await fetch("/api/chat", {
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

  return (
    <div>
      <h1>Chat Page</h1>
      <ChatBox
        sendApiRequest={sendApiRequest}
        initialInput="" // 기본값
        onReset={() => console.log("Chat reset!")} // 리셋 동작
        serviceName={currentConfig.title}
      />
    </div>
  );
}
