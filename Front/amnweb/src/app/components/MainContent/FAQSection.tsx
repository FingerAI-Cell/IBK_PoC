"use client";

import styles from "./FAQSection.module.css";

interface FAQSectionProps {
  faqs: string[];
  onFAQClick: (question: string) => void;
}

export default function FAQSection({ faqs, onFAQClick }: FAQSectionProps) {
  // 최대 2개의 FAQ만 표시하되, 텍스트는 전체 표시
  const displayFaqs = faqs.slice(0, 2);
  
  return (
    <div className={styles.faqSection}>
      {displayFaqs.map((question, index) => (
        <button
          key={index}
          onClick={() => onFAQClick(question)}
          className={styles.faqItem}
        >
          {question}
        </button>
      ))}
    </div>
  );
}
