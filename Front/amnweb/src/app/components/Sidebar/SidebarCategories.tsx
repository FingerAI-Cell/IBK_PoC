"use client";

import styles from "./SidebarCategories.module.css";
import { BsChatDots } from "react-icons/bs";
import { BiWorld, BiFile, BiStore, BiNotepad, BiUser } from "react-icons/bi";
import { useService } from "../../context/ServiceContext";

interface SidebarCategoriesProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function SidebarCategories({
  currentService,
  selectService,
}: SidebarCategoriesProps) {
  const { pageState, setPageState } = useService();
  
  const handleServiceSelect = (serviceId: string) => {
    console.log('Category Clicked:', {
      clicked: serviceId,
      current: currentService,
      pageState
    });
    
    if (serviceId === currentService) {
      setPageState('select');
    } else {
      setPageState('select');
      selectService(serviceId);
    }
  };

  const categories = [
    { id: "general-chat", name: "일반 채팅", icon: <BsChatDots /> },
    { id: "overseas-loan", name: "해외주식", icon: <BiWorld /> },
    { id: "financial-statements", name: "재무제표", icon: <BiFile /> },
    { id: "branch-manual", name: "영업점 매뉴얼", icon: <BiStore /> },
    { id: "meeting-minutes", name: "회의록", icon: <BiNotepad /> },
    { id: "investment-report", name: "개인투자정보", icon: <BiUser /> },
  ];

  return (
    <ul className={styles.categories}>
      {categories.map((category) => (
        <li
          key={category.id}
          className={`${styles.categoryItem} ${
            currentService === category.id ? styles.active : ""
          }`}
          onClick={() => handleServiceSelect(category.id)}
        >
          <span className={styles.icon}>{category.icon}</span>
          {category.name}
        </li>
      ))}
    </ul>
  );
}
