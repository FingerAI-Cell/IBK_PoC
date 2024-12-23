"use client";

import styles from "./Sidebar.module.css";
import SidebarCategories from "./SidebarCategories";
import { RecentHistory } from "./RecentHistory";

interface SidebarProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function Sidebar({
  currentService,
  selectService,
}: SidebarProps) {
  return (
    <aside className={styles.sidebar}>
      <div className={styles.categories}>
        <h2 className={styles.categoryTitle}>카테고리</h2>
        <SidebarCategories
          currentService={currentService}
          selectService={selectService}
        />
      </div>

      <div className={styles.recentHistory}>
        <RecentHistory />
      </div>
    </aside>
  );
}
