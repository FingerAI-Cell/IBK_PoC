"use client";

import styles from "./Header.module.css";
import { useService } from "../../context/ServiceContext";

export default function Header() {
  const { handleMyServices } = useService();

  return (
    <header className={styles.header}>
      <button 
        onClick={handleMyServices}
        className={styles.titleButton}
      >
        My Services
      </button>
      <button className={styles.myPageButton}>My Page</button>
    </header>
  );
}
