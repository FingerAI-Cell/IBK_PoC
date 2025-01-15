"use client";

import { useEffect, memo, useState, useRef, useCallback } from "react";
import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import "@copilotkit/react-ui/styles.css";
import styles from "./ChatBox.module.css";
import { InputProps} from "@copilotkit/react-ui";

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

interface CoAgentState {
  routing_vectordb_collection: string;
  retrieved_documents: RetrievedDocument[];
  context: string;
}

// // 메시지 컴포넌트 (메모이제이션으로 렌더링 최적화)
// const ChatMessage = memo(({ sender, text }: { sender: string; text: string }) => (
//   <div
//     className={`${styles.message} ${
//       sender === "user" ? styles.userMessage : styles.botMessage
//     }`}
//   >
//     {text.split("\n").map((line, i) => (
//       <span key={i}>
//         {line}
//         {i !== text.split("\n").length - 1 && <br />}
//       </span>
//     ))}
//   </div>
// ));
// ChatMessage.displayName = "ChatMessage";

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

  const handleSubmit = (value: string) => {
    if (value.trim()) onSend(value);
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
            handleSubmit(e.currentTarget.value);
            setValue("");
          }
        }}
      />
      <button 
        disabled={inProgress}
        className="px-4 py-2 bg-blue-500 text-white rounded-md hover:bg-blue-600 disabled:bg-gray-400 disabled:cursor-not-allowed"
        onClick={() => {
          handleSubmit(value);
          setValue("");
        }}
      >
        전송
      </button>
    </div>
  );
}

export default function ChatBox({
  initialInput = "",
  agent,
  serviceName,
  useCopilot = false,
}: ChatBoxProps) {
  const [isInitialized, setIsInitialized] = useState(false);
  const [isLoaded, setIsLoaded] = useState(false);
  const messageEndRef = useRef<HTMLDivElement>(null);
  const chatContainerRef = useRef<HTMLDivElement>(null);
  const chatRef = useRef<any>(null);

  // 디버깅을 위한 로그 추가
  useEffect(() => {
    console.log('ChatBox Props:', {
      initialInput,
      agent,
      serviceName,
      useCopilot
    });
  }, [initialInput, agent, serviceName, useCopilot]);

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

  // CopilotChat 로드 완료 체크
  useEffect(() => {
    const checkLoaded = setInterval(() => {
      const inputElement = chatRef.current?.querySelector('[role="textbox"], textarea, input');
      if (inputElement) {
        console.log('Chat loaded with element:', inputElement.tagName);
        setIsLoaded(true);
        clearInterval(checkLoaded);
      }
    }, 100);

    return () => clearInterval(checkLoaded);
  }, []);

  // 서비스 로깅 (히스토리 추적용)
  useEffect(() => {
    console.log(`현재 서비스: ${serviceName}`);
  }, [serviceName]);

  // 문서 관련 상태 (from useCoAgent)
  const { nodeName, running, state } = useCoAgent<CoAgentState>({
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
  }, [nodeName, running, state]);

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