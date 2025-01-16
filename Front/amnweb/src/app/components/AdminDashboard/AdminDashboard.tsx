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

interface FinancialDocument {
  id: string;
  fileName: string;
  updateTime: string;
  category: string;
  filePath: string;
  yearQuarter: string;
}

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'financial' | 'monitoring' | 'chat-history'>('financial');
  const [selectedStock, setSelectedStock] = useState<string | null>(null);
  const [selectedKeyword, setSelectedKeyword] = useState<string | null>(null);
  const [filingData, setFilingData] = useState<FilingData | null>(null);
  const [selectedDate, setSelectedDate] = useState<string>(
    new Date().toISOString().split('T')[0].replace(/-/g, '')
  );
  const [financialDocuments, setFinancialDocuments] = useState<FinancialDocument[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [tempDate, setTempDate] = useState<string>(
    new Date().toISOString().split('T')[0]
  );

  useEffect(() => {
    const fetchData = async () => {
      try {
        setFilingData(null);
        
        // Overview 데이터 가져오기
        const overviewResponse = await fetch(`/api/admin/filing/overview/range?from_date=${selectedDate}&to_date=${selectedDate}`);
        
        if (!overviewResponse.ok) {
          throw new Error(`HTTP 상태 오류 (Overview): ${overviewResponse.status}`);
        }
        
        const overviewData = await overviewResponse.json();

        // Summary 데이터 가져오기
        const summaryResponse = await fetch(`/api/admin/filing/search?from_date=${selectedDate}&to_date=${selectedDate}`);
        
        if (!summaryResponse.ok) {
          throw new Error(`HTTP 상태 오류 (Summary): ${summaryResponse.status}`);
        }
        
        const summaryData = await summaryResponse.json();

        // 두 데이터 합치기
        const combinedData = {
          overview: overviewData.overview || [],
          summary: summaryData.summary || []
        };
        
        // 데이터가 비어있는지 확인
        if (!combinedData.overview.length && !combinedData.summary.length) {
          return;
        }
        
        setFilingData(combinedData);
      } catch (error) {
        console.error('데이터 불러오기 실패:', error);
      }
    };

    fetchData();
  }, [selectedDate]);

  useEffect(() => {
    const fetchDocuments = async () => {
      try {
        const response = await fetch('/api/financial-documents');
        if (!response.ok) throw new Error('서버 오류');
        const data = await response.json();
        setFinancialDocuments(data);
      } catch (error) {
        console.error('문서 목록 불러오기 실패:', error);
      } finally {
        setIsLoading(false);
      }
    };

    if (activeTab === 'financial') {
      fetchDocuments();
    }
  }, [activeTab]);

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

  const handleDownload = (filePath: string) => {
    const link = document.createElement('a');
    link.href = filePath;
    link.download = filePath.split('/').pop() || '';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  const handleItemClick = (type: 'stock' | 'keyword', item: string) => {
    if (type === 'stock') {
      setSelectedStock(selectedStock === item ? null : item);
    } else {
      setSelectedKeyword(selectedKeyword === item ? null : item);
    }
  };

  const handleDateSelect = (e: React.MouseEvent<HTMLInputElement>) => {
    const input = e.currentTarget;
    const originalOnChange = (e: Event) => {
      const target = e.target as HTMLInputElement;
      if (target.value) {
        setSelectedDate(target.value.replace(/-/g, ''));
        setTempDate(target.value);
      }
    };

    // 기존 change 이벤트 리스너 제거 후 다시 추가
    input.removeEventListener('change', originalOnChange);
    input.addEventListener('change', originalOnChange, { once: true });
  };

  // 텍스트 포맷팅 헬퍼 함수 추가
  const formatText = (text: string) => {
    return text
      .replace(/S_P_500/g, 'S\u0026P500')  // S_P_500을 S&P500으로 먼저 변경
      .replace(/S_P/g, 'S\u0026P')  // 다른 S_P 케이스 처리
      .replace(/_/g, ' ');  // 나머지 언더스코어를 띄어쓰기로 변경
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
              {isLoading ? (
                <div>로딩 중...</div>
              ) : (
                <table className={styles.table}>
                  <thead>
                    <tr>
                      <th>분기</th>
                      <th>파일명</th>
                      <th>종류</th>
                      <th>업데이트 시간</th>
                      <th>다운로드</th>
                    </tr>
                  </thead>
                  <tbody>
                    {financialDocuments.map((doc) => (
                      <tr key={doc.id}>
                        <td>{doc.yearQuarter}</td>
                        <td>{doc.fileName}</td>
                        <td>{doc.category}</td>
                        <td>{doc.updateTime}</td>
                        <td>
                          <button 
                            className={styles.downloadButton}
                            onClick={() => window.location.href = doc.filePath}
                          >
                            다운로드
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              )}
            </div>
          )}

          {activeTab === 'monitoring' && (
            <div className={styles.monitoringContainer}>
              <div className={styles.dateFilterSection}>
                <input 
                  type="date" 
                  className={styles.dateInput}
                  value={tempDate}
                  onClick={handleDateSelect}
                  onChange={(e) => setTempDate(e.target.value)}
                  onKeyDown={(e) => e.preventDefault()}
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
                            <td>{formatText(symbol)}</td>
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