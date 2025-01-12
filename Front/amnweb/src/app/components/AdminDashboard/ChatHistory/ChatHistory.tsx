"use client";

import { useState } from 'react';
import styles from './ChatHistory.module.css';
import { BiArrowBack } from 'react-icons/bi';

interface ChatMessage {
  timestamp: string;
  messages: Array<{
    type: string;
    data: {
      content: string;
    };
  }>;
}

const MOCK_CHAT_HISTORY: ChatMessage[] = [
  {
    timestamp: "2024-03-19T05:30:00.000Z",
    messages: [
      {
        type: "user",
        data: {
          content: "애플 주가가 어떻게 되나요?"
        }
      },
      {
        type: "assistant",
        data: {
          content: "현재 애플(AAPL)의 주가는 $173.72입니다. 전일 대비 0.54% 상승했습니다."
        }
      }
    ]
  },
  {
    timestamp: "2024-03-19T04:15:00.000Z",
    messages: [
      {
        type: "user",
        data: {
          content: "테슬라의 최근 실적은 어떤가요?"
        }
      },
      {
        type: "assistant",
        data: {
          content: "테슬라의 2024년 1분기 실적은 다음과 같습니다: 매출액 $21.3B, 영업이익 $1.8B를 기록했습니다."
        }
      },
      {
        type: "user",
        data: {
          content: "테슬라의 주가 전망은 어떤가요?"
        }
      },
      {
        type: "assistant",
        data: {
          content: "현재 월가의 테슬라 주가 전망은 다소 엇갈리고 있습니다. 일부 애널리스트들은 전기차 시장 경쟁 심화로 인한 우려를 제기하고 있으나, 자율주행 기술 발전과 에너지 사업 확장 가능성에 주목하는 의견도 있습니다."
        }
      }
    ]
  },
  {
    timestamp: "2024-03-18T09:20:00.000Z",
    messages: [
      {
        type: "user",
        data: {
          content: "마이크로소프트의 AI 투자 현황이 궁금합니다."
        }
      },
      {
        type: "assistant",
        data: {
          content: "마이크로소프트는 OpenAI에 대한 투자를 지속적으로 확대하고 있으며, Copilot 등 AI 서비스를 전사적으로 확장하고 있습니다."
        }
      }
    ]
  },
  {
    timestamp: "2024-03-18T07:45:00.000Z",
    messages: [
      {
        type: "user",
        data: {
          content: "엔비디아 주식을 매수하려고 하는데 어떻게 생각하시나요?"
        }
      },
      {
        type: "assistant",
        data: {
          content: "엔비디아는 AI 반도체 시장에서 독보적인 위치를 차지하고 있으며, 데이터센터 부문의 성장세가 지속되고 있습니다. 다만, 현재 주가가 고평가 되어있다는 의견도 있어 신중한 접근이 필요합니다."
        }
      }
    ]
  },
  {
    timestamp: "2024-03-17T10:30:00.000Z",
    messages: [
      {
        type: "user",
        data: {
          content: "메타의 최근 동향은 어떤가요?"
        }
      },
      {
        type: "assistant",
        data: {
          content: "메타는 메타버스 투자를 지속하면서도 AI 기술 개발에도 주력하고 있습니다. 최근에는 광고 매출이 회복세를 보이고 있으며, Reality Labs 부문의 손실도 점차 감소하는 추세입니다."
        }
      }
    ]
  }
];

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
      // const response = await fetch(
      //   `/api/chat/history?date_from=${dateFrom.replace(/-/g, '')}&date_to=${dateTo.replace(/-/g, '')}`
      // );

      // if (!response.ok) {
      //   throw new Error('데이터 조회에 실패했습니다.');
      // }

      // const data = await response.json();
      
      // // 응답이 배열이 아닌 경우 빈 배열로 처리
      // setChatHistory(Array.isArray(data) ? data : []);
      setChatHistory(MOCK_CHAT_HISTORY);
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
    return new Date(timestamp).toLocaleString('ko-KR');
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
                <div className={styles.messageType}>{message.type}</div>
                <div className={styles.messageContent}>{message.data.content}</div>
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