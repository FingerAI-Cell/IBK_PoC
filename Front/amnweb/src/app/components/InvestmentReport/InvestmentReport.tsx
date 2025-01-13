"use client";

import styles from "./InvestmentReport.module.css";
import { Doughnut } from 'react-chartjs-2';
import { Chart as ChartJS, ArcElement, Tooltip, Legend } from 'chart.js';
import { useEffect, useState, useCallback } from 'react';
import { format } from 'date-fns';

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
  client_code: string;
  stocks: number;
  bondsFunds: number;
  derivatives: number;
  trust: number;
  others: number;
}

const API_ENDPOINTS = {
  STOCK: '/api/personal/stock-overview',
  PORTFOLIO: '/api/personal/portfolio-overview',
  MARKET: '/api/personal/market-summary',
  NEWS: '/api/personal/news-summary'
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

const ValueWithDiff = ({ 
  amount = 0, 
  percentage = 0,
  dayOverDayDiff = 0,
  acquisitionAmount = 0 
}: { 
  amount?: number; 
  percentage?: number;
  dayOverDayDiff?: number;
  acquisitionAmount?: number;
}) => (
  <div>
    <p className="text-xl font-bold">
      {formatNumber(amount)}원
    </p>
    <div className="flex space-x-2 text-sm">
      <span className={`${getValueColor(dayOverDayDiff)}`}>
        {formatNumber(amount - acquisitionAmount)}원
      </span>
      <span className={`${getValueColor(percentage)}`}>
        {formatPercentage(percentage)}
      </span>
    </div>
  </div>
);

const calculations = {
  // 수익률 계산 (소수점 한자리)
  profitRate: (marketValue: number, acquisitionAmount: number) => {
    return acquisitionAmount === 0 ? 0 : Number(((marketValue - acquisitionAmount) / acquisitionAmount * 100).toFixed(1));
  },
  
  // 총 평가금액
  totalMarketValue: (stocks: StockData[] | undefined) => {
    if (!stocks || !Array.isArray(stocks)) return 0;
    return stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value || '0'), 0);
  },
  
  // 총 매입금액
  totalAcquisitionAmount: (stocks: StockData[] | undefined) => {
    if (!stocks || !Array.isArray(stocks)) return 0;
    return stocks.reduce((sum, stock) => sum + parseFloat(stock.acquisition_amount || '0'), 0);
  },

  // 전일대비 계산 (소수점 한자리)
  dayOverDayChange: (currentValue: number, previousValue: number) => {
    return previousValue === 0 ? 0 : Number(((currentValue - previousValue) / previousValue * 100).toFixed(1));
  },

  // 전체 포트폴리오의 전일대비 등락률 계산
  totalDayOverDayDiff: (stocks: StockData[] | undefined) => {
    if (!stocks || !Array.isArray(stocks)) return 0;
    const totalCurrentValue = stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value || '0'), 0);
    const totalPreviousValue = stocks.reduce((sum, stock) => sum + parseFloat(stock.market_value_previous || '0'), 0);
    return calculations.dayOverDayChange(totalCurrentValue, totalPreviousValue);
  }
};

// ... 기존 imports ...

const USERS = {
  'C2025BC3F4810': '이지혜',
  'C2025D3E28923': '김범수',
  'C2025F7AC2547': '김나라',
  'C2025E9D76421': '박희준',
  'C2025A4D57392': '황범석'
} as const;

const DEFAULT_USER = 'C2025BC3F4810';

// 1. 사용하지 않는 변수 제거 및 타입 정의
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

// formatDisplayDate에서 사용하지 않는 e 파라미터 제거
const formatDisplayDate = (dateStr: string) => {
  try {
    const year = dateStr.substring(0, 4);
    const month = dateStr.substring(4, 6);
    const day = dateStr.substring(6, 8);
    return `${year}-${month}-${day}`;
  } catch {
    return '';
  }
};

// 영업일 체크 및 가장 최근 영업일 반환 함수
const getLastBusinessDay = (date: Date): Date => {
  const day = date.getDay();
  const result = new Date(date);
  
  // 일요일(0)이면 금요일로
  if (day === 0) {
    result.setDate(result.getDate() - 2);
  }
  // 토요일(6)이면 금요일로
  else if (day === 6) {
    result.setDate(result.getDate() - 1);
  }
  
  return result;
};

// 차트 옵션 정의
const chartOptions = {
  responsive: true,
  maintainAspectRatio: false,
  plugins: {
    legend: {
      display: true,
      position: 'top' as const,
      labels: {
        padding: 20,
        usePointStyle: true
      }
    }
  }
};

export default function InvestmentReport() {
  const [selectedUser, setSelectedUser] = useState(DEFAULT_USER);
  const [selectedDate, setSelectedDate] = useState(() => {
    const today = getLastBusinessDay(new Date());
    return format(today, 'yyyyMMdd');
  });
  const [stockData, setStockData] = useState<StockData[]>([]);
  const [portfolioData, setPortfolioData] = useState<PortfolioData | null>(null);
  const [selectedMarket, setSelectedMarket] = useState<'ALL' | 'KR' | 'US'>('ALL');
  const [marketSummary, setMarketSummary] = useState<MarketSummary[]>([]);
  const [newsSummary, setNewsSummary] = useState<NewsItem[]>([]);

  // 차트 데이터는 컴포넌트 내부로 이동
  const chartData = {
    labels: ['주식', '채권/펀드', '선물/옵션', '신탁', '기타'],
    datasets: [{
      data: portfolioData ? [
        portfolioData.stocks || 0,
        portfolioData.bondsFunds || 0,
        portfolioData.derivatives || 0,
        portfolioData.trust || 0,
        portfolioData.others || 0
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

  const fetchData = useCallback(async () => {
    if (!selectedUser || !selectedDate) return;
    
    try {
      const requestBody = {
        client_code: selectedUser,
        trd_dd: selectedDate
      };

      const [stockResponse, portfolioResponse, marketResponse, newsResponse] = await Promise.all([
        fetch(API_ENDPOINTS.STOCK, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        }),
        fetch(API_ENDPOINTS.PORTFOLIO, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        }),
        fetch(API_ENDPOINTS.MARKET, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ trd_dd: selectedDate })
        }),
        fetch(API_ENDPOINTS.NEWS, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(requestBody)
        })
      ]);

      const [stockData, portfolioData, marketData, newsData] = await Promise.all([
        stockResponse.json(),
        portfolioResponse.json(),
        marketResponse.json(),
        newsResponse.json()
      ]);

      setStockData(stockData || []);
      setPortfolioData(portfolioData?.[0] || null);
      setMarketSummary(marketData || []);
      setNewsSummary(newsData || []);
    } catch (error) {
      console.error('데이터 로딩 실패:', error);
    }
  }, [selectedUser, selectedDate]);

  useEffect(() => {
    fetchData();
  }, [fetchData]);

  const getFilteredStocks = (stocks: StockData[] | undefined) => {
    if (!stocks || !Array.isArray(stocks)) return [];
    if (selectedMarket === 'ALL') return stocks;
    return stocks.filter(stock => stock.market_type === selectedMarket);
  };

  // 날짜 선택 핸들러 수정
  const handleDateChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const selectedDate = new Date(e.target.value);
    // 영업일 체크하여 설정
    const businessDate = getLastBusinessDay(selectedDate);
    setSelectedDate(format(businessDate, 'yyyyMMdd'));
  };

  // 유틸리티 함수 추가
  const formatNumericValue = (text: string) => {
    // 1. 퍼센트 처리
    text = text.replace(
      /([-+]?\d{1,3}(?:,\d{3})*\.?\d*|\d*\.?\d+)%/g,
      (_, number) => {
        const value = parseFloat(number.replace(/,/g, ''));
        const colorClass = value > 0 ? 'text-red-500' : value < 0 ? 'text-blue-500' : '';
        return `<span class="${colorClass} font-bold">${number}%</span>`;
      }
    );
    
    // 2. 원, 억, 조, 만, 만원, $ 단위 처리 - 콤마가 있는 숫자도 처리
    text = text.replace(
      /([-+]?\d{1,3}(?:,\d{3})*\.?\d*|\d*\.?\d+)(원|달러|억원|억|조원|조|만원|만|\$)/g,
      (match, number, unit) => {
        const num = parseFloat(number.replace(/,/g, ''));
        return `<span class="font-bold">${number}${unit}</span>`;
      }
    );
    
    return text;
  };

  const findStockNameByTicker = (ticker: string, stocks: StockData[]) => {
    const stock = stocks.find(s => s.ticker === ticker);
    return stock ? stock.stock_name : ticker;
  };

  // renderMarketContent 함수 추가
  const renderMarketContent = (content: string) => {
    return content.split('\n').map((line, index) => {
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

  if (!selectedUser || !selectedDate) return null;

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
              max={format(getLastBusinessDay(new Date()), 'yyyy-MM-dd')}
              onChange={handleDateChange}
              className="p-2 border rounded-md"
              onKeyDown={(e) => e.preventDefault()} // 키보드 입력 방지
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
                <div className={styles.summaryContainer}>
                  <div className={styles.summaryItem}>
                    <h3>총 평가금액</h3>
                    <ValueWithDiff 
                      amount={calculations.totalMarketValue(stockData)}
                      percentage={calculations.profitRate(
                        calculations.totalMarketValue(stockData), 
                        calculations.totalAcquisitionAmount(stockData)
                      )}
                      dayOverDayDiff={calculations.totalDayOverDayDiff(stockData)}
                      acquisitionAmount={calculations.totalAcquisitionAmount(stockData)}
                    />
                  </div>
                  <div className={styles.summaryItem}>
                    <h3>매입금액</h3>
                    <p className="text-xl font-bold">
                      {formatNumber(calculations.totalAcquisitionAmount(stockData))}원
                    </p>
                  </div>
                </div>

                <div className={styles.portfolioChart}>
                  <div className={styles.chartWrapper}>
                    {portfolioData && <Doughnut data={chartData} options={chartOptions} />}
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
                  {getFilteredStocks(stockData).length > 0 ? (
                    getFilteredStocks(stockData).map((item, index) => {
                      const profitRate = calculations.profitRate(
                        parseFloat(item.market_value || '0'), 
                        parseFloat(item.acquisition_amount || '0')
                      );
                      
                      return (
                        <div key={index} className={styles.investmentItem}>
                          <span>{item.stock_name || '-'}</span>
                          <span className={`${getValueColor(profitRate)} font-bold`}>
                            {formatPercentage(profitRate)}
                          </span>
                          <span>{formatNumber(parseFloat(item.market_value || '0'))}</span>
                          <span className={`${getValueColor(parseFloat(item.market_value_diff || '0'))} font-bold`}>
                            {parseFloat(item.market_value_diff || '0')}%
                          </span>
                        </div>
                      );
                    })
                  ) : (
                    <div className="p-4 text-center text-gray-500">
                      데이터가 없습니다
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
          <div className="space-y-7 p-4 w-[60%]">
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
                {newsSummary.length > 0 ? (
                  newsSummary.map((news, index) => (
                    <div key={index} className="flex space-x-2 py-0.5 text-sm">
                      <span className="text-gray-400">•</span>
                      <span className="text-gray-700">
                        <span className="font-bold text-gray-900">
                          {findStockNameByTicker(news.ticker, stockData || [])}:
                        </span>{' '}
                        <span dangerouslySetInnerHTML={{ 
                          __html: formatNumericValue(news.content || '') 
                        }} />
                      </span>
                    </div>
                  ))
                ) : (
                  <div className="text-center text-gray-500 py-2">
                    뉴스 데이터가 없습니다
                  </div>
                )}
              </div>
            </div>
          </div>

        </div>
      </div>
    </div>
  );
} 