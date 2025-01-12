"use client";

import styles from "./InvestmentReport.module.css";
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { useEffect, useState, useCallback } from 'react';

ChartJS.register(ArcElement, Tooltip, Legend);


interface StockData {
  client_name: string;      // 사용자 이름
  stock_name: string;       // 종목명
  ticker: string;          // 종목코드
  market_type: string;     // 시장구분 (KR/US)
  holding_quantity: number; // 보유수량
  acquisition_price: string; // 매입단가
  acquisition_amount: string; // 매입금액
  market_value: string;     // 평가금액
  market_value_previous: string; // 전일 평가금액
  previous_close_price: string; // 전일종가
  trd_dd: string;          // 기준일자
  market_value_diff: string; // 전일대비 등락률
}


interface PortfolioData {
  stocks: number;
  bondsFunds: number;
  derivatives: number;
  trust: number;
  others: number;
}

const BASE_URL = 'http://ec2-54-180-66-126.ap-northeast-2.compute.amazonaws.com:8000';
const API_ENDPOINTS = {
  STOCK: `${BASE_URL}/personal/stock-overview`,
  PORTFOLIO: `${BASE_URL}/personal/portfolio-overview`
};

const formatNumber = (num: number): string => {
  return num.toLocaleString('ko-KR');
};

const getValueColor = (value: number): string => {
  return value > 0 ? 'text-red-500' : value < 0 ? 'text-blue-500' : 'text-gray-500';
};

const formatPercentage = (value: number): string => {
  // 소수점 아래 한자리로 반올림
  const roundedValue = Number(value.toFixed(1));
  return `${roundedValue > 0 ? '+' : ''}${roundedValue}%`;
};

const ValueWithDiff = ({ amount, percentage, dayOverDayDiff }: { 
  amount: number; 
  percentage: number;
  dayOverDayDiff: number;
}) => (
  <div>
    <p className={`text-sm font-bold ${getValueColor(percentage)}`}>
      {formatNumber(amount)}원 ({formatPercentage(percentage)})
    </p>
    <p className={`text-xs ${getValueColor(dayOverDayDiff)}`}>
      전일대비 {formatPercentage(dayOverDayDiff)}
    </p>
  </div>
);

const calculations = {
  // 수익률 계산 (소수점 한자리)
  profitRate: (marketValue: number, acquisitionAmount: number) => {
    return Number(((marketValue - acquisitionAmount) / acquisitionAmount * 100).toFixed(1));
  },
  
  // 총 평가금액
  totalMarketValue: (stocks: StockData[]) => {
    return stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value), 0);
  },
  
  // 총 매입금액
  totalAcquisitionAmount: (stocks: StockData[]) => {
    return stocks.reduce((sum, stock) => sum + parseFloat(stock.acquisition_amount), 0);
  },

  // 전일대비 계산 (소수점 한자리)
  dayOverDayChange: (currentValue: number, previousValue: number) => {
    return Number(((currentValue - previousValue) / previousValue * 100).toFixed(1));
  },

  // 전체 포트폴리오의 전일대비 등락률 계산
  totalDayOverDayDiff: (stocks: StockData[]) => {
    const totalCurrentValue = stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value), 0);
    const totalPreviousValue = stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value_previous), 0);
    return calculations.dayOverDayChange(totalCurrentValue, totalPreviousValue);
  }
};

// ... 기존 imports ...
import { format } from 'date-fns';

const USERS = {
  'C2025BC3F4810': '이지혜',
  'C2025D3E28923': '김범수',
  'C2025F7AC2547': '김나라',
  'C2025E9D76421': '박희준',
  'C2025A4D57392': '황범석'
} as const;

const DEFAULT_USER = 'C2025BC3F4810';

// 날짜 포맷 유틸리티 함수 추가
const formatDisplayDate = (dateStr: string) => {
  // yyyyMMdd를 yy/MM/dd 형식으로 변환
  return dateStr.replace(/(\d{4})(\d{2})(\d{2})/, '$1-$2-$3');
};

export default function InvestmentReport() {
  const today = format(new Date(), 'yyyyMMdd');
  const [selectedUser, setSelectedUser] = useState(DEFAULT_USER);
  const [selectedDate, setSelectedDate] = useState(today);
  const [stockData, setStockData] = useState<StockData[]>([]);
  const [portfolioData, setPortfolioData] = useState<PortfolioData | null>(null);
  const [selectedMarket, setSelectedMarket] = useState<'ALL' | 'KR' | 'US'>('ALL');
  const [isLoading, setIsLoading] = useState(true);

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false
  };

  // fetchData를 useEffect 전에 정의
  const fetchData = useCallback(async () => {
    setIsLoading(true);
    try {
      const requestBody = {
        client_code: selectedUser,
        trd_dd: selectedDate
      };

      // 주식 데이터 요청
      const stockResponse = await fetch(API_ENDPOINTS.STOCK, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(requestBody)
      });

      // 포트폴리오 데이터 요청
      const portfolioResponse = await fetch(API_ENDPOINTS.PORTFOLIO, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          ...requestBody
        })
      });

      if (!stockResponse.ok || !portfolioResponse.ok) 
        throw new Error('API 호출 실패');

      const stockData: StockData[] = await stockResponse.json();
      const portfolioData: PortfolioData = await portfolioResponse.json();

      setStockData(stockData);
      setPortfolioData(portfolioData);
      setIsLoading(false);
    } catch (error) {
      console.error('데이터 로딩 실패:', error);
      setIsLoading(false);
    }
  }, [selectedUser, selectedDate]);

  // useEffect는 fetchData 정의 후에 사용
  useEffect(() => {
    fetchData();
  }, [selectedUser, selectedDate, fetchData]);

  // 차트 데이터 구성
  const chartData = {
    labels: ['주식', '채권/펀드', '선물/옵션', '신탁', '기타'],
    datasets: [{
      data: portfolioData ? [
        portfolioData.stocks,
        portfolioData.bondsFunds,
        portfolioData.derivatives,
        portfolioData.trust,
        portfolioData.others
      ] : [],
      backgroundColor: [
        '#FF6384',
        '#36A2EB',
        '#FFCE56',
        '#4BC0C0',
        '#9966FF'
      ]
    }]
  };

  const getFilteredStocks = (stocks: StockData[]) => {
    if (selectedMarket === 'ALL') return stocks;
    return stocks.filter(stock => stock.market_type === selectedMarket);
  };

  // 날짜 선택 핸들러
  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = new Date(e.target.value);
    // API 요청용 날짜 형식 (yyyyMMdd)
    setSelectedDate(format(date, 'yyyyMMdd'));
  };

  if (isLoading) return <div>로딩 중...</div>;

  return (
    <div className={styles.container}>
      <div className={styles.tabContainer}>
        <div className="flex justify-between items-center mb-4">
          <div className="flex items-center space-x-4">
            <select 
              value={selectedUser}
              onChange={(e) => setSelectedUser(e.target.value)}
              className="p-2 border rounded-md"
            >
              {Object.entries(USERS).map(([code, name]) => (
                <option key={code} value={code}>
                  {name}
                </option>
              ))}
            </select>

            <input
              type="date"
              value={formatDisplayDate(selectedDate)}
              min="2025-01-09"
              max={format(new Date(), 'yyyy-MM-dd')}
              onChange={handleDateChange}
              className="p-2 border rounded-md"
            />
          </div>
        </div>

        <h2 className={styles.title}>
          <div className="flex items-center space-x-2 text-2xl">
            <div className="font-bold text-blue-800">{USERS[selectedUser as keyof typeof USERS]} 고객님</div>
            <div>개인화 투자정보 리포트 {selectedDate && `(${selectedDate.slice(2)})`}</div>
          </div>
        </h2>
        
        <div className="flex w-full space-x-10">
          <div className="flex !flex-col w-[40%]">
            <div className={styles.content}>
              <div className={styles.leftSection}>
                <div className="flex justify-between space-x-4 mb-6">
                  <div className="flex-1 bg-gray-50 p-4 rounded-lg">
                    <h3 className="text-gray-600 text-sm mb-2">총 평가금액</h3>
                    <ValueWithDiff 
                      amount={calculations.totalMarketValue(stockData)}
                      percentage={calculations.profitRate(
                        calculations.totalMarketValue(stockData), 
                        calculations.totalAcquisitionAmount(stockData)
                      )}
                      dayOverDayDiff={calculations.totalDayOverDayDiff(stockData)}
                    />
                  </div>
                  <div className="flex-1 bg-gray-50 p-4 rounded-lg">
                    <h3 className="text-gray-600 text-sm mb-2">매입금액</h3>
                    <p className="text-xl font-bold">{formatNumber(calculations.totalAcquisitionAmount(stockData))}원</p>
                  </div>
                </div>

                <div className={styles.portfolioChart}>
                  <div className={styles.chartWrapper}>
                    <Doughnut data={chartData} options={chartOptions} />
                  </div>
                </div>
              </div>
            </div>
            <div className="mt-10">
              <div className={styles.rightSection}>
                <div className={styles.categoryTabs}>
                  {[
                    { label: '전체', value: 'ALL' },
                    { label: '국내', value: 'KR' },
                    { label: '해외', value: 'US' }
                  ].map((category) => (
                    <button
                      key={category.value}
                      className={`${styles.categoryTab} ${selectedMarket === category.value ? styles.activeTab : ''}`}
                      onClick={() => setSelectedMarket(category.value as 'ALL' | 'KR' | 'US')}
                    >
                      {category.label}
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
                  {getFilteredStocks(stockData).map((item, index) => {
                    const profitRate = calculations.profitRate(
                      parseFloat(item.market_value), 
                      parseFloat(item.acquisition_amount)
                    );
                    
                    const dayOverDayChange = calculations.dayOverDayChange(
                      parseFloat(item.market_value),
                      parseFloat(item.previous_close_price) * item.holding_quantity
                    );

                    return (
                      <div key={index} className={styles.investmentItem}>
                        <span>{item.stock_name}</span>
                        <span className={`${getValueColor(profitRate)} font-bold`}>
                          {formatPercentage(profitRate)}
                        </span>
                        <span>{formatNumber(parseFloat(item.market_value))}</span>
                        <span className={`${getValueColor(dayOverDayChange)} font-bold`}>
                          {formatPercentage(dayOverDayChange)}
                        </span>
                      </div>
                    );
                  })}
                </div>
              </div>
            </div>
          </div>
          <div className="space-y-7 p-4 w-[60%]">
            <div>
              <div className="text-xl font-bold mb-3">
                전일 증시
              </div>
              <div className="space-y-2 text-sm text-gray-700">
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>KOSPI는 전 거래일 대비 <span className="text-red-500 font-bold">1.9%</span> 상승한 2,488.6P로 마감했습니다. KOSDAQ은 전 거래일 대비 <span className="text-red-500 font-bold">1.7%</span> 상승한 718.0P로 마감했습니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>올해 첫 금통위 회의가 16일 열릴 예정이며, 높은 환율 수준이 지속되면서 금리 인하 결정에 부담을 가중시키고 있는 상황입니다. 원-달러 환율은 전일 대비 <span className="text-red-500 font-bold">1.3원</span> 상승한 1,469.7원에 마감했습니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>이번 주 수요일부터 개최되는 CES 2025에 대한 기대감으로 반도체 업종은 SK하이닉스(<span className="text-red-500 font-bold">+9.8%</span>)를 중심으로 강세를 지속했습니다. 이번 행사의 중심 키워드는 AI가 될 것으로 예상됩니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>현대차그룹 전기차 5종이 미국의 $7,500 EV Credit 대상에 포함되었다는 소식이 전해진 이후, 관련 밸류체인 수혜 기대감에 강세를 보였습니다. $7,500 EV Credit 대상 차종은 40종에서 25종으로 감소했습니다.</span>
                </div>
              </div>
            </div>

            <div>
              <div className="text-xl font-bold mb-3">
                글로벌 시황
              </div>
              <div className="space-y-2 text-sm text-gray-700">
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span><span className="font-bold">미국:</span> S&P 500 지수는 반도체 강세와 대만 폭스콘의 AI 서버 수요 증가에 따른 기대감으로 <span className="text-red-500 font-bold">+0.6%</span> 상승했습니다. 엔비디아는 사상 최고치를 경신하며 <span className="text-red-500 font-bold">+3.4%</span> 올랐습니다. 미국 12월 서비스 PMI도 33개월 만에 최고치인 <span className="text-red-500 font-bold">56.8</span>을 기록했습니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span><span className="font-bold">중국:</span> 상해종합지수는 특별한 호재 부재와 경기 우려 속에서 <span className="text-blue-500 font-bold">-0.1%</span> 하락했습니다. 12월 차이신 서비스 PMI는 <span className="text-gray-500 font-bold">52.2</span>로 전월 대비 하락하며 경기 회복에 대한 우려를 키웠습니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span><span className="font-bold">유로존:</span> EuroStoxx 50 지수는 미국 관세 정책 우려 완화로 <span className="text-red-500 font-bold">+2.4%</span> 상승했습니다. 유로존, 독일, 프랑스의 서비스 PMI가 모두 예상치를 상회하며 긍정적인 경제 흐름을 보였습니다.</span>
                </div>
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span><span className="font-bold">일본:</span> Nikkei 225 지수는 미국 철강 인수 무산과 차익 실현 매도세로 <span className="text-blue-500 font-bold">-1.5%</span> 하락했습니다. 도요타와 신에쓰화학 등 대미 판매 비중이 높은 종목이 하락세를 보였습니다.</span>
                </div>
              </div>
            </div>

            <div>
              <div className="flex mb-3 text-xl font-bold">
                보유종목 주요 뉴스
              </div>
              <div className="space-y-3 text-sm text-gray-700">
                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>
                    <span className="font-bold">서플러스글로벌:</span> 자회사 이큐글로벌의 신임 CEO로 최태호 전 SK키파운드리 부사장을 선임했다고 발표했습니다. 최태호 CEO는 반도체 업계에서 약 30년간 공정 개발, 기술 개발, 마케팅 등 다양한 경험을 쌓아왔으며, 이를 바탕으로 이큐글로벌의 반도체 전후공정 수리 솔루션 전문성과 글로벌 플랫폼 전략에 기여할 것으로 기대되고 있습니다. 이큐글로벌은 현재 중국과 싱가포르에 수리 센터를 운영 중이며, 미국과 대만에서의 신규 사업 확장을 통해 글로벌 네트워크를 강화하고 있습니다.
                  </span>
                </div>

                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>
                    <span className="font-bold">현대공업:</span> 현대차 신형 팰리세이드 내장재 수주 계약을 체결했다고 밝혔습니다. 계약에 따라 현대공업은 암레스트, 시트패드, 레그레스트, 센터시트를 포함한 내장재를 매년 <span className="font-bold">570억 원</span>, 총 6년간 <span className="font-bold">3,400억 원</span> 규모로 공급할 예정입니다. 특히 센터시트는 신형 팰리세이드 1열 센터콘솔에 적용되는 신규 품목으로, 향후 SUV와 중대형 차량으로의 확대를 통해 매출 증가와 부가가치 창출이 기대되고 있습니다.
                  </span>
                </div>

                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>
                    <span className="font-bold">애플:</span> 인도네시아 산업부는 애플 대표들과 회의를 개최하여 아이폰 16을 현지에서 판매하기 위해 필요한 회사의 투자에 대해 논의할 예정입니다. 이 기기는 이전에 현지 콘텐츠 요건을 충족하지 못하여 판매가 금지된 바 있습니다. 애플은 인도네시아에 제조 시설이 없으며, 2018년부터 현지 개발자 지원 조치를 통해 협력해 왔습니다. 산업부 대변인인 페브리 헨드리(Febri Hendri)는 이번 협상이 애플의 투자 약속에 초점을 맞출 것이라고 밝혔습니다. 애플은 매 3년마다 투자 약속을 해야 하며, 지난 <span className="font-bold">1,000만 달러</span>의 투자 약속은 2023년에 만료되었습니다.
                  </span>
                </div>

                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>
                    <span className="font-bold">엔비디아:</span> CES 기간 동안 CEO 젠슨 황은 새로운 AI 도구와 칩을 선보이며 회사의 AI 기술 분야에서의 리더십을 강화했습니다. AI 블루프린트, 메가 옴니버스 블루프린트, 이삭 GR00T를 통해 로봇 기술의 발전을 강조했으며, 자율차 기술을 위한 파트너십을 확대하고 차세대 RTX 블랙웰 GPU를 출시하였습니다. 프로젝트 DIGITS는 개인 AI 슈퍼컴퓨터로 개발자들에게 고급 기능을 제공할 예정입니다. 엔비디아의 주가는 긍정적인 시장 반응 속에서 <span className="text-red-500 font-bold">+4%</span> 상승했습니다.
                  </span>
                </div>

                <div className="flex space-x-2">
                  <span className="text-gray-400">•</span>
                  <span>
                    <span className="font-bold">유나이티드 항공:</span> 시가총액 <span className="font-bold">315억 달러</span>를 보유한 UAL은 4분기 실적에서 주당 <span className="font-bold">2.97달러</span>의 이익을 예상하고 있으며, 이는 전년 대비 <span className="text-red-500 font-bold">+48.5%</span> 증가한 수치입니다. 2024 회계연도 EPS는 <span className="font-bold">10.31달러</span>, 2025 회계연도는 <span className="font-bold">11.88달러</span>로 예측됩니다. 21명의 분석가 중 20명이 &quot;강력 매수&quot; 등급을 부여했으며, 평균 목표가는 <span className="font-bold">113.14달러</span>로 <span className="text-red-500 font-bold">+18.3%</span>의 상승 가능성을 시사하고 있습니다.
                  </span>
                </div>
              </div>
            </div>

          </div>

        </div>
      </div>
    </div>
  );
} 