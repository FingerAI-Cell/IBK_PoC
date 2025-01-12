"use client";

import { useEffect, memo, useState, useRef, useCallback } from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
import styles from "./ChatBox.module.css";

interface ChatBoxProps {
  initialInput?: string;
  agent?: string;
  serviceName?: string;
  useCopilot?: boolean;
}

// 문서 메타데이터 타입
interface DocumentMetadata {
  page_number: number;
  page_chunk_number: number;
  file_name: string;
  file_url: string;
  main_topic: string;
  sub_topic: string;
  keywords: string[];
}

// 문서 항목 타입
interface RetrievedDocument {
  lc: number;
  type: string;
  id: string[];
  kwargs: {
    metadata: DocumentMetadata;
    page_content: string;
  };
}

interface Message {
  id: string;
  sender: "user" | "bot";
  text: string;
  timestamp: Date;
}

interface CoAgentState {
  routing_vectordb_collection: string;
  retrieved_documents: RetrievedDocument[];
  context: string;
}

// 메시지 컴포넌트 (메모이제이션으로 렌더링 최적화)
const ChatMessage = memo(({ sender, text }: { sender: string; text: string }) => (
  <div
    className={`${styles.message} ${
      sender === "user" ? styles.userMessage : styles.botMessage
    }`}
  >
    {text.split("\n").map((line, i) => (
      <span key={i}>
        {line}
        {i !== text.split("\n").length - 1 && <br />}
      </span>
    ))}
  </div>
));
ChatMessage.displayName = "ChatMessage";

export default function ChatBox({
  initialInput = "",
  agent,
  serviceName,
  useCopilot = false,
}: ChatBoxProps) {
  const [shouldSubmitInitial, setShouldSubmitInitial] = useState(true);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  const handleSubmitMessage = useCallback(async (message: string) => {
    // 여기에 메시지 전송 관련 로직 추가 가능
    console.log(`메시지 전송: ${message}`);
  }, []);

  useEffect(() => {
    if (initialInput?.trim() && shouldSubmitInitial) {
      handleSubmitMessage(initialInput);
      setShouldSubmitInitial(false);
    }
  }, [initialInput, handleSubmitMessage, shouldSubmitInitial]);

  // 서비스 로깅 (히스토리 추적용)
  useEffect(() => {
    console.log(`현재 서비스: ${serviceName}`);
  }, [serviceName]);

  // 문서 관련 상태 (from useCoAgent)
  const { nodeName, state } = useCoAgent<CoAgentState>({
    name: agent || "",
    initialState: {
      nodeName: "",
      running: false,
      state: {
        routing_vectordb_collection: "",
        retrieved_documents: [],
        context: "",
      },
    },
  });

  const [documents, setDocuments] = useState<RetrievedDocument[]>([]);

  // 문서 상태 업데이트
  useEffect(() => {
    if (nodeName === "__end__" && state.retrieved_documents.length > 0) {
      const uniqueDocs = state.retrieved_documents.filter(
        (doc, index, self) =>
          index ===
          self.findIndex(
            (d) => d.kwargs.metadata.file_name === doc.kwargs.metadata.file_name
          )
      );
      setDocuments(uniqueDocs);
    } else {
      setDocuments([]);
    }
  }, [nodeName, state.retrieved_documents]);

  // DOM 변경 시 스크롤 유지
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  // DOM 변경 감지 및 문서 추가
  useEffect(() => {
    const observer = new MutationObserver((mutations) => {
      mutations.forEach((mutation) => {
        mutation.addedNodes.forEach((node) => {
          if (
            node instanceof HTMLElement &&
            node.classList.contains("copilotKitAssistantMessage") &&
            !node.dataset.inserted &&
            documents.length > 0
          ) {
            // 약간의 지연을 주어 메시지가 완전히 렌더링된 후 문서를 추가
            setTimeout(() => {
              const docContainer = document.createElement("div");
              docContainer.className = "p-4 mt-2 border-l-4 border-blue-500";
              docContainer.innerHTML = `<h3 class="text-sm font-semibold">📄 관련 문서</h3>`;

              documents.forEach((doc) => {
                const keywordsHTML = doc.kwargs.metadata.keywords
                  .map((keyword) => `#${keyword}`)
                  .join(" ");
                const docElement = document.createElement("div");
                docElement.innerHTML = `
                  <div class="p-2 border rounded shadow-sm mt-2">
                    <a href="${doc.kwargs.metadata.file_url}" target="_blank" class="text-blue-600 hover:underline">
                      ${doc.kwargs.metadata.file_name}
                    </a>
                    <p class="text-xs">${doc.kwargs.metadata.main_topic}</p>
                    <p class="text-xs text-gray-500">${keywordsHTML}</p>
                  </div>`;
                docContainer.appendChild(docElement);
              });

              node.appendChild(docContainer);
              node.dataset.inserted = "true";
            }, 100);
          }
        });
      });
    });

    if (chatContainerRef.current) {
      observer.observe(chatContainerRef.current, {
        childList: true,
        subtree: true,
      });
    }

    return () => observer.disconnect();
  }, [documents]);

  return (
    <div ref={chatContainerRef} className={styles.container}>
      {useCopilot && agent && (
        <CopilotChat
          className={styles.copilotChat}
          onSubmitMessage={handleSubmitMessage}
          labels={{
            title: "IBK 투자증권 업무 효율화 챗봇",
            initial: "안녕하세요. IBK 투자증권 업무 효율화 챗봇입니다. 무엇을 도와드릴까요?",
            placeholder: "메시지를 입력하세요...",
          }}
        />
      )}
      <div ref={messageEndRef} />
    </div>
  );
}