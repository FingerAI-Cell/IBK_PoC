"use client";

import { useChat } from "../../hooks/useChat";
import styles from "./ChatBox.module.css";
import { useEffect, memo } from "react";

interface ChatBoxProps {
  sendApiRequest: (message: string) => Promise<string>;
  initialInput: string;
  serviceName: string;
  showReset?: boolean;
}

// 메시지 컴포넌트 분리
const ChatMessage = memo(({ sender, text }: { sender: string; text: string }) => (
  <div
    className={`${styles.message} ${
      sender === "user" ? styles.userMessage : styles.botMessage
    }`}
  >
    {text.split('\n').map((line, i) => (
      <span key={i}>
        {line}
        {i !== text.split('\n').length - 1 && <br />}
      </span>
    ))}
  </div>
));

ChatMessage.displayName = 'ChatMessage';

export default function ChatBox({ sendApiRequest, initialInput, serviceName }: ChatBoxProps) {
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
            {msg.text.split('\n').map((line, i) => (
              <span key={i}>
                {line}
                {i !== msg.text.split('\n').length - 1 && <br />}
              </span>
            ))}
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
          <textarea
            value={input}
            onChange={(e) => {
              setInput(e.target.value);
              // 동적 높이 조절
              e.target.style.height = 'auto';
              e.target.style.height = e.target.scrollHeight + 'px';
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter" && !e.shiftKey) {
                e.preventDefault();
                sendMessage();
              }
            }}
            ref={inputRef}
            className={styles.input}
            placeholder="메시지를 입력하세요..."
            rows={1}
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
      </div>
    </div>
  );
}
