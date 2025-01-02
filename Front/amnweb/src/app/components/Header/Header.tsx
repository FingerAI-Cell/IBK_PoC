"use client";

import styles from "./Header.module.css";
import { useService } from "../../context/ServiceContext";

export default function Header() {
  const { handleMyServices, setPageState } = useService();

  return (
    <header className={styles.header}>
      <button 
        onClick={handleMyServices}
        className={styles.titleButton}
      >
        My Services
      </button>
      <button 
        className={styles.myPageButton}
        onClick={() => setPageState('admin')}
      >
        관리자
      </button>
    </header>
  );
}
