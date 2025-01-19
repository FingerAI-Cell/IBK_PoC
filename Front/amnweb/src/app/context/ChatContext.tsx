"use client";

import { createContext, useState, useContext, ReactNode } from "react";

interface ChatContextType {
  isChatActive: boolean;
  chatInput: string;
  setChatInput: (input: string) => void;
  activateChat: (initialInput?: string) => void;
  deactivateChat: () => void;
}

const ChatContext = createContext<ChatContextType | undefined>(undefined);

export function ChatProvider({ children }: { children: ReactNode }) {
  const [isChatActive, setIsChatActive] = useState(false);
  const [chatInput, setChatInput] = useState("");

  const activateChat = (initialInput?: string) => {
    console.log("activate");
    if (initialInput) {
      setChatInput(initialInput);
    }
    setIsChatActive(true);
  };

  const deactivateChat = () => {
    setIsChatActive(false);
    setChatInput("");
  };

  return (
    <ChatContext.Provider
      value={{
        isChatActive,
        chatInput,
        setChatInput,
        activateChat,
        deactivateChat,
      }}
    >
      {children}
    </ChatContext.Provider>
  );
}

export function useChat() {
  const context = useContext(ChatContext);
  if (context === undefined) {
    throw new Error("useChat must be used within a ChatProvider");
  }
  return context;
}
