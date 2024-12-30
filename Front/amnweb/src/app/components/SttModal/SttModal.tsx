import { useState, useMemo } from 'react';
import styles from './SttModal.module.css';

interface SttContent {
  speaker: string;
  text: string;
}

interface SttModalProps {
  isOpen: boolean;
  onClose: () => void;
  contents: SttContent[];
  title?: string;
  onSummarize?: () => void;
}

export default function SttModal({ isOpen, onClose, contents, title = "회의 원문", onSummarize }: SttModalProps) {
  const [speakerNames, setSpeakerNames] = useState<Record<string, string>>({});
  const [selectedSpeakers, setSelectedSpeakers] = useState<Set<string>>(new Set());
  const [searchText, setSearchText] = useState('');
  
  // 중복 없는 스피커 목록 (알파벳 순 정렬)
  const speakers = useMemo(() => {
    const uniqueSpeakers = Array.from(new Set(contents.map(content => content.speaker)));
    return uniqueSpeakers.sort();
  }, [contents]);

  // 스피커 이름 변경 처리
  const handleSpeakerNameChange = (originalName: string, newName: string) => {
    setSpeakerNames(prev => ({
      ...prev,
      [originalName]: newName
    }));
  };

  // 스피커 필터 토글
  const toggleSpeaker = (speaker: string) => {
    setSelectedSpeakers(prev => {
      const newSet = new Set(prev);
      if (newSet.has(speaker)) {
        newSet.delete(speaker);
      } else {
        newSet.add(speaker);
      }
      return newSet;
    });
  };

  // 필터링 및 검색이 적용된 내용
  const processedContents = useMemo(() => {
    return contents
      .filter(content => {
        // 스피커 필터링
        if (selectedSpeakers.size > 0 && !selectedSpeakers.has(content.speaker)) {
          return false;
        }
        // 텍스트 검색
        if (searchText && !content.text.toLowerCase().includes(searchText.toLowerCase())) {
          return false;
        }
        return true;
      })
      .map(content => ({
        ...content,
        speaker: speakerNames[content.speaker] || content.speaker
      }));
  }, [contents, speakerNames, selectedSpeakers, searchText]);

  if (!isOpen) return null;

  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <h2 className={styles.modalTitle}>{title}</h2>
          <div className={styles.headerButtons}>
            <button 
              className={styles.summarizeButton}
              onClick={onSummarize}
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
            {speakers.map(speaker => (
              <div key={speaker} className={styles.speakerItem}>
                <span className={styles.originalSpeaker}>{speaker}</span>
                <input
                  type="text"
                  className={styles.speakerInput}
                  value={speakerNames[speaker] || ''}
                  onChange={(e) => handleSpeakerNameChange(speaker, e.target.value)}
                  placeholder="발화자 이름 입력"
                />
                <button
                  className={`${styles.filterButton} ${
                    selectedSpeakers.has(speaker) ? styles.selected : ''
                  }`}
                  onClick={() => toggleSpeaker(speaker)}
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
          {processedContents.map((content, index) => (
            <div key={index} className={styles.sttItem}>
              <span className={styles.speaker}>
                {speakerNames[content.speaker] || content.speaker}:
              </span>
              <span className={styles.text}>{content.text}</span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
