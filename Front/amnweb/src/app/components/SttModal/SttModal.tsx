import { useState, useMemo, useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './SttModal.module.css';

interface Speaker {
  speakerId: string;  // "SPEAKER_00" 형식의 문자열
  cuserId: number;
  name: string;
}

interface LogContent {
  content: string;
  cuserId: number | null;  // null 가능하도록 수정
  name: string;
  startTime: string;
  logId: number;  // logId 추가
}

interface SpeakerUpdate {
  speakerId: string;
  cuserId: number;
  name: string;
}

interface ChangedLog {
  logId: number;
  cuserId: number | null;
}

interface SttModalProps {
  isOpen: boolean;
  onClose: () => void;
  speakers: Speaker[];
  contents: LogContent[];
  title: string;
  loadingStates: { [key: number]: boolean }; // 추가
  setLoadingStates: React.Dispatch<React.SetStateAction<{ [key: number]: boolean }>>; // 추가
  onSummarize?: () => void;
  confId?: number;
}

export default function SttModal({ 
  isOpen, 
  onClose, 
  speakers, 
  contents: initialContents,  // contents를 initialContents로 이름 변경
  title = "회의 원문", 
  onSummarize,
  confId,
  setLoadingStates,
}: SttModalProps) {
  const [mounted, setMounted] = useState(false);
  const [speakerNames, setSpeakerNames] = useState<Record<string, string>>({});
  const [selectedSpeakers, setSelectedSpeakers] = useState<Set<string>>(new Set());
  const [searchText, setSearchText] = useState('');
  const [changedLogs, setChangedLogs] = useState<ChangedLog[]>([]);
  const [logContents, setLogContents] = useState<LogContent[]>([]);  // logContents 상태 추가
  
  useEffect(() => {
    setMounted(true);
    return () => setMounted(false);
  }, []);

  useEffect(() => {
    console.log("logContents 상태 변경됨:", logContents);
  }, [logContents]);

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

  // 초기 contents를 logContents에 설정
  useEffect(() => {
    setLogContents(initialContents);
  }, [initialContents]);

  // 실제 데이터 또는 폴백 데이터 사용
  const displaySpeakers = speakers.length > 0 
    ? speakers 
    : [];

  const displayContents = initialContents.length > 0 
    ? initialContents 
    : [];

  const uniqueSpeakers = useMemo(() => {
    return displaySpeakers
      .sort((a, b) => {
        // UNKNOWN은 항상 마지막으로
        if (a.speakerId === 'UNKNOWN') return 1;
        if (b.speakerId === 'UNKNOWN') return -1;
        // 나머지는 기존처럼 알파벳 순
        return a.speakerId.localeCompare(b.speakerId);
      });
  }, [displaySpeakers]);

  // 스피커 이름 변경 처리
  const handleSpeakerNameChange = (speakerId: string, newName: string) => {
    // speakerNames 상태 업데이트
    setSpeakerNames((prev) => ({
      ...prev,
      [speakerId]: newName,
    }));
  
    // logContents 상태 업데이트
    setLogContents((prevContents) =>
      prevContents.map((log) => {
        // speakerId와 매핑된 cuserId가 동일한 경우 이름 업데이트
        const speaker = speakers.find((s) => s.speakerId === speakerId);
        if (speaker && log.cuserId === speaker.cuserId) {
          return {
            ...log,
            name: newName || speaker.speakerId, // 새 이름이 없으면 speakerId를 사용
          };
        }
        return log; // 매칭되지 않는 경우 기존 로그 유지
      })
    );
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
    setLoadingStates((prev) => ({ ...prev, [confId]: true }));
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
    } finally {
      // 로딩 상태 종료
      setLoadingStates((prev) => ({ ...prev, [confId]: false }));
    }
  };

  // 발화자 이름 저장 처리
  const handleSaveSpeakerNames = async () => {
    try {
      // 1. 스피커 정보 생성 (항상 모든 스피커 데이터를 포함)
      const speakerUpdates: SpeakerUpdate[] = speakers.map((speaker) => ({
        speakerId: speaker.speakerId,
        cuserId: speaker.cuserId,
        name: speakerNames[speaker.speakerId] || "", // 이름이 없으면 빈 문자열로 처리
      }));
  
      // 2. 변경된 로그 데이터 생성
      const logUpdates = changedLogs.map((log) => ({
        logId: log.logId,
        cuserId: log.cuserId,
      }));
  
      // 3. 스피커 업데이트 API 호출
      const speakerResponse = await fetch("/api/meetings/speakers/update", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          confId, // 회의 ID
          speakers: speakerUpdates,
        }),
      });
  
      const speakerResult = await speakerResponse.json();
      if (!speakerResult.success) {
        throw new Error("스피커 업데이트 실패");
      }
  
      // 4. 로그 업데이트 API 호출 (변경된 로그가 있는 경우만)
      if (logUpdates.length > 0) {
        const logsResponse = await fetch("/api/meetings/logs/update", {
          method: "POST",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify({
            confId, // 회의 ID
            logs: logUpdates,
          }),
        });
  
        const logsResult = await logsResponse.json();
        if (!logsResult.success) {
          throw new Error("로그 업데이트 실패");
        }
      }
  
      // 저장 성공 알림
      alert("발화자 정보 및 로그 변경 사항이 저장되었습니다.");
  
      // 상태 초기화
      setChangedLogs([]);
    } catch (error) {
      console.error("저장 오류:", error);
      alert("저장에 실패했습니다.");
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
              <div key={speaker.speakerId} className={`${styles.speakerItem} ${speaker.speakerId === 'UNKNOWN' ? styles.unknownItem : ''}`}>
                <span className={styles.originalSpeaker}>{speaker.speakerId}</span>
                {speaker.speakerId !== 'UNKNOWN' && (
                  <input
                    type="text"
                    className={styles.speakerChange}
                    defaultValue={speaker.name}
                    onChange={(e) => handleSpeakerNameChange(speaker.speakerId, e.target.value)}
                    placeholder="발화자 이름 입력"
                  />
                )}
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
            const speaker = displaySpeakers.find((s) => s.cuserId === content.cuserId);
            const currentLog = logContents.find((log) => log.logId === content.logId);
            return (
              <div key={content.logId} className={styles.sttItem}>
                
                {/* 드롭다운: 발화자 선택 */}
                <select
                  className={styles.speakerInput}
                  value={currentLog?.cuserId || ""}
                  onChange={(e) => {
                    const selectedCuserId = parseInt(e.target.value, 10);
                    const selectedSpeaker = speakers.find((s) => s.cuserId === selectedCuserId);

                    setLogContents((prevContents) =>
                      prevContents.map((item) =>
                        item.logId === content.logId
                          ? {
                              ...item,
                              cuserId: selectedCuserId || null,
                              name: selectedSpeaker ? selectedSpeaker.name || selectedSpeaker.speakerId : "",
                            }
                          : item
                      )
                    );

                    setChangedLogs((prevChanges) => {
                      const updatedChanges = [...prevChanges];
                      const existingIndex = updatedChanges.findIndex((log) => log.logId === content.logId);

                      if (existingIndex >= 0) {
                        updatedChanges[existingIndex].cuserId = selectedCuserId || null;
                      } else {
                        updatedChanges.push({
                          logId: content.logId,
                          cuserId: selectedCuserId || null,
                        });
                      }

                      return updatedChanges;
                    });
                  }}
                >
                  {speakers
                    .sort((a, b) => {
                      // UNKNOWN은 항상 마지막으로
                      if (a.speakerId === 'UNKNOWN') return 1;
                      if (b.speakerId === 'UNKNOWN') return -1;
                      // 나머지는 기존처럼 알파벳 순
                      return a.speakerId.localeCompare(b.speakerId);
                    })
                    .map((s) => (
                      <option key={s.speakerId} value={s.cuserId || ""}>
                        {speakerNames[s.speakerId] || s.speakerId}
                      </option>
                    ))}
                </select>

                {/* 대화 내용 */}
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
