"use client";

import styles from "./InvestmentReport.module.css";
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { useService } from "@/app/context/ServiceContext";

ChartJS.register(ArcElement, Tooltip, Legend);

interface InvestmentDetail {
  name: string;
  value: string;
  amount: string;
  dayBefore: string;
}

interface InvestmentReportType {
  totalAmount: number;
  investedAmount: number;
  portfolioData: {
    stocks: number;
    bondsFunds: number;
    derivatives: number;
    trust: number;
    others: number;
  };
  investmentDetails: InvestmentDetail[];
  date: string;
  content: string;
}

const sampleInvestmentReport: InvestmentReportType = {
  totalAmount: 100000000,
  investedAmount: 80000000,
  portfolioData: {
    stocks: 40000000,
    bondsFunds: 20000000,
    derivatives: 8000000,
    trust: 8000000,
    others: 4000000
  },
  investmentDetails: [
    { name: '삼성전자', value: '77,800원', amount: '+1,000', dayBefore: '+1.3%' },
    { name: 'KODEX 200', value: '35,565원', amount: '-235', dayBefore: '-0.7%' }
  ],
  date: '2024-03-20',
  content: '투자 현황 보고서'
};

export default function InvestmentReport() {
  const { reportDate, reportData } = useService();
  const currentData = reportData || sampleInvestmentReport;

  const chartData = {
    labels: ['주식', '채권/펀드', '선물/옵션', '신탁', '기타'],
    datasets: [
      {
        data: [
          currentData.portfolioData.stocks,
          currentData.portfolioData.bondsFunds,
          currentData.portfolioData.derivatives,
          currentData.portfolioData.trust,
          currentData.portfolioData.others
        ],
        backgroundColor: [
          'rgba(255, 99, 132, 0.8)',
          'rgba(54, 162, 235, 0.8)',
          'rgba(75, 192, 192, 0.8)',
          'rgba(153, 102, 255, 0.8)',
          'rgba(201, 203, 207, 0.8)'
        ],
        borderColor: [
          'rgba(255, 99, 132, 1)',
          'rgba(54, 162, 235, 1)',
          'rgba(75, 192, 192, 1)',
          'rgba(153, 102, 255, 1)',
          'rgba(201, 203, 207, 1)'
        ],
        borderWidth: 1,
      },
    ],
  };

  const chartOptions = {
    plugins: {
      legend: {
        position: 'right' as const,
      }
    },
    cutout: '60%',
    maintainAspectRatio: false
  };

  return (
    <div className={styles.container}>
      <h2 className={styles.title}>
        개인화 투자정보 리포트 {reportDate && `(${reportDate})`}
      </h2>
      
      <div className={styles.content}>
        <div className={styles.leftSection}>
          <div className={styles.summaryBox}>
            <div className={styles.amountItem}>
              <h3>총 평가금액</h3>
              <p>{currentData.totalAmount}</p>
            </div>
            <div className={styles.amountItem}>
              <h3>매입금액</h3>
              <p>{currentData.investedAmount}</p>
            </div>
          </div>

          <div className={styles.portfolioChart}>
            <div className={styles.chartWrapper}>
              <Doughnut data={chartData} options={chartOptions} />
            </div>
          </div>
        </div>

        <div className={styles.rightSection}>
          <div className={styles.categoryTabs}>
            {['전체', '국내', '해외'].map((category) => (
              <button
                key={category}
                className={`${styles.categoryTab} ${category === '전체' ? styles.activeTab : ''}`}
              >
                {category}
              </button>
            ))}
          </div>
          
          <div className={styles.investmentList}>
            <div className={styles.listHeader}>
              <span>종목명</span>
              <span>수익률</span>
              <span>평가금액</span>
              <span>전일대비</span>
            </div>
            {currentData.investmentDetails.map((item, index) => (
              <div key={index} className={styles.investmentItem}>
                <span>{item.name}</span>
                <span className={styles.value}>{item.value}</span>
                <span>{item.amount}</span>
                <span>{item.dayBefore}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
    </div>
  );
} 