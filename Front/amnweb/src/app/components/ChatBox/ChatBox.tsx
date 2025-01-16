"use client";

import { useEffect, useState, useRef, useCallback } from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
import styles from "./ChatBox.module.css";
import { InputProps} from "@copilotkit/react-ui";
import { serviceConfig } from "@/app/config/serviceConfig";

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

// InputProps 타입 확장
interface CustomInputProps extends InputProps {
  initialInput?: string;
}

function CustomInput({ inProgress, onSend, initialInput }: CustomInputProps) {
  const [value, setValue] = useState("");
  const isFirstRender = useRef(true);

  // 초기 메시지 자동 전송
  useEffect(() => {
    if (isFirstRender.current && initialInput) {
      console.log('Auto sending initial message:', initialInput);
      onSend(initialInput);
      isFirstRender.current = false;
    }
  }, []);

  const handleSubmit = () => {
    if (value.trim()) {
      onSend(value);
      setValue(""); // 입력 필드 초기화
    }
  };

  return (  
    <div className="flex gap-2 p-4 border-t">
      <input 
        disabled={inProgress}
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="메시지를 입력하세요..."
        className="flex-1 p-2 rounded-md border border-gray-300 focus:outline-none focus:border-blue-500 disabled:bg-gray-100"
        onKeyDown={(e) => {
          if (e.key === 'Enter') {
            handleSubmit();
          }
        }}
      />
      <button 
        disabled={inProgress}
        className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed"
        onClick={() => {
          handleSubmit();
        }}
      >
        전송
      </button>
    </div>
  );
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



export default function ChatBox({
  initialInput = "",
  agent,
  serviceName,
  useCopilot = false,
}: ChatBoxProps) {
  const {
    nodeName,
    running,
    state
  } = useCoAgent<CoAgentState>({
    name: agent ? agent : "olaf_ibk_poc_agent",
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
  const [documents, setDocuments] = useState<any[]>([]);
  const chatContainerRef = useRef<HTMLDivElement>(null);


  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const chatRef = useRef<any>(null);

  // 에이전트 상태 변경 시 디버깅
  useEffect(() => {
    console.log("Agent state updated:", state);
  }, [state]);

  // 실제 메시지 전송을 처리하는 함수
  const handleSubmitMessage = useCallback(async (message: string) => {
    console.log('handleSubmitMessage called with:', message);
    
    if (!chatRef.current || !isLoaded) {
      console.log('Chat not ready:', { 
        chatRef: !!chatRef.current, 
        isLoaded,
        element: chatRef.current
      });
      return;
    }
    
    const inputElement = chatRef.current.querySelector('[role="textbox"], textarea, input');
    console.log('Found input element:', {
      element: inputElement,
      type: inputElement?.tagName,
      role: inputElement?.getAttribute('role')
    });
    
    if (inputElement) {
      try {
        // contentEditable div인 경우
        if (inputElement.getAttribute('role') === 'textbox') {
          inputElement.textContent = message;
        } else {
          // textarea나 input인 경우
          inputElement.value = message;
        }
        
        // input 이벤트 발생 전 로그
        console.log('Before input event dispatch');
        inputElement.dispatchEvent(new Event('input', { bubbles: true }));
        console.log('After input event dispatch');

        // 약간의 지연 후 Enter 이벤트 발생
        setTimeout(() => {
          console.log('Dispatching Enter event');
          const enterEvent = new KeyboardEvent('keydown', {
            key: 'Enter',
            code: 'Enter',
            keyCode: 13,
            which: 13,
            bubbles: true,
            cancelable: true
          });
          
          inputElement.dispatchEvent(enterEvent);
          console.log('Enter event dispatched');
        }, 100);

      } catch (error) {
        console.error('Error in handleSubmitMessage:', error);
      }
    }
  }, [isLoaded]);

  // initialInput 처리 - handleSubmitMessage 사용
  useEffect(() => {
    if (initialInput?.trim() && isLoaded && !isInitialized) {
      console.log('Sending initial message via handleSubmitMessage');
      handleSubmitMessage(initialInput);
      setIsInitialized(true);
    }
  }, [initialInput, isLoaded, isInitialized, handleSubmitMessage]);

  // 서비스 로깅 (히스토리 추적용)
  useEffect(() => {
    console.log(`현재 서비스: ${serviceName}`);
  }, [serviceName]);

   // 새로운 문서가 도착할 때 상태 업데이트
  useEffect(() => {
  console.log("nodeName, running, state", nodeName, running, state);

  if (nodeName === "save_chat_history" && state?.retrieved_documents?.length > 0) {
    const extractedDocs = state.retrieved_documents;
    const uniqueDocs = extractedDocs.filter(
      (doc, index, self) =>
        index ===
        self.findIndex((d) => d.kwargs.metadata.file_name === doc.kwargs.metadata.file_name)
    );
    setDocuments(uniqueDocs);

    // DOM 조작 수행
    if (chatContainerRef.current) {
      const messages = chatContainerRef.current.querySelectorAll(".copilotKitAssistantMessage");
      const lastMessage = messages[messages.length - 1] as HTMLDivElement;

      if (lastMessage && !lastMessage.dataset.inserted) {
        const docContainer = document.createElement("div");
        docContainer.className = "p-4 mt-2 border-l-4 border-blue-500";
        docContainer.innerHTML = `<h3 class="text-sm font-semibold">📄 관련 문서</h3>`;

        uniqueDocs.forEach((doc) => {
          const docElement = document.createElement("div");
          const keywordsHTML = doc.kwargs.metadata.keywords
            .map((keyword) => `#${keyword}`)
            .join(" ");
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
      // 추가된 DOM의 이벤트 전파 차단
      docContainer.addEventListener("click", (e) => {
        e.stopPropagation();
      });
        lastMessage.appendChild(docContainer);
        lastMessage.dataset.inserted = "true"; // 중복 삽입 방지
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

  // DOM 변경 시 스크롤 유지
  useEffect(() => {
    messageEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, []);

  return (
    <div ref={chatContainerRef} className={styles.container} suppressHydrationWarning>
      {useCopilot && agent && (
        <div className={styles.chatWrapper}>
          <CopilotChat
            className={styles.copilotChat}
            Input={(props) => <CustomInput {...props} initialInput={initialInput} />}
            labels={{
              title: "IBK 투자증권 업무 효율화 챗봇",
              initial: "안녕하세요. IBK 투자증권 업무 효율화 챗봇입니다. 무엇을 도와드릴까요?",
              placeholder: "메시지를 입력하세요...",
            }}
          />
        </div>
      )}
    </div>
  );
}