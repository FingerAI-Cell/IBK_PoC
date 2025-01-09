"use client";

import { useState } from 'react';
import styles from './ChatHistory.module.css';
import { BiArrowBack } from 'react-icons/bi';

interface ChatHistoryItem {
  id: string;
  category: string;
  title: string;
  date: string;
  content?: string;
}

export default function ChatHistory() {
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [selectedChat, setSelectedChat] = useState<ChatHistoryItem | null>(null);

  // 샘플 데이터
  const categories = [
    { id: 'all', name: '전체' },
    { id: 'general-chat', name: '일반 채팅' },
    { id: 'overseas-loan', name: '해외주식' },
    { id: 'financial-statements', name: '재무제표' },
    { id: 'branch-manual', name: '영업점 매뉴얼' },
  ];

  const chatHistory: ChatHistoryItem[] = [
    {
      id: '1',
      category: 'general-chat',
      title: '신규 서비스 문의',
      date: '2024-03-19 14:30',
      content: '신규 서비스 관련 상담 내용...'
    },
    {
      id: '2',
      category: 'overseas-loan',
      title: '해외주식 담보대출 한도 문의',
      date: '2024-03-18 11:20',
      content: '해외주식 담보대출 관련 상담 내용...'
    },
    // ... 더 많은 샘플 데이터
  ];

  const filteredHistory = selectedCategory === 'all' 
    ? chatHistory 
    : chatHistory.filter(chat => chat.category === selectedCategory);

  const handleBack = () => {
    setSelectedChat(null);
  };

  if (selectedChat) {
    return (
      <div className={styles.chatDetailContainer}>
        <button onClick={handleBack} className={styles.backButton}>
          <BiArrowBack /> 목록으로 돌아가기
        </button>
        <div className={styles.chatDetail}>
          <h3 className={styles.chatTitle}>{selectedChat.title}</h3>
          <div className={styles.chatMeta}>
            <span>{categories.find(c => c.id === selectedChat.category)?.name}</span>
            <span>{selectedChat.date}</span>
          </div>
          <div className={styles.chatContent}>
            {selectedChat.content}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.categoryFilter}>
        {categories.map((category) => (
          <button
            key={category.id}
            className={`${styles.categoryButton} ${
              selectedCategory === category.id ? styles.active : ''
            }`}
            onClick={() => setSelectedCategory(category.id)}
          >
            {category.name}
          </button>
        ))}
      </div>

      <div className={styles.historyList}>
        {filteredHistory.map((chat) => (
          <div
            key={chat.id}
            className={styles.historyItem}
            onClick={() => setSelectedChat(chat)}
          >
            <div className={styles.historyTitle}>{chat.title}</div>
            <div className={styles.historyMeta}>
              <span>{categories.find(c => c.id === chat.category)?.name}</span>
              <span>{chat.date}</span>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
} 