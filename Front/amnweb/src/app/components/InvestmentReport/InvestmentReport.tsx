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

interface MarketSummary {
  category: 'previous_day_market' | 'global_market_overview';
  content: string;
  trd_dd: string;
}

interface NewsItem {
  ticker: string;
  market_type: string;
  content: string;
  trd_dd: string;
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

const ValueWithDiff = ({ amount, percentage, dayOverDayDiff, previousAmount }: { 
  amount: number; 
  percentage: number;
  dayOverDayDiff: number;
  previousAmount: number;
}) => (
  <div>
    <p className="text-xl font-bold">
      {formatNumber(amount)}원
    </p>
    <p className={`text-sm ${getValueColor(percentage)}`}>
      {formatNumber(amount - previousAmount)}원
      <span className="ml-1">
        ({formatPercentage(percentage)})
      </span>
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

// 종목 코드로 종목명을 찾는 헬퍼 함수
const findStockNameByTicker = (ticker: string, stocks: StockData[]) => {
  const stock = stocks.find(s => s.ticker === ticker);
  return stock ? stock.stock_name : ticker;
};

const formatNumericValue = (text: string) => {
  // 1. 퍼센트 처리 (숫자% 패턴)
  text = text.replace(
    /([-+]?\d*\.?\d+)%/g,
    (match, number) => {
      const num = parseFloat(number);
      const colorClass = num > 0 ? 'text-red-500' : num < 0 ? 'text-blue-500' : '';
      return `<span class="${colorClass} font-bold">${number}%</span>`;
    }
  );
  
  // 2. 원, 억, 조, 만, 만원, $ 단위 처리
  text = text.replace(
    /(\d+\.?\d*)(원|달러|억원|억|조원|조|만원|만|\$)/g,
    '<span class="font-bold">$1$2</span>'
  );
  
  return text;
};

// 주말인지 확인하는 유틸리티 함수 추가
const isWeekend = (date: Date): boolean => {
  const day = date.getDay();
  return day === 0 || day === 6; // 0은 일요일, 6은 토요일
};

// 가장 최근 평일을 반환하는 유틸리티 함수 추가
const getLatestWeekday = (date: Date): Date => {
  const newDate = new Date(date);
  while (isWeekend(newDate)) {
    newDate.setDate(newDate.getDate() - 1);
  }
  return newDate;
};

export default function InvestmentReport() {
  const today = format(getLatestWeekday(new Date()), 'yyyyMMdd');
  const [selectedUser, setSelectedUser] = useState(DEFAULT_USER);
  const [selectedDate, setSelectedDate] = useState(today);
  const [stockData, setStockData] = useState<StockData[]>([]);
  const [portfolioData, setPortfolioData] = useState<PortfolioData | null>(null);
  const [selectedMarket, setSelectedMarket] = useState<'ALL' | 'KR' | 'US'>('ALL');
  const [isLoading, setIsLoading] = useState(true);
  const [marketSummary, setMarketSummary] = useState<MarketSummary[]>([]);
  const [newsSummary, setNewsSummary] = useState<NewsItem[]>([]);

  const chartOptions = {
    responsive: true,
    maintainAspectRatio: false,
    // 차트 크기와 레이아웃 관련 옵션 추가
    layout: {
      padding: 20
    },
    plugins: {
      legend: {
        position: 'bottom' as const,
        labels: {
          padding: 20
        }
      }
    }
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

      // 시장 요약 데이터 요청
      const marketSummaryResponse = await fetch(
        'http://ec2-54-180-66-126.ap-northeast-2.compute.amazonaws.com:8000/personal/market-summary',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ trd_dd: selectedDate })
        }
      );

      // 뉴스 요약 데이터 요청
      const newsSummaryResponse = await fetch(
        'http://ec2-54-180-66-126.ap-northeast-2.compute.amazonaws.com:8000/personal/news-summary',
        {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            client_code: selectedUser,
            trd_dd: selectedDate
          })
        }
      );

      if (!stockResponse.ok || !portfolioResponse.ok) 
        throw new Error('API 호출 실패');

      const stockData: StockData[] = await stockResponse.json();
      const portfolioDataResponse = await portfolioResponse.json();
      const marketData = await marketSummaryResponse.json();
      const newsData = await newsSummaryResponse.json();

      setStockData(stockData);
      setPortfolioData(portfolioDataResponse[0]);
      setMarketSummary(marketData);
      setNewsSummary(newsData);
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

  // portfolioData 상태 확인을 위한 useEffect 추가
  useEffect(() => {
    console.log('Portfolio Data:', portfolioData);
  }, [portfolioData]);

  // 차트 데이터 구성
  const chartData = {
    labels: ['주식', '채권/펀드', '선물/옵션', '신탁', '기타'],
    datasets: [{
      data: portfolioData ? [
        Number(portfolioData.stocks) || 0,
        Number(portfolioData.bondsFunds) || 0,
        Number(portfolioData.derivatives) || 0,
        Number(portfolioData.trust) || 0,
        Number(portfolioData.others) || 0
      ] : [0, 0, 0, 0, 0],
      backgroundColor: [
        '#FF6384',
        '#36A2EB',
        '#FFCE56',
        '#4BC0C0',
        '#9966FF'
      ],
      borderWidth: 1
    }]
  };

  const getFilteredStocks = (stocks: StockData[]) => {
    if (selectedMarket === 'ALL') return stocks;
    return stocks.filter(stock => stock.market_type === selectedMarket);
  };

  // 날짜 선택 핸들러
  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const date = new Date(e.target.value);
    
    // 주말인 경우 가장 최근 평일로 설정
    if (isWeekend(date)) {
      const dayOfWeek = date.getDay();
      const daysToSubtract = dayOfWeek === 0 ? 2 : 1; // 일요일이면 2일 전, 토요일이면 1일 전
      date.setDate(date.getDate() - daysToSubtract);
    }
    
    setSelectedDate(format(date, 'yyyyMMdd'));
  };

  // 마켓 요약 컨텐츠 렌더링 함수 수정
  const renderMarketContent = (content: string) => {
    return content.split('\n').map((line, index) => {
      // "**국가**:" 또는 "국가:" 패턴 처리
      const countryMatch = line.match(/(?:\*\*)?([^*:]+)(?:\*\*)?:/);
      if (countryMatch) {
        const country = countryMatch[1].trim();
        const restContent = line.replace(/(?:\*\*)?[^:]+:/, '').trim();
        return (
          <div key={index} className="flex space-x-2 py-0.5">
            <span className="text-gray-400">•</span>
            <span className="text-gray-700">
              <span className="font-bold">{country}: </span>
              <span dangerouslySetInnerHTML={{ 
                __html: formatNumericValue(restContent) 
              }} />
            </span>
          </div>
        );
      }
      
      if (line.startsWith('- ')) {
        return (
          <div key={index} className="flex space-x-2 py-0.5">
            <span className="text-gray-400">•</span>
            <span 
              className="text-gray-700"
              dangerouslySetInnerHTML={{ 
                __html: formatNumericValue(line.substring(2)) 
              }}
            />
          </div>
        );
      }
      
      return (
        <p 
          key={index} 
          className="text-gray-700 py-0.5"
          dangerouslySetInnerHTML={{ 
            __html: formatNumericValue(line) 
          }}
        />
      );
    });
  };

  if (isLoading) return <div className={styles.container}></div>;

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
              className={styles.dateInput}
              onKeyDown={(e) => e.preventDefault()}
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
                      dayOverDayDiff={0}
                      previousAmount={calculations.totalAcquisitionAmount(stockData)}
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
                    
                    const dayOverDayChange = parseFloat(item.market_value_diff);

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
          <div className="space-y-2 p-2 w-[60%]">
            {marketSummary.map((summary) => (
              <div key={summary.category} className="bg-white rounded p-3">
                <div className="text-lg font-bold mb-1.5 text-gray-900">
                  {summary.category === 'previous_day_market' ? '전일 증시' : '글로벌 시황'}
                </div>
                <div className="space-y-0.5 text-sm">
                  {renderMarketContent(summary.content)}
                </div>
              </div>
            ))}

            <div className="bg-white rounded p-3">
              <div className="text-lg font-bold mb-1.5 text-gray-900">
                보유종목 주요 뉴스
              </div>
              <div className="space-y-0.5">
                {newsSummary.map((news, index) => (
                  <div key={index} className="flex space-x-2 py-0.5 text-sm">
                    <span className="text-gray-400">•</span>
                    <span className="text-gray-700">
                      <span className="font-bold text-gray-900">
                        {findStockNameByTicker(news.ticker, stockData)}:
                      </span>{' '}
                      <span dangerouslySetInnerHTML={{ 
                        __html: formatNumericValue(news.content) 
                      }} />
                    </span>
                  </div>
                ))}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
} 