import styles from './AlertModal.module.css';

interface AlertModalProps {
  isOpen: boolean;
  onClose: () => void;
  message: string;
}

export default function AlertModal({ isOpen, onClose, message }: AlertModalProps) {
  if (!isOpen) return null;

  return (
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
} 