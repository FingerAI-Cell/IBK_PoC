import { useState, useMemo, useEffect } from 'react';
import styles from './SttModal.module.css';
import { apiConfig } from '../../config/serviceConfig';

interface Speaker {
  speakerId: string;  // "SPEAKER_00" 형식의 문자열
  cuserId: number;
  name: string;
}

interface LogContent {
  content: string;
  cuserId: number;
  name: string;
  startTime?: string;  // optional로 추가
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

export default function SttModal({ 
  isOpen, 
  onClose, 
  speakers, 
  contents, 
  title = "회의 원문", 
  onSummarize,
  confId
}: SttModalProps) {
  const [speakerNames, setSpeakerNames] = useState<Record<string, string>>({});
  const [selectedSpeakers, setSelectedSpeakers] = useState<Set<string>>(new Set());
  const [searchText, setSearchText] = useState('');
  
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

  // 중복 없는 스피커 목록 (알파벳 순 정렬)
  const uniqueSpeakers = useMemo(() => {
    return speakers.sort((a, b) => a.speakerId.localeCompare(b.speakerId));
  }, [speakers]);

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

  // // 필터링 및 검색이 적용된 내용
  // const processedContents = useMemo(() => {
  //   return contents.filter(content => {
  //     // 스피커 필터링
  //     if (selectedSpeakers.size > 0) {
  //       const speaker = speakers.find(s => s.cuserId === content.cuserId);
  //       if (!speaker || !selectedSpeakers.has(speaker.speakerId)) {
  //         return false;
  //       }
  //     }
  //     // 텍스트 검색
  //     if (searchText && !content.content.toLowerCase().includes(searchText.toLowerCase())) {
  //       return false;
  //     }
  //     return true;
  //   });
  // }, [contents, speakers, selectedSpeakers, searchText]);

  // 시간 기준으로 정렬된 contents
  const sortedContents = useMemo(() => {
    return [...contents].sort((a, b) => {
      // startTime이 없는 경우 맨 뒤로
      if (!a.startTime) return 1;
      if (!b.startTime) return -1;
      return new Date(a.startTime).getTime() - new Date(b.startTime).getTime();
    });
  }, [contents]);

  const handleSummarize = async () => {
    try {
      const response = await fetch(`${apiConfig.baseURL}/api/meetings/summarize`, {
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

  if (!isOpen) return null;

  return (
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
          <div className={styles.filterTitle}>발화자 설정</div>
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
            const speaker = speakers.find(s => s.cuserId === content.cuserId);
            const displayName = speaker?.speakerId  // speakerId가 있을 때만 처리
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
}
