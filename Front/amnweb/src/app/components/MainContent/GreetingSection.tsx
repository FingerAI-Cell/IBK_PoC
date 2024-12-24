"use client";

import styles from "./GreetingSection.module.css";
import { serviceConfig } from "../../config/serviceConfig";

interface GreetingSectionProps {
  chatInput: string;
  onInputChange: (value: string) => void;
  onSubmit: () => void;
  serviceType: string;
}

export default function GreetingSection({
  chatInput,
  onInputChange,
  onSubmit,
  serviceType
}: GreetingSectionProps) {
  const config = serviceConfig[serviceType] || serviceConfig["general-chat"];

  return (
    <div className={styles.container}>
      <div className={styles.greeting}>
        <h1>{config.greeting}</h1>
        <p>무엇이든 물어보세요.</p>
      </div>
      <div className={styles.chatBox}>
        <div className={styles.inputContainer}>
          <textarea
            placeholder={config.defaultMessage}
            value={chatInput}
            onChange={(e) => onInputChange(e.target.value)}
            className={styles.input}
          />
          <button onClick={onSubmit} className={styles.sendButton}>
            →
          </button>
        </div>
      </div>
    </div>
  );
}
