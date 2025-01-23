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
      // 전체 모달 컨텐츠와 헤더를 포함할 임시 div 생성
      const tempDiv = document.createElement('div');
      tempDiv.style.background = 'white';
      tempDiv.style.padding = '20px';
      tempDiv.style.width = '800px';  // PDF 너비에 맞춤

      // 헤더 복제 (버튼 제외)
      const headerDiv = document.createElement('div');
      headerDiv.style.marginBottom = '20px';
      const titleH2 = document.createElement('h2');
      titleH2.style.fontSize = '24px';
      titleH2.style.fontWeight = 'bold';
      titleH2.style.margin = '0';
      titleH2.textContent = title;
      headerDiv.appendChild(titleH2);
      tempDiv.appendChild(headerDiv);

      // 컨텐츠 복제
      const element = document.getElementById('modal-content');
      if (!element) return;
      const contentClone = element.cloneNode(true) as HTMLElement;
      tempDiv.appendChild(contentClone);

      // 임시 div를 document에 추가 (화면 밖)
      tempDiv.style.position = 'absolute';
      tempDiv.style.left = '-9999px';
      document.body.appendChild(tempDiv);

      // PDF 생성
      const canvas = await html2canvas(tempDiv, {
        logging: false,
        useCORS: true,
        scale: 2,
        windowWidth: tempDiv.scrollWidth,
        windowHeight: tempDiv.scrollHeight,
        width: tempDiv.scrollWidth,
        height: tempDiv.scrollHeight
      });

      // 임시 div 제거
      document.body.removeChild(tempDiv);

      const imgData = canvas.toDataURL('image/png');
      
      const pdf = new jsPDF({
        orientation: 'p',
        unit: 'mm',
        format: 'a4'
      });

      const pdfWidth = pdf.internal.pageSize.getWidth();
      const pdfHeight = pdf.internal.pageSize.getHeight();
      
      const imgWidth = canvas.width;
      const imgHeight = canvas.height;
      
      const ratio = Math.min(pdfWidth / imgWidth, pdfHeight / imgHeight);
      
      const imgX = (pdfWidth - imgWidth * ratio) / 2;
      let imgY = 0;
      
      let remainingHeight = imgHeight;
      let currentPosition = 0;
      
      while (remainingHeight > 0) {
        if (currentPosition > 0) {
          pdf.addPage();
        }
        
        pdf.addImage(
          imgData,
          'PNG',
          imgX,
          imgY - (currentPosition * pdfHeight),
          imgWidth * ratio,
          imgHeight * ratio
        );
        
        remainingHeight -= pdfHeight / ratio;
        currentPosition++;
      }

      pdf.save(`${title}_회의록.pdf`);
    } catch (error) {
      console.error('PDF 생성 중 오류:', error);
    }
  };
  
  const renderContent = () => {
    try {
      const parsedTopics = JSON.parse(content).topics;
  
      return parsedTopics.map((topicEntry: any, index: number) => (
        <div key={`topic-${index}`} className={styles.topicSection}>
          <h4 className={styles.topicTitle}>{topicEntry.topic || '제목 없음'}</h4>
          <div className={styles.speakerSection}>
            {topicEntry.speakers
              .filter((speaker: any) => speaker.content && speaker.content.trim() !== '')
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
        <div id="modal-content" className={styles.modalContent}>
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

  if (typeof document === 'undefined') return null;

  const modalRoot = document.getElementById('modal-root');
  if (!modalRoot) return null;

  return createPortal(modalContent, modalRoot);
}
