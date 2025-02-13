"use client";

import styles from "./FAQSection.module.css";

interface FAQSectionProps {
  faqs: string[];
  onFAQClick: (question: string) => void;
}

export default function FAQSection({ faqs, onFAQClick }: FAQSectionProps) {
  // 최대 2개의 FAQ만 표시하고, 각 FAQ 텍스트도 필요하다면 줄임
  const displayFaqs = faqs.slice(0, 2).map(faq => 
    faq.length > 25 ? faq.substring(0, 25) + '...' : faq
  );
  
  return (
    <div className={styles.faqSection}>
      {displayFaqs.map((question, index) => (
        <button
          key={index}
          onClick={() => onFAQClick(question)}
          className={styles.faqItem}
          title={question}  /* 전체 텍스트는 hover시 툴팁으로 표시 */
        >
          {question}
        </button>
      ))}
    </div>
  );
}
