import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './SummaryModal.module.css';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

interface Participant {
  id: string;
  name: string | null;
}

interface SummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  date: string;
  participants: Participant[];
  content: string;
  confId?: number;
}

export default function SummaryModal({ 
  isOpen, 
  onClose, 
  title, 
  date, 
  participants=[], 
  content
}: SummaryModalProps) {
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    return () => setMounted(false);
  }, []);

  if (!isOpen || !mounted) return null;
  
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
      // JSON 데이터를 파싱
      const parsedTopics = JSON.parse(content).topics;
  
      // topics 배열을 순회하여 렌더링
      return parsedTopics.map((topicEntry: any, index: number) => (
        <div key={`topic-${index}`} className={styles.topicSection}>
          <h4 className={styles.topicTitle}>{topicEntry.topic || '제목 없음'}</h4>
          <div className={styles.speakerSection}>
            {topicEntry.speakers
              .filter((speaker: any) => speaker.content && speaker.content.trim() !== '') // content가 비어있지 않은 경우만
              .map((speaker: any, speakerIndex: number) => (
                <div key={`speaker-${speakerIndex}`} className={styles.speakerDetails}>
                  <span className={styles.speakerName}>{speaker.name}</span>
                  <span className={styles.speakerContent}>{speaker.content}</span>
                </div>
              ))}
          </div>
        </div>
      ));
    } catch (error) {
      console.error('Summary 데이터 처리 오류:', error);
      return <p>회의 데이터를 처리할 수 없습니다.</p>;
    }
  };
  
  
  const modalContent = (
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
                <span>{participants.map((p) => p.name || p.id).join(', ')}</span>
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

  // document가 있는지 확인 (SSR 대비)
  if (typeof document === 'undefined') return null;

  const modalRoot = document.getElementById('modal-root');
  if (!modalRoot) return null;

  return createPortal(modalContent, modalRoot);
}
