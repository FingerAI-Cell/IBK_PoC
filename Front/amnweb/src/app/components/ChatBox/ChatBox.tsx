"use client";

import { useEffect, useState, useRef} from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import { CopilotKit } from "@copilotkit/react-core";
import "@copilotkit/react-ui/styles.css";
import styles from "./ChatBox.module.css";
// import { InputProps } from "@copilotkit/react-ui";
import { useService } from "@/app/context/ServiceContext";
import { useChat } from "@/app/context/ChatContext";
import { serviceConfig } from "@/app/config/serviceConfig";

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

// CoAgent 상태 타입
interface AgentState {
  routing_vectordb_collection: string;
  retrieved_documents: RetrievedDocument[];
  context: string;
  alert: string;
}

// 전체 상태 타입
interface CoAgentState {
  nodeName: string;
  running: boolean;
  state: AgentState;
}


export default function ChatBox() {
  const currentService = useService().currentService;
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const { isChatActive, chatInput } = useChat();
  const currentConfig = serviceConfig[currentService];
  const [documents, setDocuments] = useState<RetrievedDocument[]>([]);

  const { nodeName, running, state } = useCoAgent<CoAgentState>({
    name: currentConfig.agent || "olaf_ibk_poc_agent",
    initialState: {
      nodeName: "",
      running: false,
      state: {
        routing_vectordb_collection: "",
        retrieved_documents: [],
        context: "",
        alert: "",
      },
    },
  });

  useEffect(() => {
    console.log("[ChatBox] 활성화된 상태로 렌더링됨");
  }, [isChatActive]);

  // 새로운 문서가 도착할 때 상태 업데이트
  useEffect(() => {
    console.log("nodeName, running, state", nodeName, running, state);
    if (nodeName === 'save_chat_history' && state?.retrieved_documents?.length > 0) {
      const extractedDocs = state.retrieved_documents;
      const uniqueDocs = extractedDocs.filter(
        (doc, index, self) =>
          index ===
          self.findIndex((d) => d.kwargs.metadata.file_name === doc.kwargs.metadata.file_name)
      );
      setDocuments(uniqueDocs);

      // 문서가 업데이트되면 즉시 DOM에 추가
      if (chatContainerRef.current) {
        const messages = chatContainerRef.current.querySelectorAll('.copilotKitAssistantMessage');
        const lastMessage = messages[messages.length - 1] as HTMLDivElement;

        if (lastMessage && !lastMessage.dataset.inserted) {
          const docContainer = document.createElement('div');
          docContainer.className = "p-4 mt-2 border-l-4 border-blue-500";
          docContainer.innerHTML = `<h3 class="text-sm font-semibold">📄 관련 문서</h3>`;

          uniqueDocs.forEach(doc => {
            const docElement = document.createElement('div');
            const keywordsHTML = doc.kwargs.metadata.keywords
              .map((keyword) => `#${keyword}`).join(' ');

            docElement.innerHTML = `
              <div class="p-2 border rounded shadow-sm mt-2">
                <a href="${doc.kwargs.metadata.file_url}" target="_blank" class="text-blue-600 hover:underline">
                  ${doc.kwargs.metadata.file_name}
                </a>
                <p class="text-xs">주제: ${doc.kwargs.metadata.main_topic}</p>
                <p class="text-xs">페이지 번호: ${doc.kwargs.metadata.page_number}</p>
                <p class="text-xs text-gray-500">${keywordsHTML}</p>
              </div>
            `;
            docContainer.appendChild(docElement);
          });

          lastMessage.appendChild(docContainer);
          lastMessage.dataset.inserted = "true";
        }
      }
    } else {
      setDocuments([]);
    }
  }, [nodeName, running, state]);

  useEffect(() => {
    if (state?.alert && state.alert !== '') {
      const messages = document.querySelectorAll('.copilotKitAssistantMessage');
      const lastMessage = messages[messages.length - 1] as HTMLDivElement;

      if (lastMessage && !lastMessage.dataset.alertInserted) {
        const alertContainer = document.createElement('div');
        alertContainer.className = "p-4 mt-2 bg-red-100 border-l-4 border-red-500 text-red-700 rounded";
        alertContainer.innerHTML = `
          <div class="flex items-center">
            <svg class="w-5 h-5 mr-2" fill="currentColor" viewBox="0 0 20 20">
              <path fill-rule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7-4a1 1 0 11-2 0 1 1 0 012 0zM9 9a1 1 0 000 2v3a1 1 0 001 1h1a1 1 0 100-2v-3a1 1 0 00-1-1H9z" clip-rule="evenodd"/>
            </svg>
            <p>${state.alert}</p>
          </div>
        `;

        lastMessage.appendChild(alertContainer);
        lastMessage.dataset.alertInserted = "true";
      }
    }
  }, [state?.alert]);

  console.log("[ChatBox] 활성화된 상태로 렌더링됨",currentConfig.apiEndpoint, currentConfig.agent);
  
  return (
    <CopilotKit 
      runtimeUrl={currentConfig.apiEndpoint}
      agent={currentConfig.agent}
    >
      <div ref={chatContainerRef} className={styles.chatWrapper}>
        <div className={styles.container}>
          <CopilotChat
            className={styles.copilotChat}
            Input={(props) => (
              <div className={styles.inputContainer}>
                <input
                  type="text"
                  defaultValue={chatInput || ""} // 초기값 설정
                  placeholder="메시지를 입력하세요"
                  className={styles.input}
                  disabled={props.inProgress}
                  onKeyDown={(e) => {
                    if (e.key === "Enter" && !props.inProgress) {
                      const inputValue = e.currentTarget.value; // 현재 입력값 가져오기
                      props.onSend(inputValue); // 전송
                      e.currentTarget.value = ""; // 전송 후 입력 필드 초기화
                    }
                  }}
                />
                <button
                  className={styles.sendButton}
                  disabled={props.inProgress}
                  onClick={(e) => {
                    const inputElement = e.currentTarget.previousElementSibling as HTMLInputElement;
                    const inputValue = inputElement.value; // 현재 입력값 가져오기
                    props.onSend(inputValue); // 전송
                    inputElement.value = ""; // 전송 후 입력 필드 초기화
                  }}
                >
                  전송
                </button>
              </div>
            )}
            labels={{
              title: currentConfig.title,
              initial: currentConfig.greeting,
            }}
          />
        </div>
      </div>
    </CopilotKit>
  );
}
