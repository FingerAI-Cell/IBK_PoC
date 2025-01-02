"use client";

import { useEffect, useState } from "react";
import { useService } from "../../context/ServiceContext";
import { serviceConfig } from "../../config/serviceConfig";
import styles from "./RecentHistory.module.css";
import { investmentReportHistory } from "../../data/investmentReportData";

interface HistoryItem {
  id: number;
  text: string;
  time: string;
}

interface InvestmentHistoryItem {
  id: number;
  date: string;
  title: string;
}

export function RecentHistory() {
  const { currentService, setReportDate } = useService();
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const [investmentHistory, setInvestmentHistory] = useState<InvestmentHistoryItem[]>([]);
  const currentConfig = serviceConfig[currentService] || serviceConfig["general-chat"];

  useEffect(() => {
    if (currentService === 'investment-report') {
      // API 호출로 대체 가능
      setInvestmentHistory(investmentReportHistory);
    } else {
      const historyKey = currentConfig.historyKey;
      try {
        const savedHistory = localStorage.getItem(historyKey);
        if (savedHistory) {
          setHistory(JSON.parse(savedHistory));
        } else {
          setHistory([]);
        }
      } catch (error) {
        console.error('히스토리 로드 중 오류:', error);
        setHistory([]);
      }
    }
  }, [currentService, currentConfig.historyKey]);

  const handleReportClick = async (date: string) => {
    try {
      // API 호출 시도
      const response = await fetch(`/api/investment-report/${date}`);
      if (response.ok) {
        const data = await response.json();
        setReportDate(date, data);
      } else {
        // API 호출 실패시 샘플 데이터 사용
        setReportDate(date);
      }
    } catch (error) {
      console.error('리포트 로드 중 오류:', error);
      setReportDate(date);
    }
  };

  if (currentService === 'investment-report') {
    return (
      <div className={styles.container}>
        <h2 className={styles.title}>투자정보 리포트 목록</h2>
        {investmentHistory.length > 0 ? (
          <ul className={styles.historyList}>
            {investmentHistory.map((item) => (
              <li 
                key={item.id} 
                className={styles.historyItem}
                onClick={() => handleReportClick(item.date)}
              >
                <p className={styles.historyText}>{item.title}</p>
                <p className={styles.historyTime}>{item.date}</p>
              </li>
            ))}
          </ul>
        ) : (
          <p className={styles.emptyMessage}>리포트 목록이 없습니다.</p>
        )}
      </div>
    );
  }

  // 기존 채팅 히스토리 렌더링
  return (
    <div className={styles.container}>
      <h2 className={styles.title}>
        {currentService === "general-chat" 
          ? "최근 이력" 
          : `${currentConfig.title} 최근 이력`}
      </h2>
      {history.length > 0 ? (
        <ul className={styles.historyList}>
          {history.map((item) => (
            <li key={item.id} className={styles.historyItem}>
              <p className={styles.historyText}>{item.text}</p>
              <p className={styles.historyTime}>{item.time}</p>
            </li>
          ))}
        </ul>
      ) : (
        <p className={styles.emptyMessage}>최근 대화 이력이 없습니다.</p>
      )}
    </div>
  );
}
