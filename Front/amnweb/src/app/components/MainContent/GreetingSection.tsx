"use client";

import styles from "./GreetingSection.module.css";
import { serviceConfig } from "../../config/serviceConfig";
import FAQSection from "./FAQSection";

interface GreetingSectionProps {
  chatInput: string;
  onInputChange: (value: string) => void;
  onSubmit: () => void;
  serviceType: string;
  faqs: string[];
  onFAQClick: (question: string) => void;
}

export default function GreetingSection({
  chatInput,
  onInputChange,
  onSubmit,
  serviceType,
  faqs,
  onFAQClick
}: GreetingSectionProps) {
  const config = serviceConfig[serviceType] || serviceConfig["general-chat"];
  const isFinancialService = serviceType === 'financial-statements';

  return (
    <div className={styles.container}>
      <div className={styles.greeting}>
        <h1>{config.greeting}</h1>
        {/* {isFinancialService ? (
          <p className={styles.updateMessage}>서비스 업데이트 예정입니다.</p>
        ) : ( */}
          <p>무엇이든 물어보세요.</p>
        {/* )} */}
      </div>
        <>
          <div className={styles.chatBox}>
            <div className={styles.inputContainer}>
              <textarea
                placeholder={config.defaultMessage}
                value={chatInput}
                onChange={(e) => onInputChange(e.target.value)}
                className={styles.input}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault();
                    onSubmit();
                  }
                }}
              />
              <button onClick={onSubmit} className={styles.sendButton}>
                →
              </button>
            </div>
          </div>
          <FAQSection faqs={faqs} onFAQClick={onFAQClick} />
        </>
    </div>
  );
}
