"use client";

import { useState } from 'react';
import styles from './AdminDashboard.module.css';

export default function AdminDashboard() {
  const [activeTab, setActiveTab] = useState('financial');
  const [searchTerm, setSearchTerm] = useState('');

  // 재무제표 샘플 데이터
  const financialStatements = [
    { id: 1, fileName: '2024년 1분기 재무제표.xlsx', author: '김재무', createdAt: '2024-03-15 14:30' },
    { id: 2, fileName: '2023년 4분기 재무제표.xlsx', author: '이경제', createdAt: '2024-01-10 09:15' },
    { id: 3, fileName: '2023년 3분기 재무제표.xlsx', author: '박회계', createdAt: '2023-10-05 11:45' },
  ];

  // 영업점 메뉴얼 샘플 데이터
  const branchManuals = [
    { id: 1, name: '신규 계좌 개설 가이드', version: '2.1', updatedAt: '2024-03-20' },
    { id: 2, name: '고객 상담 매뉴얼', version: '1.5', updatedAt: '2024-02-15' },
    { id: 3, name: '금융 상품 안내서', version: '3.0', updatedAt: '2024-01-30' },
  ];

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
            className={`${styles.tab} ${activeTab === 'manual' ? styles.active : ''}`}
            onClick={() => setActiveTab('manual')}
          >
            영업점 메뉴얼 정보 조회
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

          {activeTab === 'manual' && (
            <div className={styles.tableContainer}>
              <div className={styles.searchContainer}>
                <input
                  type="text"
                  placeholder="메뉴얼 검색..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className={styles.searchInput}
                />
              </div>
              <table className={styles.table}>
                <thead>
                  <tr>
                    <th>번호</th>
                    <th>메뉴얼명</th>
                    <th>버전</th>
                    <th>최근 수정일</th>
                    <th>다운로드</th>
                  </tr>
                </thead>
                <tbody>
                  {branchManuals
                    .filter(manual => 
                      manual.name.toLowerCase().includes(searchTerm.toLowerCase())
                    )
                    .map((manual) => (
                      <tr key={manual.id}>
                        <td>{manual.id}</td>
                        <td>{manual.name}</td>
                        <td>{manual.version}</td>
                        <td>{manual.updatedAt}</td>
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
        </div>
      </div>
    </div>
  );
} 