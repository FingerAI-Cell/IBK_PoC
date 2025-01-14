import { useState, useMemo, useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './SttModal.module.css';
import { meetings } from '@/data/meetings';  // meetings 데이터 import 추가

interface Speaker {
  speakerId: string;  // "SPEAKER_00" 형식의 문자열
  cuserId: number;
  name: string;
}

interface LogContent {
  content: string;
  cuserId: number;
  name: string;
  startTime: string;  // optional에서 required로 변경
}

interface SpeakerUpdate {
  speakerId: string;
  cuserId: number;
  name: string;
}

interface SttModalProps {
  isOpen: boolean;
  onClose: () => void;
  speakers: Speaker[];
  contents: LogContent[];
  title: string;
  onSummarize?: () => void;
  confId?: number;
}

// 샘플 데이터를 meetings.ts와 일치하도록 수정
const SAMPLE_SPEAKERS: Speaker[] = [
  { speakerId: "SPEAKER_00", cuserId: 0, name: "SPEAKER_00" },
  { speakerId: "SPEAKER_01", cuserId: 1, name: "SPEAKER_01" },
  { speakerId: "SPEAKER_02", cuserId: 2, name: "SPEAKER_02" },
  { speakerId: "SPEAKER_03", cuserId: 3, name: "SPEAKER_03" }
];

const SAMPLE_CONTENTS: LogContent[] = [
  { 
    content: "안녕하세요. 오늘은 LLM 모델에서 발생하는 gen_config 속성 에러에 대해 논의하도록 하겠습니다.", 
    cuserId: 0, 
    name: "SPEAKER_00",
    startTime: "2024-12-27T10:07:15"
  },
  { 
    content: "네, 현재 발생하는 에러는 'LLMOpenAI' 객체에서 gen_config 속성을 찾을 수 없다는 내용입니다.", 
    cuserId: 1, 
    name: "SPEAKER_01",
    startTime: "2024-12-27T10:07:15"
  },
  { 
    content: "이 문제는 최근 업데이트된 버전에서 API 인터페이스가 변경되면서 발생한 것으로 보입니다.", 
    cuserId: 2, 
    name: "SPEAKER_02",
    startTime: "2024-12-27T10:07:15"
  }
];

export default function SttModal({ 
  isOpen, 
  onClose, 
  speakers, 
  contents, 
  title = "회의 원문", 
  onSummarize,
  confId
}: SttModalProps) {
  const [mounted, setMounted] = useState(false);
  const [speakerNames, setSpeakerNames] = useState<Record<string, string>>({});
  const [selectedSpeakers, setSelectedSpeakers] = useState<Set<string>>(new Set());
  const [searchText, setSearchText] = useState('');
  
  useEffect(() => {
    setMounted(true);
    return () => setMounted(false);
  }, []);

  // speakers가 변경될 때마다 speakerNames 초기화
  useEffect(() => {
    const initialNames: Record<string, string> = {};
    speakers.forEach(speaker => {
      if (speaker?.speakerId) {  // speakerId가 있을 때만 초기화
        initialNames[speaker.speakerId] = speaker.name;
      }
    });
    setSpeakerNames(initialNames);
  }, [speakers]);

  // API 실패시 meetings.ts에서 해당 회의 데이터를 가져오는 로직
  const getFallbackData = (confId: number) => {
    const meeting = meetings.find(m => m.confId === confId);
    if (!meeting) return { speakers: [], contents: [] };

    const uniqueSpeakers = Array.from(new Set(meeting.content.map(c => c.speaker)));
    const speakers = uniqueSpeakers.map((speakerId, index) => ({
      speakerId,
      cuserId: index,
      name: `SPEAKER_${String(index).padStart(2, '0')}`
    }));

    const contents = meeting.content.map(item => ({
      content: item.text,
      cuserId: parseInt(item.speaker.split('_')[1]),
      name: item.speaker,
      startTime: meeting.startTime
    }));

    return { speakers, contents };
  };

  // 실제 데이터 또는 폴백 데이터 사용
  const displaySpeakers = speakers.length > 0 
    ? speakers 
    : (confId ? getFallbackData(confId).speakers : SAMPLE_SPEAKERS);

  const displayContents = contents.length > 0 
    ? contents 
    : (confId ? getFallbackData(confId).contents : SAMPLE_CONTENTS);

  const uniqueSpeakers = useMemo(() => {
    return displaySpeakers.sort((a, b) => a.speakerId.localeCompare(b.speakerId));
  }, [displaySpeakers]);

  // 스피커 이름 변경 처리
  const handleSpeakerNameChange = (speakerId: string, newName: string) => {
    setSpeakerNames(prev => ({
      ...prev,
      [speakerId]: newName
    }));
  };

  // 스피커 필터 토글
  const toggleSpeaker = (speakerId: string) => {
    setSelectedSpeakers(prev => {
      const newSet = new Set(prev);
      if (newSet.has(speakerId)) {
        newSet.delete(speakerId);
      } else {
        newSet.add(speakerId);
      }
      return newSet;
    });
  };

  // 필터링 및 검색이 적용된 내용 (주석 해제)
  const processedContents = useMemo(() => {
    return displayContents.filter(content => {
      // 스피커 필터링
      if (selectedSpeakers.size > 0) {
        const speaker = displaySpeakers.find(s => s.cuserId === content.cuserId);
        if (!speaker || !selectedSpeakers.has(speaker.speakerId)) {
          return false;
        }
      }
      // 텍스트 검색
      if (searchText && !content.content.toLowerCase().includes(searchText.toLowerCase())) {
        return false;
      }
      return true;
    });
  }, [displayContents, displaySpeakers, selectedSpeakers, searchText]);

  // sortedContents를 processedContents로 변경
  const sortedContents = useMemo(() => {
    return [...processedContents].sort((a, b) => {
      if (!a.startTime) return 1;
      if (!b.startTime) return -1;
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
    });
  }, [processedContents]);

  const handleSummarize = async () => {
    try {
      const response = await fetch('/api/meetings/summarize', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({ confId }),
      });

      const result = await response.json();
      if (result.success) {
        onSummarize?.();
      }
    } catch (error) {
      console.error('요약 생성 실패:', error);
    }
  };

  // 발화자 이름 저장 처리
  const handleSaveSpeakerNames = async () => {
    try {
      const speakerUpdates: SpeakerUpdate[] = speakers.map(speaker => ({
        speakerId: speaker.speakerId,
        cuserId: speaker.cuserId,
        name: speakerNames[speaker.speakerId] || ''
      }));

      const response = await fetch('/api/meetings/speakers/update', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          confId,
          speakers: speakerUpdates
        }),
      });

      const result = await response.json();
      if (result.success) {
        alert('발화자 이름이 저장되었습니다.');
      } else {
        throw new Error('발화자 이름 저장 실패');
      }
    } catch (error) {
      console.error('발화자 이름 저장 오류:', error);
      alert('발화자 이름 저장에 실패했습니다.');
    }
  };

  if (!isOpen || !mounted) return null;

  const modalContent = (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <h2 className={styles.modalTitle}>{title}</h2>
          <div className={styles.headerButtons}>
            <button 
              className={styles.summarizeButton}
              onClick={handleSummarize}
            >
              요약하기
            </button>
            <button 
              className={styles.closeButton}
              onClick={onClose}
            >
              ✕
            </button>
          </div>
        </div>
        <div className={styles.speakerFilter}>
          <div className={styles.filterHeader}>
            <div className={styles.filterTitle}>발화자 설정</div>
            <button 
              className={styles.saveSpeakersButton}
              onClick={handleSaveSpeakerNames}
            >
              저장
            </button>
          </div>
          <div className={styles.speakerList}>
            {uniqueSpeakers.map(speaker => (
              <div key={speaker.speakerId} className={styles.speakerItem}>
                <span className={styles.originalSpeaker}>{speaker.speakerId}</span>
                <input
                  type="text"
                  className={styles.speakerInput}
                  defaultValue={speaker.name}
                  onChange={(e) => handleSpeakerNameChange(speaker.speakerId, e.target.value)}
                  placeholder="발화자 이름 입력"
                />
                <button
                  className={`${styles.filterButton} ${
                    selectedSpeakers.has(speaker.speakerId) ? styles.selected : ''
                  }`}
                  onClick={() => toggleSpeaker(speaker.speakerId)}
                >
                  필터
                </button>
              </div>
            ))}
          </div>
          <div className={styles.searchBox}>
            <input
              type="text"
              className={styles.searchInput}
              value={searchText}
              onChange={(e) => setSearchText(e.target.value)}
              placeholder="대화 내용 검색..."
            />
          </div>
        </div>
        <div className={styles.modalContent}>
          {sortedContents.map((content, index) => {
            const speaker = displaySpeakers.find(s => s.cuserId === content.cuserId);
            const displayName = speaker?.speakerId
              ? (speakerNames[speaker.speakerId] === undefined 
                  ? speaker.name || speaker.speakerId
                  : speakerNames[speaker.speakerId] || speaker.speakerId)
              : content.name;
            
            return (
              <div key={index} className={styles.sttItem}>
                <span className={styles.speaker}>{displayName}:</span>
                <span className={styles.text}>{content.content}</span>
              </div>
            );
          })}
        </div>
      </div>
    </div>
  );

  // document가 있는지 확인 (SSR 대비)
  if (typeof document === 'undefined') return null;

  const modalRoot = document.getElementById('modal-root');
  if (!modalRoot) return null;

  return createPortal(modalContent, modalRoot);
}
