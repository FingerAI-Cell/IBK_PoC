// import { useState } from 'react';
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
}

export default function SummaryModal({ isOpen, onClose, title, date, participants, content }: SummaryModalProps) {
  if (!isOpen) return null;

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
              <div className={styles.contentText}>
                {content}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 