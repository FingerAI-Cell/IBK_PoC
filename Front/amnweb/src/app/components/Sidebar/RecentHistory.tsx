"use client";

import { useEffect, useState } from "react";
import { useService } from "../../context/ServiceContext";
import { serviceConfig } from "../../config/serviceConfig";

interface HistoryItem {
  id: number;
  text: string;
  time: string;
}

export function RecentHistory() {
  const { currentService } = useService();
  const [history, setHistory] = useState<HistoryItem[]>([]);
  
  useEffect(() => {
    // 현재 서비스 설정이 유효한지 확인
    const currentConfig = serviceConfig[currentService] || serviceConfig["general-chat"];
    const historyKey = currentConfig.historyKey;
    
    try {
      const savedHistory = localStorage.getItem(historyKey);
      if (savedHistory) {
        setHistory(JSON.parse(savedHistory));
      } else {
        setHistory([]); // 히스토리가 없으면 빈 배열
      }
    } catch (error) {
      console.error('히스토리 로드 중 오류:', error);
      setHistory([]); // 에러 발생 시 빈 배열로 초기화
    }
  }, [currentService]);

  return (
    <div className="p-6 border-t border-gray-300">
      <h2 className="text-lg font-bold mb-4">최근 이력</h2>
      {history.length > 0 ? (
        <ul className="space-y-2">
          {history.map((item) => (
            <li key={item.id} className="text-sm">
              <p className="font-medium">{item.text}</p>
              <p className="text-xs text-gray-500">{item.time}</p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="text-sm text-gray-500">최근 대화 이력이 없습니다.</p>
      )}
    </div>
  );
}
