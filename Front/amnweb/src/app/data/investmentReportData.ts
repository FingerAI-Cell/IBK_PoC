export interface InvestmentReport {
  date: string;
  totalAmount: string;
  investedAmount: string;
  portfolioData: {
    stocks: number;
    bondsFunds: number;
    derivatives: number;
    trust: number;
    others: number;
  };
  investmentDetails: Array<{
    name: string;
    value: string;
    amount: string;
    dayBefore: string;
  }>;
}

export const sampleInvestmentReport: InvestmentReport = {
  date: "2024-03-19",
  totalAmount: "32,803,200원",
  investedAmount: "31,300,000원",
  portfolioData: {
    stocks: 40,
    bondsFunds: 35,
    derivatives: 10,
    trust: 10,
    others: 5
  },
  investmentDetails: [
    { name: "삼성전자", value: "-2.34%", amount: "2,660,000", dayBefore: "53,500" },
    { name: "NVIDIA", value: "-0.85%", amount: "123,554", dayBefore: "21,430" },
    { name: "카카오", value: "-20.5%", amount: "3,158,632", dayBefore: "758,575" }
  ]
};

export const investmentReportHistory = [
  {
    id: 1,
    date: "2024-03-19",
    title: "3월 19일자 투자정보 리포트",
  },
  {
    id: 2,
    date: "2024-03-18",
    title: "3월 18일자 투자정보 리포트",
  },
  {
    id: 3,
    date: "2024-03-17",
    title: "3월 17일자 투자정보 리포트",
  }
]; 