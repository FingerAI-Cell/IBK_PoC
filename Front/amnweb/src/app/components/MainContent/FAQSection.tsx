"use client";

import styles from "./FAQSection.module.css";

interface FAQSectionProps {
  faqs: string[];
  onFAQClick: (question: string) => void;
}

export default function FAQSection({ faqs, onFAQClick }: FAQSectionProps) {
  return (
    <div className={styles.container}>
      <h2>자주 묻는 질문</h2>
      <div className={styles.faqList}>
        {faqs.map((question, index) => (
          <button
            key={index}
            onClick={() => onFAQClick(question)}
            className={styles.faqItem}
          >
            {question}
          </button>
        ))}
      </div>
    </div>
  );
}
