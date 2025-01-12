"use client";

import styles from "./Header.module.css";
import { useService } from "../../context/ServiceContext";
import { HiMenuAlt2 } from "react-icons/hi";

export default function Header() {
  const { handleMyServices, setPageState, toggleSidebar } = useService();

  return (
    <header className={styles.header}>
      <button 
        className={styles.sidebarToggle}
        onClick={toggleSidebar}
      >
        <HiMenuAlt2 size={24} />
      </button>
      
      <div className={`
        ${styles.titleContainer}
        xl:justify-start // 넓은 화면에서는 왼쪽 정렬
      `}>
        <button 
          onClick={handleMyServices}
          className={styles.titleButton}
        >
          My Services
        </button>
      </div>
      
      <button 
        className={styles.myPageButton}
        onClick={() => setPageState('admin')}
      >
        관리자
      </button>
    </header>
  );
}
