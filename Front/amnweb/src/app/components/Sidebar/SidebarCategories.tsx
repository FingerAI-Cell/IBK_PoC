"use client";

import styles from "./SidebarCategories.module.css";
import { BsChatDots } from "react-icons/bs";
import { BiWorld, BiFile, BiStore, BiNotepad, BiUser, BiCog } from "react-icons/bi";
import { useService } from "../../context/ServiceContext";
import { useChat } from "@/app/context/ChatContext";

interface SidebarCategoriesProps {
  currentService: string;
  selectService: (service: string) => void;
}

export default function SidebarCategories({
  currentService,
  selectService,
}: SidebarCategoriesProps) {
  const { pageState, setPageState } = useService();
  const { deactivateChat } = useChat();
  
  const categories = [
    { id: "general-chat", name: "일반 채팅", icon: <BsChatDots /> },
    { id: "overseas-loan", name: "해외주식", icon: <BiWorld /> },
    { id: "financial-statements", name: "재무제표", icon: <BiFile /> },
    { id: "branch-manual", name: "영업점 매뉴얼", icon: <BiStore /> },
    { id: "meeting-minutes", name: "회의록", icon: <BiNotepad /> },
    { id: "investment-report", name: "개인투자정보", icon: <BiUser /> },
    { 
      id: "admin", 
      name: "Admin", 
      icon: <BiCog />,
      onClick: () => {
        setPageState('admin');
        deactivateChat();
      }
    },
  ];

  const handleServiceSelect = (category: typeof categories[0]) => {
    if (category.onClick) {
      category.onClick();
    } else {
      if (category.id === currentService) {
        setPageState('select');
        deactivateChat();
      } else {
        setPageState('select');
        selectService(category.id);
        deactivateChat();
      }
    }
  };

  return (
    <nav className={styles.nav}>
      <ul className={styles.categories}>
        {categories.filter(cat => cat.id !== 'admin').map((category) => (
          <li
            key={category.id}
            className={`${styles.categoryItem} ${
              currentService === category.id && pageState !== 'admin'
                ? styles.active 
                : ""
            }`}
            onClick={() => handleServiceSelect(category)}
          >
            <span className={styles.icon}>{category.icon}</span>
            {category.name}
          </li>
        ))}
      </ul>
      
      {/* Admin 메뉴 별도 렌더링 */}
      <div className={styles.adminContainer}>
        {categories.filter(cat => cat.id === 'admin').map((category) => (
          <li
            key={category.id}
            className={`${styles.categoryItem} ${
              pageState === 'admin' ? styles.active : ""
            }`}
            onClick={() => handleServiceSelect(category)}
          >
            <span className={styles.icon}>{category.icon}</span>
            {category.name}
          </li>
        ))}
      </div>
    </nav>
  );
}
