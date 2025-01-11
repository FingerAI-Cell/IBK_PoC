"use client";

import { useEffect, memo, useState, useRef } from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import styles from "./ChatBox.module.css";

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

interface ChatBoxProps {
  agent: string;
}

export default function ChatBox({ agent }: ChatBoxProps) {
  const {
    nodeName,
    running,
    state
  } = useCoAgent<CoAgentState>({
    name: agent,
    initialState: {
      nodeName: '',
      running: false,
      state: {
        routing_vectordb_collection: '',
        retrieved_documents: [],
        context: '',
      },
    },
  });
  
  const [documents, setDocuments] = useState<RetrievedDocument[]>([]);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // 새로운 문서가 도착할 때 상태 업데이트
  useEffect(() => {
    if (nodeName === '__end__' && state.retrieved_documents.length > 0) {
      const extractedDocs = state.retrieved_documents;

      // 중복 제거 (file_name 기준)
      const uniqueDocs = extractedDocs.filter(
        (doc, index, self) =>
          index ===
          self.findIndex((d) => d.kwargs.metadata.file_name === doc.kwargs.metadata.file_name)
      );
      setDocuments(uniqueDocs);
    } else {
      setDocuments([]); // 문서가 없으면 빈 배열로 초기화
    }
  }, [nodeName, running, state]);

 // DOM 변경 감지 및 문서 추가
 useEffect(() => {
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.addedNodes.length) {
        const messages = document.querySelectorAll('.copilotKitAssistantMessage');
        const lastMessage = messages[messages.length - 1] as HTMLDivElement;

        // 문서가 존재하고, 중복이 아닌 경우에만 문서 삽입
        if (lastMessage && !lastMessage.dataset.inserted && documents.length > 0) {
          const docContainer = document.createElement('div');
          docContainer.className = "p-4 mt-2 border-l-4 border-blue-500";
          docContainer.innerHTML = `<h3 class="text-sm font-semibold">📄 관련 문서</h3>`;

          // 문서 컴포넌트 생성 및 렌더링
          documents.forEach(doc => {
            const docElement = document.createElement('div');
            const keywordsHTML = doc.kwargs.metadata.keywords
              .map((keyword) => `#${keyword}`)
              .join(' '); // 키워드를 공백으로 구분
            
            docElement.innerHTML = `
              <div class="p-2 border rounded shadow-sm mt-2">
                <a href="${doc.kwargs.metadata.file_url}" target="_blank" class="text-blue-600 hover:underline">
                  ${doc.kwargs.metadata.file_name}
                </a>
                <p class="text-xs">${doc.kwargs.metadata.main_topic}</p>
                <p class="text-xs">페이지 번호: ${doc.kwargs.metadata.page_number}</p>
                <p class="text-xs">페이지 수: ${doc.kwargs.metadata.page_chunk_number}</p>
                <p class="text-xs text-gray-500">${keywordsHTML}</p> <!-- 키워드 섹션 -->
              </div>
            `;
            docContainer.appendChild(docElement);
          });

          lastMessage.appendChild(docContainer);
          lastMessage.dataset.inserted = "true";  // 중복 방지
        }
      }
    });
  });

  if (chatContainerRef.current) {
    observer.observe(chatContainerRef.current, {
      childList: true,
      subtree: true,
    });
  }

  return () => observer.disconnect();
}, [documents]); // documents가 변경될 때만 재감지

return (
  <div ref={chatContainerRef} className="h-full w-full flex flex-col font-noto">
    <CopilotChat
      className="flex-1"
      labels={{
        title: "IBK 투자증권 업무 효율화 챗봇",
        initial: "안녕하세요. IBK 투자증권 업무 효율화 챗봇입니다. 무엇을 도와드릴까요?",
      }}
    />
  </div>
);
}