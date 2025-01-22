'use client';

import { useCoAgent } from "@copilotkit/react-core";
import { CopilotChat } from "@copilotkit/react-ui";
import { useEffect, useState, useRef } from "react";

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

// 채팅 컴포넌트
export function Chat({ agent }: { agent: string }) {
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
        alert: '',
      },
    },
  });

  const [documents, setDocuments] = useState<any[]>([]);
  const chatContainerRef = useRef<HTMLDivElement>(null);

  // 새로운 문서가 도착할 때 상태 업데이트
  useEffect(() => {
    // console.log("nodeName, running, state", nodeName, running, state);
    if (nodeName === 'save_chat_history' && state?.retrieved_documents?.length > 0) {
      const extractedDocs = state.retrieved_documents;
      const uniqueDocs = extractedDocs.filter(
        (doc, index, self) =>
          index === self.findIndex((d) => 
            (d.metadata?.file_name || d.kwargs?.metadata?.file_name) === 
            (doc.metadata?.file_name || doc.kwargs?.metadata?.file_name)
          )
      );
      setDocuments(uniqueDocs);

      console.log("[문서 업데이트] nodeName, running, state", nodeName, running, state);
      
      // 문서가 업데이트되면 즉시 DOM에 추가
      if (chatContainerRef.current) {
        const messages = chatContainerRef.current.querySelectorAll('.copilotKitAssistantMessage');
        const lastMessage = messages[messages.length - 1] as HTMLDivElement;
        
        if (lastMessage && !lastMessage.dataset.inserted) {
          let isInserted = false;
          const docContainer = document.createElement('div');
          docContainer.className = "p-4 mt-2 border-l-4 border-blue-500";
          docContainer.innerHTML = `<h3 class="text-sm font-semibold">📄 관련 문서</h3>`;
          
          state.retrieved_documents[0].forEach((doc) => {
            if (!doc.kwargs?.metadata) return; // 메타데이터가 없으면 건너뜀
            
            console.log("Document metadata", doc.kwargs.metadata);

            const docElement = document.createElement('div');
            const getKeywords = (doc: any) => {
              const keywords = doc.kwargs?.metadata?.keywords || doc.metadata?.keywords;
              return Array.isArray(keywords) ? keywords : [];
            };
            
            const keywordsHTML = getKeywords(doc)
              .map((keyword) => `#${keyword}`)
              .join(' ');
            
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
            
            if (!isInserted) {
              isInserted = true;
            }
          });

          if (isInserted) {
            lastMessage.appendChild(docContainer);
            lastMessage.dataset.inserted = "true";
          }
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