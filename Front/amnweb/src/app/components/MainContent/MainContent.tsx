"use client";

import { useService } from "@/app/context/ServiceContext";
import { useChat } from "@/app/context/ChatContext";
import styles from "./MainContent.module.css";
import AdminDashboard from "../AdminDashboard/AdminDashboard";
import MeetingList from "../MeetingList/MeetingList";
import InvestmentReport from "../InvestmentReport/InvestmentReport";
import ChatContainer from "../Chat/ChatContainer";
import { useEffect } from "react";

export default function MainContent() {
  const { currentService, pageState, setPageState } = useService();
  const { deactivateChat } = useChat();

  // 채팅이 아닌 다른 페이지로 이동할 때 채팅 상태 초기화
  useEffect(() => {
    if (pageState === 'admin' || currentService === 'meeting-minutes' || currentService === 'investment-report') {
      deactivateChat();
    }
  }, [pageState, currentService]);

  const handleAdminButtonClick = () => {
    setPageState('admin');
    deactivateChat();
  };

  return (
    <div className={styles.container}>
      {pageState === "admin" ? (
        <AdminDashboard />
      ) : currentService === "meeting-minutes" ? (
        <MeetingList />
      ) : currentService === "investment-report" ? (
        <InvestmentReport />
      ) : (
        <ChatContainer />
      )}
    </div>
  );
}