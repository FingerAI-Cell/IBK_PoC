"use client";

import { useState } from 'react';
import styles from './AdminDashboard.module.css';
import ChatHistory from './ChatHistory/ChatHistory';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState<'financial' | 'monitoring' | 'chat-history'>('financial');
  const [selectedStock, setSelectedStock] = useState<string | null>(null);
  const [selectedKeyword, setSelectedKeyword] = useState<string | null>(null);

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
                  onChange={(e) => console.log(e.target.value)}
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
                        <tr><td>엔비디아</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>아마존닷컴</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>알파벳 A</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>알파벳 C</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>메타 플랫폼스</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>버크셔 해서웨이</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>일라이 릴리</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                        <tr><td>프록터앤드</td><td>X</td><td>X</td><td>X</td><td>X</td></tr>
                      </tbody>
                    </table>
                  </div>
                </div>

                <div className={styles.summarySection}>
                  <p className={styles.summaryText}>
                    - 마크로 인수합병과 분사를 통해 사업을 확장하고 재편하고 있습니다. 2024년에는 아이데이아도, 하트 이어로 인수하였습니다. 2023년에는 피오니어스, 바이오사이언스, 이러고 바이오사이언스 등을 인수하였습니다. 2021년에는 요가를 분사하였습니다.
                  </p>
                  <p className={styles.summaryText}>
                    - ExxonMobile은 2024년 5월 3일 Pioneer Natural Resources를 인수하였습니다. 이 인수를 통해 ExxonMobile은 Permian 지역의 생산량을 두 배로 늘릴 수 있게 되었고 시너지 효과를 기대할 수 있게 되었습니다. 특히 Advantaged Volume Growth 부문에서 Permian 지역 생산 증가와 Pioneer 인수 효과로 우위가 크게 높아졌습니다. 현재 Pioneer 인수 관련 세부도 잘 진행되고 있어서 이 인수를 통해 ExxonMobile의 Upstream 부문 전망이 개선되었습니다. 앞으로도 ExxonMobile은 Pioneer 인수를 통해 성장할 것으로 전망됩니다.
                  </p>
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