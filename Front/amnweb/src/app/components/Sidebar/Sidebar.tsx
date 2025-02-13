"use client";

import styles from "./Sidebar.module.css";
import SidebarCategories from "./SidebarCategories";
import { useService } from "../../context/ServiceContext";
import Image from 'next/image';

interface SidebarProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function Sidebar({
  currentService,
  selectService,
}: SidebarProps) {
  const { isSidebarOpen, handleMyServices, toggleSidebar } = useService();
  
  console.log('Sidebar - isSidebarOpen:', isSidebarOpen);

  return (
    <>
      <button 
        className={`
          ${styles.hamburgerButton} 
          hidden xl:hidden
        `}
        onClick={toggleSidebar}
        aria-label="메뉴 열기/닫기"
      >
        <Image src="/img/hamburger.svg" alt="" width={24} height={24} />
      </button>
      <aside className={`
        ${styles.sidebar}
        ${!isSidebarOpen ? styles.hidden : ''}
      `}>
        <div className={styles.logoContainer}>
          <button onClick={handleMyServices}>
            <Image src="/img/ibk_logo.png" alt="" width={138} height={34} />
          </button>
          <button 
            className={styles.toggleButton}
            onClick={toggleSidebar}
            aria-label="사이드바 닫기"
          >
            <svg className="w-6 h-6" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h16" />
            </svg>
          </button>
        </div>
        <div className={styles.categories}>
          <h2 className="hidden">카테고리</h2>
          <SidebarCategories
            currentService={currentService}
            selectService={selectService}
          />
        </div>
      </aside>
    </>
  );
}
