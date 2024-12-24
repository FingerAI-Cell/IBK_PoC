"use client";

import { useEffect, useState } from "react";
import { useService } from "../../context/ServiceContext";
import { serviceConfig } from "../../config/serviceConfig";
import styles from "./RecentHistory.module.css";

interface HistoryItem {
  id: number;
  text: string;
  time: string;
}

export function RecentHistory() {
  const { currentService } = useService();
  const [history, setHistory] = useState<HistoryItem[]>([]);
  const currentConfig = serviceConfig[currentService] || serviceConfig["general-chat"];
  
  const needsSmallerFont = (service: string) => {
    return ["branch-manual", "investment-report"].includes(service);
  };

  useEffect(() => {
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
  }, [currentService, currentConfig.historyKey]);

  return (
    <div className={styles.container}>
      <h2 className={`${styles.title} ${needsSmallerFont(currentService) ? styles.smallTitle : ''}`}>
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
