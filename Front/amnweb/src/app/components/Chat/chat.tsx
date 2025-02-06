"use client";

import { useEffect, useState, useRef} from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
import styles from "../ChatBox/ChatBox.module.css";
// import { InputProps } from "@copilotkit/react-ui";
import { useService } from "@/app/context/ServiceContext";
import { useChat } from "@/app/context/ChatContext";
import { serviceConfig } from "@/app/config/serviceConfig";
import DocumentList from "@/app/chatbot/documentList";
import { DocumentCard } from "@/app/chatbot/DocumentCard";
import { createRoot } from "react-dom/client";

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

const DocumentSection = ({ documents }: { documents: RetrievedDocument[] }) => {
  return (
    <div className="p-4 mt-2 border-l-4 border-blue-500">
      <h3 className="text-sm font-semibold">📄 관련 문서</h3>
      <DocumentList data={documents} />
    </div>
  );
};

export default function Chat() {
  const currentService = useService().currentService;
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const chatInputRef = useRef<HTMLInputElement>(null); // 입력 필드 참조
  const { isChatActive, chatInput } = useChat();
  const currentConfig = serviceConfig[currentService];
  const [documents, setDocuments] = useState<RetrievedDocument[]>([]);
  const [initialMessageSent, setInitialMessageSent] = useState(false);

  const { nodeName, running, state } = useCoAgent<CoAgentState>({
    name: currentConfig.agent || "olaf_ibk_poc_agent",
    initialState: {
      nodeName: '',
      running: false,
      state: {
        routing_vectordb_collection: '',
        retrieved_documents: [],
        context: '',
        alert: '',
      },
    },
  });

  // 커스텀 이벤트 핸들러를 컴포넌트 레벨로 이동
  const handleCustomSend = (onSend: (message: string) => void) => (e: CustomEvent<{ message: string }>) => {
    onSend(e.detail.message);
  };

  useEffect(() => {
    console.log("[ChatBox0] 활성화된 상태로 렌더링됨",isChatActive);
    chatInputRef.current.focus();
    
    // chatInput이 있고 아직 초기 메시지를 보내지 않았을 때만 실행
    if (chatInput && !initialMessageSent) {
      const customEvent = new CustomEvent('copilotSendMessage', {
        detail: { message: chatInput }
      });
      document.dispatchEvent(customEvent);
      setInitialMessageSent(true);
    }
  }, [isChatActive]);

  // 새로운 문서가 도착할 때 상태 업데이트
  useEffect(() => {
    if (
      nodeName === "save_chat_history" &&
      running &&
      state?.retrieved_documents?.length > 0 &&
      documents.length !== state.retrieved_documents.length
    ) {
      const uniqueDocs = state.retrieved_documents.filter(
        (doc, index, self) =>
          index === self.findIndex((d) =>
            (d.kwargs?.metadata?.file_name || '') ===
            (doc.kwargs?.metadata?.file_name || '')
          )
      );
      setDocuments(uniqueDocs);
  
      console.log("[문서 업데이트] nodeName, running, state", nodeName, running, state);
    }
  }, [nodeName, running, state?.retrieved_documents]);
  
  useEffect(() => {
    if (chatContainerRef.current && documents.length > 0) {
      const messages = chatContainerRef.current.querySelectorAll('.copilotKitAssistantMessage');
      const lastMessage = messages[messages.length - 1] as HTMLDivElement;
  
      if (lastMessage && !lastMessage.dataset.inserted) {
        const docContainer = document.createElement('div');
        docContainer.id = 'document-section-container';
        lastMessage.appendChild(docContainer);
        
        // React 컴포넌트로 렌더링
        createRoot(docContainer).render(
          <DocumentSection documents={documents} />
        );
        
        lastMessage.dataset.inserted = "true";
      }
    }
  }, [documents]);
  

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

  console.log("[ChatBox] 활성화된 상태로 렌더링됨",isChatActive, currentConfig.apiEndpoint, currentConfig.agent);
  
  // Input 컴포넌트를 별도로 분리
  const CustomInput = (props: any) => {
    useEffect(() => {
      const handler = handleCustomSend(props.onSend);
      document.addEventListener('copilotSendMessage', handler as EventListener);
      return () => {
        document.removeEventListener('copilotSendMessage', handler as EventListener);
      };
    }, [props.onSend]);

    return (
      <div className={styles.inputContainer}>
        <input
          autoFocus
          ref={chatInputRef}
          type="textarea"
          placeholder="메시지를 입력하세요"
          className={styles.input}
          disabled={props.inProgress}
          onKeyDown={(e) => {
            if (e.key === "Enter" && !e.shiftKey && !props.inProgress) {
              e.preventDefault();
              const inputValue = e.currentTarget.value;
              props.onSend(inputValue);
              e.currentTarget.value = "";
            }
          }}
        />
        <button
          className={styles.sendButton}
          disabled={props.inProgress}
          onClick={(e) => {
            const inputElement = e.currentTarget.previousElementSibling as HTMLInputElement;
            const inputValue = inputElement.value;
            props.onSend(inputValue);
            inputElement.value = "";
          }}
        >
          전송
        </button>
      </div>
    );
  };

  return (
      <div ref={chatContainerRef} className={styles.chatWrapper}>
        <div className={styles.container}>
          <CopilotChat
            className={styles.copilotChat}
            Input={CustomInput}
            labels={{
              title: currentConfig.title,
              initial: currentConfig.greeting,
            }}
          />
        </div>
      </div>
  );
}
