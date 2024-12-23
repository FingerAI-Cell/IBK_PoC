"use client";

import { useChat } from "../../hooks/useChat";
import styles from "./ChatBox.module.css";
import { useEffect, memo } from "react"; // 추가: useEffect 임포트

interface ChatBoxProps {
  sendApiRequest: (message: string) => Promise<string>;
  initialInput: string;
  onReset: () => void;
  serviceName: string;
  showReset?: boolean; // 돌아가기 버튼 표시 여부
}

// 메시지 컴포넌트 분리
const ChatMessage = memo(({ sender, text }: { sender: string; text: string }) => (
  <div
    className={`${styles.message} ${
      sender === "user" ? styles.userMessage : styles.botMessage
    }`}
  >
    {text}
  </div>
));

ChatMessage.displayName = 'ChatMessage';

export default function ChatBox({ sendApiRequest, initialInput, onReset, serviceName, showReset = true }: ChatBoxProps) {
  const {
    messages,
    input,
    isSending,
    messageEndRef,
    inputRef,
    setInput,
    sendMessage,
    cancelRequest,
  } = useChat(sendApiRequest, `chat-history-${serviceName}`);

  // 초기 입력값 설정
  useEffect(() => {
    if (initialInput.trim()) {
      setInput(initialInput);
    }
  }, [initialInput, setInput]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === "Enter") {
      e.preventDefault();
      sendMessage();
    }
  };

  return (
    <div className={styles.container}>
      <div className={styles.messageList}>
        {messages.map((msg, idx) => (
          <div
            key={idx}
            className={`${styles.message} ${
              msg.sender === "user" ? styles.userMessage : styles.botMessage
            }`}
          >
            {msg.text}
          </div>
        ))}
        <div ref={messageEndRef} />
      </div>
      <div className={styles.inputContainer}>
        {isSending ? (
          <input
            type="text"
            value="응답 중..."
            disabled
            className={`${styles.input} ${styles.disabledInput}`}
          />
        ) : (
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            ref={inputRef}
            className={styles.input}
            placeholder="메시지를 입력하세요..."
          />
        )}
        {isSending ? (
          <button onClick={cancelRequest} className={`${styles.sendButton} ${styles.cancelButton}`}>
            취소
          </button>
        ) : (
          <button onClick={sendMessage} className={styles.sendButton} disabled={!input.trim()}>
            전송
          </button>
        )}
        {showReset && ( // 조건부 렌더링
          <button onClick={onReset} className={styles.cancelButton}>
            돌아가기
          </button>
        )}
      </div>
    </div>
  );
}
