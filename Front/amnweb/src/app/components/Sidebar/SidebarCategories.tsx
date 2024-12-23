"use client";

import styles from "./SidebarCategories.module.css";

interface SidebarCategoriesProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function SidebarCategories({
  currentService,
  selectService,
}: SidebarCategoriesProps) {
  const categories = [
    { id: "general-chat", name: "일반 채팅" },
    { id: "overseas-loan", name: "해외주식" },
    { id: "financial-statements", name: "재무제표" },
    { id: "branch-manual", name: "영업점 매뉴얼" },
    { id: "meeting-minutes", name: "회의록" },
    { id: "investment-report", name: "개인투자정보" },
  ];

  return (
    <ul className={styles.categories}>
      {categories.map((category) => (
        <li
          key={category.id}
          className={`${styles.categoryItem} ${
            currentService === category.id ? styles.active : ""
          }`}
          onClick={() => selectService(category.id)}
        >
          {category.name}
        </li>
      ))}
    </ul>
  );
}
