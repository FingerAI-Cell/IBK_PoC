"use client";

import { useState, useEffect } from "react";
import styles from "./MeetingList.module.css";
import { apiConfig } from '../../config/serviceConfig';
import SttModal from '../SttModal/SttModal';
import SummaryModal from '../SummaryModal/SummaryModal';
import AlertModal from '../AlertModal/AlertModal';

interface Meeting {
  confId: number;
  title: string;
  startTime: string;
  endTime: string | null;
  summary: string | null;
  sttSrc: string | null;
  participants: number | null;
}

interface SttContent {
  speaker: string;
  text: string;
}

export default function MeetingList() {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [sttContents, setSttContents] = useState<SttContent[]>([]);
  const [currentSttSrc, setCurrentSttSrc] = useState<string | null>(null);
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);
  const [currentMeeting, setCurrentMeeting] = useState<Meeting | null>(null);
  const [isAlertOpen, setIsAlertOpen] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');

  // 회의 시간 계산 함수
  const calculateDuration = (start: string, end: string | null): string => {
    if (!end) return ''; // endTime이 null인 경우 빈 문자열 반환
    
    try {
      const startTime = new Date(start).getTime();
      const endTime = new Date(end).getTime();
      const diffMinutes = Math.round((endTime - startTime) / (1000 * 60));
      
      if (diffMinutes < 60) {
        return `${diffMinutes}분`;
      }
      const hours = Math.floor(diffMinutes / 60);
      const minutes = diffMinutes % 60;
      return minutes > 0 ? `${hours}시간 ${minutes}분` : `${hours}시간`;
    } catch (error) {
      console.error('시간 계산 오류:', error);
      return '';
    }
  };

  // 날짜 포맷팅 함수 추가
  const formatDate = (dateString: string): string => {
    try {
      const date = new Date(dateString);
      return date.toLocaleDateString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch (error) {
      console.error('날짜 변환 오류:', error);
      return dateString;
    }
  };

  // STT 내용 가져오기
  const fetchSttContent = async (sttSrc: string) => {
    try {
      const response = await fetch(`${apiConfig.baseURL}/api/meetings/stt`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ sttSrc }),
      });
      
      const result = await response.json();
      
      if (result.success) {
        setSttContents(result.data);
        setIsModalOpen(true);
        setCurrentSttSrc(sttSrc);
      } else {
        setAlertMessage('회의 원문을 찾을 수 없습니다.');
        setIsAlertOpen(true);
      }
    } catch (error) {
      setAlertMessage('회의 원문을 불러오는 중 오류가 발생했습니다.');
      setIsAlertOpen(true);
    }
  };

  // API 호출
  useEffect(() => {
    const fetchMeetings = async () => {
      try {
        const response = await fetch(`${apiConfig.baseURL}/api/meetings/`);
        const data = await response.json();
        console.log('API 응답:', data);
        setMeetings(data);
      } catch (error) {
        console.error('회의록 로딩 실패:', error);
      }
    };

    fetchMeetings();
  }, []);

  // 샘플 데이터
  const sampleSummary = {
    title: "생성형AI POC 프로젝트",
    date: "2024-11-19 AM10:00~11:00",
    participants: [
      "김지수 차장, 이승환 선임주임",
      "김종환 상무, 김태완 과장",
      "정한열 대표, 전현준 CTO"
    ],
    content: `[회의 내용]
- 당사와 평가와 계약완료. 금일로부터 1개월 이내(12월19일) 개발완료 예정
- 개발 완료 후 서버 당사로 이관하여 테스트 진행 가능
- LLM은 IBKS 서버에 탑재, LLM 학습 서버는 원라인에이아이 서버에 있음
...`
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>회의 목록</h2>
      <div className={styles.meetingList}>
        {meetings && meetings.map((meeting) => (
          <div key={meeting.confId} className={styles.meetingItem}>
            <div className={styles.meetingInfo}>
              <h3 className={styles.meetingTitle}>{meeting.title}</h3>
              <div className={styles.meetingDetails}>
                {formatDate(meeting.startTime)}
                {meeting.endTime && ' · ' + calculateDuration(meeting.startTime, meeting.endTime)}
              </div>
            </div>
            <div className={styles.buttonGroup}>
              <button 
                className={`${styles.originalButton}`}
                disabled={meeting.sttSrc === null}
                onClick={() => {
                  if (meeting.sttSrc) {
                    fetchSttContent(meeting.sttSrc);
                  }
                }}
              >
                원문보기
              </button>
              <button 
                className={`${styles.summaryButton}`}
                // disabled={meeting.summary === null}  // 추후 실제 데이터 연동 시 주석 해제
                onClick={() => {
                  setCurrentMeeting(meeting);
                  setIsSummaryModalOpen(true);
                }}
              >
                요약보기
              </button>
            </div>
          </div>
        ))}
      </div>
      
      <SttModal 
        isOpen={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        contents={sttContents}
        title={meetings.find(m => m.sttSrc === currentSttSrc)?.title || "회의 원문"}
      />
      <SummaryModal 
        isOpen={isSummaryModalOpen}
        onClose={() => setIsSummaryModalOpen(false)}
        title={sampleSummary.title}
        date={sampleSummary.date}
        participants={sampleSummary.participants}
        content={sampleSummary.content}
      />
      <AlertModal
        isOpen={isAlertOpen}
        onClose={() => setIsAlertOpen(false)}
        message={alertMessage}
      />
    </div>
  );
} 