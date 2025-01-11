"use client";

import { useState, useEffect } from 'react';
import styles from './AdminDashboard.module.css';
import ChatHistory from './ChatHistory/ChatHistory';

interface FilingData {
  overview: Array<Record<string, {
    '10-K': string;
    '10-Q': string;
    '8-K': string;
    news: string;
  }>>;
  summary: Array<Record<string, Array<{
    form: string;
    date: string;
    matching_sections: {
      [key: string]: {
        keywords_found: Record<string, string>;
        text_ko_summary: string;
      };
    };
  }>>>;
}

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'financial' | 'monitoring' | 'chat-history'>('financial');
  const [selectedStock, setSelectedStock] = useState<string | null>(null);
  const [selectedKeyword, setSelectedKeyword] = useState<string | null>(null);
  const [filingData, setFilingData] = useState<FilingData | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0].replace(/-/g, '')
  );

  useEffect(() => {
    const fetchFilingData = async () => {
      try {
        const response = await fetch(`/api/admin/filing/${selectedDate}`);
        
        if (!response.ok) {
          throw new Error(`HTTP 상태 오류: ${response.status}`);
        }
        const data = await response.json();
        setFilingData(data);
      } catch (error) {
        console.error('데이터 불러오기 실패:', error);
      }
    };

    fetchFilingData();
  }, [selectedDate]);

  // 요약 텍스트 추출 함수
  const getSummaryTexts = () => {
    if (!filingData?.summary?.[0]) return [];
    
    return Object.values(filingData.summary[0]).flatMap(company => 
      company.map(item => 
        Object.values(item.matching_sections)
          .map(section => section.text_ko_summary)
      ).flat()
    ).filter(text => text);
  };

  // 재무제표 샘플 데이터
  const financialStatements = [
    { id: 1, fileName: '2024년 1분기 재무제표.xlsx', author: '김재무', createdAt: '2024-03-15 14:30' },
    { id: 2, fileName: '2023년 4분기 재무제표.xlsx', author: '이경제', createdAt: '2024-01-10 09:15' },
    { id: 3, fileName: '2023년 3분기 재무제표.xlsx', author: '박회계', createdAt: '2023-10-05 11:45' },
  ];

  const handleItemClick = (type: 'stock' | 'keyword', item: string) => {
    if (type === 'stock') {
      setSelectedStock(selectedStock === item ? null : item);
    } else {
      setSelectedKeyword(selectedKeyword === item ? null : item);
    }
  };

  return (
    <div className={styles.container}>
      <h1 className={styles.title}>관리자 대시보드</h1>
      
      <div className={styles.tabContainer}>
        <div className={styles.tabs}>
          <button 
            className={`${styles.tab} ${activeTab === 'financial' ? styles.active : ''}`}
            onClick={() => setActiveTab('financial')}
          >
            증권사 재무제표
          </button>
          <button 
            className={`${styles.tab} ${activeTab === 'monitoring' ? styles.active : ''}`}
            onClick={() => setActiveTab('monitoring')}
          >
            해외주식 담보대출 모니터링
          </button>
          <button 
            className={`${styles.tab} ${activeTab === 'chat-history' ? styles.active : ''}`}
            onClick={() => setActiveTab('chat-history')}
          >
            채팅 이력 관리
          </button>
        </div>

        <div className={styles.content}>
          {activeTab === 'financial' && (
            <div className={styles.tableContainer}>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>번호</th>
                    <th>파일명</th>
                    <th>작성자</th>
                    <th>작성시간</th>
                    <th>다운로드</th>
                  </tr>
                </thead>
                <tbody>
                  {financialStatements.map((statement) => (
                    <tr key={statement.id}>
                      <td>{statement.id}</td>
                      <td>{statement.fileName}</td>
                      <td>{statement.author}</td>
                      <td>{statement.createdAt}</td>
                      <td>
                        <button className={styles.downloadButton}>
                          다운로드
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {activeTab === 'monitoring' && (
            <div className={styles.monitoringContainer}>
              <div className={styles.dateFilterSection}>
                <input 
                  type="date" 
                  className={styles.dateInput}
                  defaultValue={new Date().toISOString().split('T')[0]}
                  onChange={(e) => setSelectedDate(e.target.value.replace(/-/g, ''))}
                />
              </div>

              <div className={styles.contentLayout}>
                <div className={styles.tableWrapper}>
                  <div className={styles.tableContainer}>
                    <table className={styles.monitoringTable}>
                      <thead>
                        <tr>
                          <th>종목명</th>
                          <th>10-K</th>
                          <th>10-Q</th>
                          <th>8-K</th>
                          <th>뉴스</th>
                        </tr>
                      </thead>
                      <tbody>
                        {filingData?.overview?.[0] && Object.entries(filingData.overview[0]).map(([symbol, data]) => (
                          <tr key={symbol}>
                            <td>{symbol}</td>
                            <td>{data['10-K']}</td>
                            <td>{data['10-Q']}</td>
                            <td>{data['8-K']}</td>
                            <td>{data.news}</td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className={styles.summarySection}>
                  {getSummaryTexts().map((text, index) => (
                    <p key={index} className={styles.summaryText}>
                      {text}
                    </p>
                  ))}
                </div>
              </div>
            </div>
          )}

          {activeTab === 'chat-history' && (
            <ChatHistory />
          )}
        </div>
      </div>
    </div>
  );
} 