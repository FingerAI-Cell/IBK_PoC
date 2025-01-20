import { createPortal } from 'react-dom';
import styles from './AlertModal.module.css';

interface AlertModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: string;
}

export default function AlertModal({ isOpen, onClose, message }: AlertModalProps) {
  if (!isOpen) return null;

  const modalContent = (
    <div className={styles.alertOverlay}>
      <div className={styles.alertModal}>
        <div className={styles.alertContent}>
          <p className={styles.alertMessage}>{message}</p>
          <button className={styles.alertButton} onClick={onClose}>
            확인
          </button>
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