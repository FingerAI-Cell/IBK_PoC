import styles from './SummaryModal.module.css';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';
import { meetings } from '@/data/meetings';

interface SummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  date: string;
  participants: string[];
  content: string;
  confId?: number;
}

export default function SummaryModal({ 
  isOpen, 
  onClose, 
  title, 
  date, 
  participants, 
  content,
  confId 
}: SummaryModalProps) {
  if (!isOpen) return null;
  
  // API 실패시 meetings.ts에서 해당 회의 데이터를 가져오는 로직
  const getFallbackData = (confId: number) => {
    const meeting = meetings.find(m => m.confId === confId);
    if (!meeting) return null;

    return {
      title: meeting.title,
      date: meeting.startTime,
      participants: meeting.participants,
      content: meeting.summary
    };
  };

  // content가 비어있거나 유효하지 않은 JSON 형식일 때 fallback 데이터 사용
  const fallbackData = confId ? getFallbackData(confId) : null;
  
  const displayContent = (() => {
    try {
      JSON.parse(content);
      return content;
    } catch {
      return fallbackData?.content || "";
    }
  })();
  
  const displayParticipants = participants?.length > 0 
    ? participants 
    : fallbackData?.participants || [];
  
  const displayTitle = title || fallbackData?.title || "";
  const displayDate = date || fallbackData?.date || "";
  
  const handleDownload = async () => {
    try {
      const element = document.getElementById('summaryContent');
      if (!element) return;

      const canvas = await html2canvas(element);
      const imgData = canvas.toDataURL('image/png');
      
      const pdf = new jsPDF('p', 'mm', 'a4');
      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = (canvas.height * pdfWidth) / canvas.width;
      
      pdf.addImage(imgData, 'PNG', 0, 0, pdfWidth, pdfHeight);
      pdf.save(`${title}_회의록.pdf`);
    } catch (error) {
      console.error('PDF 생성 중 오류:', error);
    }
  };
  
  const renderContent = () => {
    try {
      // 1. "topic"과 각 topic에 포함된 "details" 또는 직접 텍스트 추출 정규식
      const topicRegex = /"topic":\s*"([^"]+)"/g;
      const topicBlockRegex = /{[^}]*"topic":\s*"([^"]+)"[^}]*}/g;
      const speakerDetailsRegex = /"(\w+)":\s*(?:{[^}]*"details":\s*"([^"]+)"|"(.*?)")/g;
  
      // 2. Topic 추출
      const topics = [...displayContent.matchAll(topicRegex)].map((match) => match[1]);
  
      // 3. Topic 블록 단위로 세분화하여 Speaker 및 Details 또는 Text 추출
      const topicBlocks = [...displayContent.matchAll(topicBlockRegex)].map((match) => match[0]);
  
      const topicDetailsMap = topicBlocks.map((block, index) => {
        const details = [...block.matchAll(speakerDetailsRegex)].map((match) => ({
          speaker: match[1],
          details: match[2] || match[3], // "details" 값이 없으면 텍스트 값 사용
        }));
        return {
          topic: topics[index],
          details,
        };
      });
  
      // 4. 렌더링 (details에서 topic 제거)
      return topicDetailsMap.map((entry, index) => (
        <div key={`topic-${index}`} className={styles.topicSection}>
          {/* topic 제목만 표시 */}
          <h4 className={styles.topicTitle}>{entry.topic}</h4>
          {/* topic과 관련된 details만 표시 */}
          {entry.details.map((d, i) => (
            d.details && !d.details.includes(entry.topic) ? ( // topic 중복 제거
              <p key={`details-${index}-${i}`} className={styles.speakerDetails}>
                {d.details}
              </p>
            ) : null
          ))}
        </div>
      ));
    } catch (error) {
      console.error('문자열 처리 오류:', error);
      return <p>회의 데이터를 처리할 수 없습니다.</p>;
    }
  };
  
  return (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <h2 className={styles.modalTitle}>{title}</h2>
          <div className={styles.headerButtons}>
            <button 
              className={styles.downloadButton}
              onClick={handleDownload}
            >
              다운로드
            </button>
            <button 
              className={styles.closeButton}
              onClick={onClose}
            >
              ✕
            </button>
          </div>
        </div>
        <div id="summaryContent" className={styles.summaryContent}>
          <div className={styles.contentWrapper}>
            <div className={styles.meetingInfo}>
              <div className={styles.infoItem}>
                <span className={styles.label}>회의 일시:</span>
                <span>{date}</span>
              </div>
              <div className={styles.infoItem}>
                <span className={styles.label}>참석자:</span>
                <span>{participants.join(', ')}</span>
              </div>
            </div>
            <div className={styles.divider} />
            <div className={styles.mainContent}>
              <h3 className={styles.sectionTitle}>회의 내용</h3>
              <div className={styles.contentText}>{renderContent()}</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
