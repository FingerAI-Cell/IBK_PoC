"use client";

import { useState } from 'react';
import styles from './AdminDashboard.module.css';
import { BiDownload } from 'react-icons/bi';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'financial' | 'monitoring'>('financial');
  const [selectedStock, setSelectedStock] = useState<string | null>(null);
  const [selectedKeyword, setSelectedKeyword] = useState<string | null>(null);

  // 재무제표 샘플 데이터
  const financialStatements = [
    { id: 1, fileName: '2024년 1분기 재무제표.xlsx', author: '김재무', createdAt: '2024-03-15 14:30' },
    { id: 2, fileName: '2023년 4분기 재무제표.xlsx', author: '이경제', createdAt: '2024-01-10 09:15' },
    { id: 3, fileName: '2023년 3분기 재무제표.xlsx', author: '박회계', createdAt: '2023-10-05 11:45' },
  ];

  // 모니터링 데이터
  const monitoringStocks = [
    '애플', '엔비디아', '아마존닷컴', '알파벳A', '버크셔 해서웨이'
  ];

  const monitoringKeywords = [
    '파산', '상장폐지', '합병', '회사분할', '주식분할', '티커변경'
  ];

  const monitoringResults = [
    { id: 1, date: '2024-01-03', content: '엔비디아 주식분할 논의 중', status: '주의' },
    { id: 2, date: '2024-01-02', content: '아마존 실적 발표', status: '정상' },
    { id: 3, date: '2024-01-01', content: '알파벳A 티커변경 예정', status: '주의' },
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
              <div className={styles.monitoringGrid}>
                {/* 모니터링 종목 */}
                <div className={styles.monitoringCard}>
                  <h3 className={styles.cardTitle}>모니터링 종목</h3>
                  <ul className={styles.monitoringList}>
                    {monitoringStocks.map((stock, index) => (
                      <li 
                        key={index} 
                        className={`${styles.monitoringItem} ${selectedStock === stock ? styles.selected : ''}`}
                        onClick={() => handleItemClick('stock', stock)}
                        role="button"
                        tabIndex={0}
                      >
                        {stock}
                      </li>
                    ))}
                  </ul>
                </div>

                {/* 모니터링 키워드 */}
                <div className={styles.monitoringCard}>
                  <h3 className={styles.cardTitle}>모니터링 키워드</h3>
                  <ul className={styles.monitoringList}>
                    {monitoringKeywords.map((keyword, index) => (
                      <li 
                        key={index} 
                        className={`${styles.monitoringItem} ${selectedKeyword === keyword ? styles.selected : ''}`}
                        onClick={() => handleItemClick('keyword', keyword)}
                        role="button"
                        tabIndex={0}
                      >
                        {keyword}
                      </li>
                    ))}
                  </ul>
                </div>

                {/* 모니터링 결과 */}
                <div className={styles.monitoringCard}>
                  <h3 className={styles.cardTitle}>모니터링 결과</h3>
                  <ul className={styles.resultsList}>
                    {monitoringResults.map((result) => (
                      <li key={result.id} className={styles.resultItem}>
                        <div className={styles.resultContent}>
                          <div className={styles.resultDate}>{result.date}</div>
                          <div className={styles.resultText}>{result.content}</div>
                        </div>
                        <button className={styles.iconButton} aria-label="다운로드">
                          <BiDownload size={18} />
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
} 