"use client";

import styles from "./Sidebar.module.css";
import SidebarCategories from "./SidebarCategories";
import { useService } from "../../context/ServiceContext";

interface SidebarProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function Sidebar({
  currentService,
  selectService,
}: SidebarProps) {
  const { isSidebarOpen } = useService();

  return (
    <aside className={`
      ${styles.sidebar}
      ${!isSidebarOpen ? styles.hidden : ''}
    `}>
      <div className={styles.categories}>
        <h2 className={styles.categoryTitle}>카테고리</h2>
        <SidebarCategories
          currentService={currentService}
          selectService={selectService}
        />
      </div>
    </aside>
  );
}
