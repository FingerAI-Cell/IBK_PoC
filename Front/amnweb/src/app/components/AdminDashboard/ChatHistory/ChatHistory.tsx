"use client";

import { useState } from 'react';
import styles from './ChatHistory.module.css';
import { BiArrowBack } from 'react-icons/bi';

interface MessageData {
  content: string;
  type: string;
  name: null;
  id: string;
  example: boolean;
  additional_kwargs: any;
  response_metadata?: {
    finish_reason: string;
    model_name: string;
    system_fingerprint: string;
  };
}

interface ChatMessage {
  thread_id: string;
  timestamp: string;
  messages: Array<{
    type: string;
    data: MessageData;
  }>;
}

interface ChatHistoryResponse {
  total_count: number;
  chat_history: ChatMessage[];
}

export default function ChatHistory() {
  const [dateFrom, setDateFrom] = useState<string>('');
  const [dateTo, setDateTo] = useState<string>('');
  const [chatHistory, setChatHistory] = useState<ChatMessage[]>([]);
  const [selectedChat, setSelectedChat] = useState<ChatMessage | null>(null);
  const [currentPage, setCurrentPage] = useState(1);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const itemsPerPage = 10;

  const fetchChatHistory = async () => {
    if (!dateFrom || !dateTo) return;
    
    setIsLoading(true);
    setError(null);
    
    try {
      const response = await fetch(
        `/api/chat/history?date_from=${dateFrom.replace(/-/g, '')}&date_to=${dateTo.replace(/-/g, '')}`
      );

      if (!response.ok) {
        throw new Error('데이터 조회에 실패했습니다.');
      }

      const data: ChatHistoryResponse = await response.json();
      setChatHistory(data.chat_history || []);
      setCurrentPage(1);
    } catch (error) {
      console.error('채팅 이력 조회 실패:', error);
      setError('채팅 이력을 불러오는데 실패했습니다.');
      setChatHistory([]);
    } finally {
      setIsLoading(false);
    }
  };

  const formatTimestamp = (timestamp: string) => {
    // "225156" -> "22:51:56" 형식으로 변환
    const hours = timestamp.slice(0, 2).padStart(2, '0');
    const minutes = timestamp.slice(2, 4).padStart(2, '0');
    const seconds = timestamp.slice(4, 6).padStart(2, '0');
    
    const today = new Date();
    const date = new Date(
      today.getFullYear(), 
      today.getMonth(), 
      today.getDate(), 
      parseInt(hours), 
      parseInt(minutes), 
      parseInt(seconds)
    );
    
    return date.toLocaleString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit',
      second: '2-digit',
      hour12: false
    });
  };

  const paginatedHistory = Array.isArray(chatHistory) ? chatHistory.slice(
    (currentPage - 1) * itemsPerPage,
    currentPage * itemsPerPage
  ) : [];

  const totalPages = Math.ceil((Array.isArray(chatHistory) ? chatHistory.length : 0) / itemsPerPage);

  const handleBack = () => {
    setSelectedChat(null);
  };

  const handleDateChange = (type: 'from' | 'to', value: string) => {
    if (type === 'from') {
      const newDateFrom = value;
      // 시작일이 종료일보다 늦으면 값을 서로 교환
      if (dateTo && newDateFrom > dateTo) {
        setDateFrom(dateTo);    // 시작일에 기존 종료일 설정
        setDateTo(newDateFrom); // 종료일에 새로 선택한 날짜 설정
      } else {
        setDateFrom(newDateFrom);
      }
    } else {
      const newDateTo = value;
      // 종료일이 시작일보다 이르면 값을 서로 교환
      if (dateFrom && newDateTo < dateFrom) {
        setDateTo(dateFrom);    // 종료일에 기존 시작일 설정
        setDateFrom(newDateTo); // 시작일에 새로 선택한 날짜 설정
      } else {
        setDateTo(newDateTo);
      }
    }
  };

  if (selectedChat) {
    return (
      <div className={styles.chatDetailContainer}>
        <button onClick={handleBack} className={styles.backButton}>
          <BiArrowBack /> 목록으로 돌아가기
        </button>
        <div className={styles.chatDetail}>
          <h3 className={styles.chatTitle}>{formatTimestamp(selectedChat.timestamp)}</h3>
          <div className={styles.chatContent}>
            {selectedChat.messages.map((message, index) => (
              <div 
                key={index} 
                className={styles.messageItem}
                data-type={message.type}
              >
                <div className={styles.messageType}>
                  {message.type === 'human' ? '사용자' : 'AI'}
                </div>
                <div className={styles.messageContent}>
                  {message.data.content}
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className={styles.container}>
      <div className={styles.filterSection}>
        <input
          type="date"
          value={dateFrom}
          onChange={(e) => handleDateChange('from', e.target.value)}
          className={styles.dateInput}
        />
        <span className={styles.dateSeparator}>~</span>
        <input
          type="date"
          value={dateTo}
          onChange={(e) => handleDateChange('to', e.target.value)}
          className={styles.dateInput}
        />
        <button 
          onClick={fetchChatHistory}
          className={styles.searchButton}
          disabled={!dateFrom || !dateTo || isLoading}
        >
          {isLoading ? '조회 중...' : '조회'}
        </button>
      </div>

      {error && (
        <div className={styles.errorMessage}>
          {error}
        </div>
      )}

      {!error && chatHistory.length === 0 && !isLoading && (
        <div className={styles.emptyMessage}>
          조회된 채팅 이력이 없습니다.
        </div>
      )}

      {isLoading && (
        <div className={styles.loadingMessage}>
          데이터를 불러오는 중입니다...
        </div>
      )}

      {!isLoading && !error && chatHistory.length > 0 && (
        <>
          <div className={styles.historyList}>
            {paginatedHistory.map((chat, index) => (
              <div
                key={index}
                className={styles.historyItem}
                onClick={() => setSelectedChat(chat)}
              >
                <div className={styles.historyTitle}>
                  {formatTimestamp(chat.timestamp)}
                </div>
                <div className={styles.historyMeta}>
                  <span>메시지 수: {chat.messages.length}</span>
                </div>
              </div>
            ))}
          </div>

          {totalPages > 1 && (
            <div className={styles.pagination}>
              <button
                onClick={() => setCurrentPage(prev => Math.max(prev - 1, 1))}
                disabled={currentPage === 1}
                className={styles.pageButton}
              >
                이전
              </button>
              <span className={styles.pageInfo}>
                {currentPage} / {totalPages}
              </span>
              <button
                onClick={() => setCurrentPage(prev => Math.min(prev + 1, totalPages))}
                disabled={currentPage === totalPages}
                className={styles.pageButton}
              >
                다음
              </button>
            </div>
          )}
        </>
      )}
    </div>
  );
} 