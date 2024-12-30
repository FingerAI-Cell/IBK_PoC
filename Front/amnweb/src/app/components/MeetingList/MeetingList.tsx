"use client";

import { useState, useEffect } from "react";
import styles from "./MeetingList.module.css";

interface Meeting {
  id: string;
  title: string;
  date: string;
  duration: string;
  size: string;
  content: string;
  summary: string;
}

const sampleMeetings: Meeting[] = [
  {
    id: "1",
    title: "주간 영업회의",
    date: "2024-12-20",
    duration: "45분",
    size: "12MB",
    content: "1. 프로젝트 진행 현황\n- AI 챗봇 서비스 개발 진행률 85%\n- 사용자 피드백 반영 중\n\n2. 향후 계획\n- 4월 중 베타 서비스 출시 예정\n- 마케팅 전략 수립 필요",
    summary: "AI 챗봇 서비스 개발 진행률 85% 달성. 4월 베타 출시 예정. 성능 최적화와 보안 점검 필요."
  },
  {
    id: "2",
    title: "해외주식 전략회의",
    date: "2024-12-19",
    duration: "60분",
    size: "15MB",
    content: "1. 현재 UI 문제점\n- 사용자 피드백 분석 결과\n- 개선이 필요한 부분\n\n2. 개선 방향\n- 직관적인 네비게이션 구조\n- 모바일 대응 강화",
    summary: "UI/UX 개선을 위한 사용자 피드백 분석 완료. 네비게이션 구조 개선과 모바일 대응 강화 예정."
  }
];

export default function MeetingList() {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [selectedMeeting, setSelectedMeeting] = useState<Meeting | null>(null);
  const [viewMode, setViewMode] = useState<'original' | 'summary'>('original');

  // DB에서 회의록 목록을 가져오는 함수
  const fetchMeetings = async () => {
    try {
      const response = await fetch('/api/meetings');
      const data = await response.json();
      setMeetings(data);
    } catch (error) {
      console.error('회의록 로딩 실패:', error);
    }
  };

  useEffect(() => {
    setMeetings(sampleMeetings);
  }, []);

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>회의록 녹음파일</h2>
      <div className={styles.meetingList}>
        {meetings.map((meeting) => (
          <div key={meeting.id} className={styles.meetingItem}>
            <div className={styles.meetingInfo}>
              <h3 className={styles.meetingTitle}>{meeting.title}</h3>
              <div className={styles.meetingDetails}>
                {meeting.date} · {meeting.duration} · {meeting.size}
              </div>
            </div>
            <div className={styles.buttonGroup}>
              <button className={styles.summaryButton}>
                요약보기
              </button>
              <button className={styles.originalButton}>
                원문보기
              </button>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
} 