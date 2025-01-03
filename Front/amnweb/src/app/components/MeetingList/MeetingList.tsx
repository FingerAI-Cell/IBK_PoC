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
  participants: number | null;
  sttSign: boolean;
}

interface Speaker {
  speakerId: string;
  cuserId: number;
  name: string;
}

interface LogContent {
  content: string;
  cuserId: number;
  name: string;
}

export default function MeetingList() {
  const [meetings, setMeetings] = useState<Meeting[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [isSummaryModalOpen, setIsSummaryModalOpen] = useState(false);
  const [isAlertOpen, setIsAlertOpen] = useState(false);
  const [alertMessage, setAlertMessage] = useState('');
  const [speakers, setSpeakers] = useState<Speaker[]>([]);
  const [logContents, setLogContents] = useState<LogContent[]>([]);
  const [currentMeetingId, setCurrentMeetingId] = useState<number | null>(null);
  const [summaryData, setSummaryData] = useState({
    title: '',
    date: '',
    participants: [],
    content: ''
  });

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

  // speakers와 logs를 가져오는 함수
  const fetchSttContent = async (meetingId: number) => {
    try {
      // 1. speakers 데이터 가져오기
      const speakersResponse = await fetch(`${apiConfig.baseURL}/api/meetings/speakers`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ meetingId }),
      });
      
      const speakersResult = await speakersResponse.json();
      
      // 2. logs 데이터 가져오기
      const logsResponse = await fetch(`${apiConfig.baseURL}/api/meetings/logs`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ meetingId }),
      });
      
      const logsResult = await logsResponse.json();
      
      if (speakersResult.success && logsResult.success) {
        setSpeakers(speakersResult.data);
        setLogContents(logsResult.data);
        setCurrentMeetingId(meetingId);
        setIsModalOpen(true);
      } else {
        setAlertMessage('회의 원문을 찾을 수 없습니다.');
        setIsAlertOpen(true);
      }
    } catch (error) {
      console.error('API 호출 오류:', error);
      setAlertMessage('회의 원문을 불러오는 중 오류가 발생했습니다.');
      setIsAlertOpen(true);
    }
  };

  // API 호출
  useEffect(() => {
    const fetchMeetings = async () => {
      try {
        const response = await fetch(`${apiConfig.baseURL}/api/meetings/`, {
          method: 'GET',
          headers: {
            'Accept': 'application/json'
          }
        });
        
        if (!response.ok) {
          throw new Error('회의록 목록을 불러오는데 실패했습니다.');
        }

        const data = await response.json();
        setMeetings(data);
      } catch (error) {
        console.error('회의록 로딩 실패:', error);
        setAlertMessage('회의록 목록을 불러오는데 실패했습니다.');
        setIsAlertOpen(true);
      }
    };

    fetchMeetings();
  }, []);

  const handleSummaryClick = async (meeting: Meeting) => {
    try {
      // 1. speakers 데이터 가져오기
      const speakersResponse = await fetch(`${apiConfig.baseURL}/api/meetings/speakers`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ meetingId: meeting.confId }),
      });
      
      const speakersResult = await speakersResponse.json();
      
      if (speakersResult.success) {
        const formattedDate = formatMeetingTime(meeting.startTime, meeting.endTime);
        const participantNames = speakersResult.data.map((speaker: Speaker) => speaker.name || speaker.speakerId);

        setIsSummaryModalOpen(true);
        setSummaryData({
          title: meeting.title,
          date: formattedDate,
          participants: participantNames,
          content: meeting.summary || ''
        });
      }
    } catch (error) {
      console.error('요약 데이터 로딩 실패:', error);
      setAlertMessage('요약 데이터를 불러오는데 실패했습니다.');
      setIsAlertOpen(true);
    }
  };

  // 시간 포맷팅 함수
  const formatMeetingTime = (start: string, end: string | null): string => {
    const startTime = new Date(start);
    const formattedStart = startTime.toLocaleString('ko-KR', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit',
      hour: '2-digit',
      minute: '2-digit'
    });
    
    if (!end) {
      return `${formattedStart} ~ ?`;
    }
    
    const endTime = new Date(end);
    const formattedEnd = endTime.toLocaleString('ko-KR', {
      hour: '2-digit',
      minute: '2-digit'
    });
    
    return `${formattedStart} ~ ${formattedEnd}`;
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
                disabled={!meeting.sttSign}
                onClick={() => fetchSttContent(meeting.confId)}
              >
                원문보기
              </button>
              <button 
                className={`${styles.summaryButton}`}
                disabled={!meeting.summary}
                onClick={() => handleSummaryClick(meeting)}
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
        speakers={speakers}
        contents={logContents}
        title={meetings.find(m => m.confId === currentMeetingId)?.title || "회의 원문"}
        confId={currentMeetingId!}
        onSummarize={() => {
          // 요약 완료 후 처리
        }}
      />
      <SummaryModal 
        isOpen={isSummaryModalOpen}
        onClose={() => setIsSummaryModalOpen(false)}
        title={summaryData.title}
        date={summaryData.date}
        participants={summaryData.participants}
        content={summaryData.content}
      />
      <AlertModal
        isOpen={isAlertOpen}
        onClose={() => setIsAlertOpen(false)}
        message={alertMessage}
      />
    </div>
  );
} 