"use client";

import styles from "./GreetingSection.module.css";

interface GreetingSectionProps {
  chatInput: string;
  onInputChange: (value: string) => void;
  onSubmit: () => void;
  placeholder: string;
}

export default function GreetingSection({
  chatInput,
  onInputChange,
  onSubmit,
  placeholder
}: GreetingSectionProps) {
  return (
    <div className={styles.container}>
      <div className={styles.greeting}>
        <h1>안녕하세요!</h1>
        <p>무엇을 도와드릴까요?</p>
      </div>
      <div className={styles.inputContainer}>
        <input
          type="text"
          placeholder={placeholder}
          value={chatInput}
          onChange={(e) => onInputChange(e.target.value)}
          className={styles.input}
        />
        <button onClick={onSubmit} className={styles.sendButton}>
          ➤
        </button>
      </div>
    </div>
  );
}
