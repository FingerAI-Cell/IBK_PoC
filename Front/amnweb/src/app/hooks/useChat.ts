import { useState, useRef, useEffect } from "react";

interface Message {
  id: string;
  sender: 'user' | 'bot';
  text: string;
  timestamp: Date;
}

export function useChat(
  
  sendApiRequest: (message: string) => Promise<string>,
  historyKey: string
) {
  const [messages, setMessages] = useState<Message[]>([]);
  const [input, setInput] = useState("");
  const [isSending, setIsSending] = useState(false);
  const [controller, setController] = useState<AbortController | null>(null);
  const messageEndRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLTextAreaElement | null>(null);
  const [error, setError] = useState<string | null>(null);

  
  
  const scrollToBottom = () => {
    if (messageEndRef.current) {
      messageEndRef.current.scrollIntoView({ behavior: "smooth" });
    }
  };

  console.log("useChat initialized with historyKey:", historyKey);
  console.log("Message sent to API:", input);


  // 메시지가 변경될 때 자동으로 하단으로 스크롤
  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // 응답이 완료되면 입력창에 포커스 설정
  useEffect(() => {
    if (!isSending && inputRef.current) {
      inputRef.current.focus();
    }
  }, [isSending]);

  const addMessage = (sender: 'user' | 'bot', text: string) => {
    const newMessage = {
      id: Date.now().toString(),
      sender,
      text,
      timestamp: new Date()
    };
    
    setMessages(prev => {
      const newMessages = [...prev, newMessage];
      
      // 히스토리 저장 (사용자 메시지만)
      if (sender === 'user') {
        const historyItem = {
          id: Date.now(),
          text,
          time: new Date().toLocaleString()
        };
        
        const savedHistory = localStorage.getItem(historyKey);
        const history = savedHistory ? JSON.parse(savedHistory) : [];
        history.unshift(historyItem);
        localStorage.setItem(historyKey, JSON.stringify(history.slice(0, 5))); // 최근 5개만 저장
      }
      
      return newMessages;
    });
  };

  const handleError = (err: Error) => {
    setError(err.message);
    addMessage('bot', "안녕하세요, 업무 챗봇입니다. \n무엇을 도와드릴까요?"); //오류가 발생했습니다: ${err.message}
  };

  const sendMessage = async () => {
    if (!input.trim() || isSending) return;
    console.log("Sending API request with message:", input); // API 호출 전 로그
    // 사용자 메시지 추가
    addMessage('user', input);
    setInput("");

    if (controller) {
      controller.abort(); // 이전 요청 중단
    }

    const newController = new AbortController();
    setController(newController);
    setIsSending(true);

    try {
      // 부모에서 전달된 API 호출
      console.log("Sending API request with message:", input); // API 호출 전 로그

      const reply = await sendApiRequest(input);

      console.log("API reply received:", reply); // API 응답 로그
      
      addMessage('bot', reply || "응답 없음");
    } catch (err) {
      if (err instanceof Error) {
        if (err.name === "AbortError") {
          console.log("요청이 취소되었습니다."); // 요청 취소 로그
          return; // 요청 취소 시 아무것도 하지 않음
        }
        console.error("에러 발생:", err.message);
        handleError(err);
      }
    } finally {
      setIsSending(false);
    }
  };

  const cancelRequest = () => {
    if (controller) {
      controller.abort(); // 요청 취소
    }
    setController(null); // 컨트롤러 초기화
    setIsSending(false); // 입력창 잠금 해제
  };

  const clearError = () => {
    setError(null);
  };

  useEffect(() => {
    if (error) {
      // 에러가 있으면 3초 후 자동으로 제거
      const timer = setTimeout(clearError, 3000);
      return () => clearTimeout(timer);
    }
  }, [error]);

  return {
    messages,
    input,
    isSending,
    messageEndRef,
    inputRef,
    setInput,
    sendMessage,
    cancelRequest,
    error,
    clearError,
  };
}
