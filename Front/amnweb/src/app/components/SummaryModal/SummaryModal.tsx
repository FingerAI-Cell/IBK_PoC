import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './SummaryModal.module.css';
import html2canvas from 'html2canvas';
import jsPDF from 'jspdf';

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
      const parsedData = JSON.parse(content);
      const cleanedOutput = parsedData.output.replace(/```json\n|\n```/g, '');
      const summaryContent = JSON.parse(cleanedOutput);

      // 파싱된 데이터를 React 컴포넌트로 변환
      return summaryContent.map((entry: any, index: number) => (
        <div key={`topic-${index}`} className={styles.topicSection}>
          <h4 className={styles.topicTitle}>{entry.topic}</h4>
          <div className={styles.speakerSection}>
            {Object.entries(entry)
              .filter(([key]) => key.startsWith('speaker_'))
              .map(([speaker, value]: [string, any], i: number) => {
                // speaker의 내용을 추출하는 로직
                let speakerContent = '';
                if (typeof value === 'string') {
                  // 문자열인 경우 그대로 사용
                  speakerContent = value;
                } else if (typeof value === 'object') {
                  // 객체인 경우 text나 details 필드 사용
                  speakerContent = value.text || value.details || '';
                }

                // 스피커 이름 또는 ID 표시 로직
                const speakerName = (() => {
                  const speakerId = speaker.replace('speaker_', '').toUpperCase();
                  const participant = participants.find(p => 
                    p.toLowerCase().includes(speakerId.toLowerCase())
                  );
                  return participant || `SPEAKER_${speakerId}`;
                })();

                speakerContent = speakerContent.replace(/SPEAKER_\d+:\s*/g, '');
                // 개행문자(\n)를 <br/>로 변환
                const formattedContent = speakerContent.split('\\n').map((line, lineIndex) => (
                  <span key={lineIndex}>
                    {line}
                    {lineIndex < speakerContent.split('\\n').length - 1 && <br />}
                  </span>
                ));

                return (
                  <p key={`${speaker}-${i}`} className={styles.speakerDetails}>
                    <span className={styles.speaker}>{speakerName}:</span> 
                    {formattedContent}
                  </p>
                );
              })}
          </div>
        </div>
      ));
    } catch (error) {
      console.error('문자열 처리 오류:', error);
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

  // document가 있는지 확인 (SSR 대비)
  if (typeof document === 'undefined') return null;

  const modalRoot = document.getElementById('modal-root');
  if (!modalRoot) return null;

  return createPortal(modalContent, modalRoot);
}
