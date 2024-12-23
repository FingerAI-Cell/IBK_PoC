"use client";

import styles from "./Header.module.css";
import { useService } from "../../context/ServiceContext";
import { useRouter } from "next/navigation";
import { useEffect } from 'react';

export default function Header() {
  const { currentService, setCurrentService } = useService();
  const router = useRouter();

  const handleTitleClick = () => {
    setCurrentService(currentService === "general-chat" ? "temp" : "general-chat");
  };

  useEffect(() => {
    if (currentService === "general-chat") {
      router.push('/');
    }
  }, [currentService, router]);

  return (
    <header className={styles.header}>
      <button 
        onClick={handleTitleClick}
        className={styles.titleButton}
      >
        My Services
      </button>
      <button className={styles.myPageButton}>My Page</button>
    </header>
  );
}
