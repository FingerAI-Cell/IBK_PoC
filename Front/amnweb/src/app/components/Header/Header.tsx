"use client";

import styles from "./Header.module.css";
import { useService } from "../../context/ServiceContext";
import { HiMenuAlt2 } from "react-icons/hi";
import { useChat } from "../../context/ChatContext";

export default function Header() {
  const { handleMyServices, setPageState, toggleSidebar, isSidebarOpen } = useService();
  const { deactivateChat } = useChat();

  const handleToggle = () => {
    console.log('Toggling sidebar:', !isSidebarOpen); // 디버깅용
    toggleSidebar();
  };

  const handleAdminButtonClick = () => {
    console.log('Navigating to admin page');
    setPageState('admin');
    deactivateChat();
  };

  const handleHeaderButtonClick = () => {
    handleMyServices();
    deactivateChat();
  }

  return (
    <header className={styles.header}>
      <button 
        className={styles.sidebarToggle}
        onClick={handleToggle}
        aria-label={isSidebarOpen ? "Close sidebar" : "Open sidebar"}
      >
        <HiMenuAlt2 size={24} />
      </button>
      
      <div className={`
        ${styles.titleContainer}
        xl:justify-start
      `}>
        <button 
          onClick={handleHeaderButtonClick}
          className={styles.titleButton}
        >
          My Services
        </button>
      </div>
      
      <button 
        className={styles.myPageButton}
        onClick={handleAdminButtonClick}
      >
        관리자
      </button>
    </header>
  );
}
