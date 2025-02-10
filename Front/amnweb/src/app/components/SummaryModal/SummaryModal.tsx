"use client";

import { useState, useEffect } from 'react';
import { createPortal } from 'react-dom';
import styles from './SummaryModal.module.css';
import { Tab } from '@headlessui/react';
import { pdf } from '@react-pdf/renderer';
import { Document, Page, Text, View, StyleSheet } from '@react-pdf/renderer';
import { Font } from '@react-pdf/renderer';
import { vfs } from '../../../fonts/vfs_fonts';  // base64 폰트 정보 불러오기
import { pdfStyles } from './pdfStyles';

interface Participant {
  id: string;
  name: string | null;
}

interface OverallSummaryData {
  topics: Array<{
    topic: string;    // 예: "주요 논의 사항", "결정 사항"
    content: string;  // 해당 토픽의 내용
  }>;
}

// 기존 인터페이스도 명시적으로 정의
interface SpeakerSummaryData {
  topics: Array<{
    topic: string;
    speakers: Array<{
      name: string;
      content: string;
    }>;
  }>;
}

// API 응답 구조 정의
interface SummaryApiResponse {
  overall: {
    topics: Array<{
      topic: string;
      content: string;
    }>;
  };
  speaker: {
    topics: Array<{
      topic: string;
      speakers: Array<{
        name: string;
        content: string;
      }>;
    }>;
  };
}

interface SummaryModalProps {
  isOpen: boolean;
  onClose: () => void;
  title: string;
  date: string;
  participants: Participant[];
  content: string;
  confId?: number;
}

export default function SummaryModal({ 
  isOpen, 
  onClose, 
  title, 
  date, 
  participants=[], 
  content=''
}: SummaryModalProps) {
  const [mounted, setMounted] = useState(false);
  const [activeTab, setActiveTab] = useState(0);
  const [speakerSummary, setSpeakerSummary] = useState<SpeakerSummaryData | null>(null);
  const [overallSummary, setOverallSummary] = useState<OverallSummaryData | null>(null);

  useEffect(() => {
    setMounted(true);
    if (content) {
      try {
        const parsedData: SummaryApiResponse = JSON.parse(content);
        
        // 전체 요약 데이터 설정
        setOverallSummary(parsedData.overall);
        
        // 화자별 요약 데이터 설정
        setSpeakerSummary(parsedData.speaker);
      } catch (error) {
        console.error('Summary parsing error:', error);
        setOverallSummary(null);
        setSpeakerSummary(null);
      }
    }
    return () => setMounted(false);
  }, [content]);

  if (!isOpen || !mounted) return null;

  const renderSpeakerSummary = () => {
    if (!content) return <p>데이터가 없습니다.</p>;
    
    try {
      const parsedTopics = JSON.parse(content).topics;
  
      return parsedTopics.map((topicEntry: any, index: number) => (
        <div key={`topic-${index}`} className={styles.topicSection}>
          <h4 className={styles.topicTitle}>{topicEntry.topic || '제목 없음'}</h4>
          <div className={styles.speakerSection}>
            {topicEntry.speakers
              .filter((speaker: any) => speaker.content && speaker.content.trim() !== '')
              .map((speaker: any, speakerIndex: number) => (
                <div key={`speaker-${speakerIndex}`} className={styles.speakerDetails}>
                  <span className={styles.speakerName}>{speaker.name}</span>
                  <span className={styles.speakerContent}>{speaker.content}</span>
                </div>
              ))}
          </div>
        </div>
      ));
    } catch (error) {
      console.error('Summary 데이터 처리 오류:', error);
      return <p>회의 데이터를 처리할 수 없습니다.</p>;
    }
  };

  const renderOverallSummary = () => {
    if (!overallSummary) return <p>데이터가 없습니다.</p>;

    return (
      <div className={styles.overallSummary}>
        {overallSummary.topics.map((topic, index) => (
          <div key={`topic-${index}`} className={styles.summarySection}>
            <h4 className={styles.summaryTitle}>{topic.topic}</h4>
            <ul className={styles.summaryList}>
              {topic.content.split('\n').map((item, itemIndex) => (
                <li key={itemIndex}>{item}</li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    );
  };

  // PDF 다운로드 핸들러 부분만 수정
  const handleDownload = async () => {
    try {
      const PDFContent = () => (
        <Document>
          <Page size="A4" style={pdfStyles.page}>
            <View style={pdfStyles.header}>
              <Text style={pdfStyles.mainTitle}>{title}</Text>
              <View style={pdfStyles.infoSection}>
                <Text>
                  <Text style={pdfStyles.infoLabel}>회의 일시:</Text>
                  {date}
                </Text>
              </View>
              {activeTab === 1 && (  // 화자별 요약일 때만 참석자 정보 표시
                <View style={pdfStyles.infoSection}>
                  <Text>
                    <Text style={pdfStyles.infoLabel}>참석자:</Text>
                    {participants
                      .filter(p => p.id !== 'UNKNOWN')
                      .map(p => p.name || p.id)
                      .join(', ')}
                  </Text>
                </View>
              )}
            </View>

            <View style={pdfStyles.contentSection}>
              {activeTab === 0 ? (
                // 전체 요약
                overallSummary?.topics.map((topic, index) => (
                  <View key={index} wrap={false} style={pdfStyles.overallSection}>
                    <Text style={pdfStyles.sectionTitle}>{topic.topic}</Text>
                    {topic.content.split('\n').map((item, itemIndex) => (
                      <Text key={itemIndex} style={pdfStyles.content}>{item}</Text>
                    ))}
                  </View>
                ))
              ) : (
                // 화자별 요약
                speakerSummary?.topics.map((topic: any, index: number) => (
                  <View key={index} wrap={false} style={pdfStyles.topicSection}>
                    <Text style={pdfStyles.sectionTitle}>{topic.topic || '제목 없음'}</Text>
                    {topic.speakers
                      .filter((speaker: any) => speaker.content && speaker.content.trim() !== '')
                      .map((speaker: any, speakerIndex: number) => (
                        <View key={speakerIndex} wrap={false} style={pdfStyles.speakerSection}>
                          <Text style={pdfStyles.speakerName}>{speaker.name}</Text>
                          <Text style={pdfStyles.content}>{speaker.content}</Text>
                        </View>
                      ))}
                  </View>
                ))
              )}
            </View>
          </Page>
        </Document>
      );

      const blob = await pdf(PDFContent()).toBlob();
      const url = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = `${title}_${activeTab === 0 ? '전체요약' : '화자별요약'}.pdf`;
      link.click();
      URL.revokeObjectURL(url);
    } catch (error) {
      console.error('PDF 생성 오류:', error);
    }
  };

  const modalContent = (
    <div className={styles.modalOverlay}>
      <div className={styles.modal}>
        <div className={styles.modalHeader}>
          <h2 className={styles.modalTitle}>{title}</h2>
          <div className={styles.headerButtons}>
            <button 
              className={styles.downloadButton}
              onClick={handleDownload}
            >
              다운로드
            </button>
            <button className={styles.closeButton} onClick={onClose}>
              ✕
            </button>
          </div>
        </div>

        <Tab.Group onChange={setActiveTab}>
          <div className={styles.tabHeader}>
            <Tab.List className={styles.tabList}>
              <Tab className={({ selected }) => 
                `${styles.tab} ${selected ? styles.tabSelected : ''}`
              }>
                전체 요약
              </Tab>
              <Tab className={({ selected }) => 
                `${styles.tab} ${selected ? styles.tabSelected : ''}`
              }>
                화자별 요약
              </Tab>
            </Tab.List>
          </div>

          <div className={styles.contentWrapper}>
            <div className={styles.meetingInfo}>
              <div className={styles.infoItem}>
                <span className={styles.label}>회의 일시:</span>
                <span>{date}</span>
              </div>
              {activeTab === 1 && (  // 참석자 정보만 화자별 요약(activeTab이 1)일 때 표시
                <div className={styles.infoItem}>
                  <span className={styles.label}>참석자:</span>
                  <span>
                    {participants
                      .filter(p => p.id !== 'UNKNOWN')
                      .map((p) => p.name || p.id)
                      .join(', ')}
                  </span>
                </div>
              )}
            </div>
            
            <Tab.Panels className={styles.tabPanels}>
              <Tab.Panel>
                {renderOverallSummary()}
              </Tab.Panel>
              <Tab.Panel>
                <div className={styles.mainContent}>
                  <div className={styles.contentText}>
                    {renderSpeakerSummary()}
                  </div>
                </div>
              </Tab.Panel>
            </Tab.Panels>
          </div>
        </Tab.Group>
      </div>
    </div>
  );

  if (typeof document === 'undefined') return null;

  const modalRoot = document.getElementById('modal-root');
  if (!modalRoot) return null;

  return createPortal(modalContent, modalRoot);
}
