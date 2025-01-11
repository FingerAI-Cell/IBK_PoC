-- Table: financial_ratio
CREATE TABLE financial_ratio (
    id SERIAL PRIMARY KEY,  -- 고유 ID
    company_name VARCHAR(255) NOT NULL,  -- 회사명
    fiscal_month VARCHAR(10),  -- 결산 월 (예: 12월)
    roa NUMERIC(10, 2),  -- 총자산이익률 (Return on Assets)
    roe NUMERIC(10, 2),  -- 자기자본이익률 (Return on Equity)
    debt_ratio NUMERIC(10, 2),  -- 부채비율
    net_capital_ratio NUMERIC(10, 2),  -- 순자본비율
    industry_type VARCHAR(50),  -- 업권 구분 (예: 증권)
    operating_net_capital_ratio NUMERIC(10, 2),  -- 영업용 순자본비율
    equity_ratio NUMERIC(10, 2),  -- 자기자본비율
    report_period VARCHAR(10) NOT NULL  -- 데이터 기준 시점 (예: 2023-Q2)
);

-- Add comments for each column
COMMENT ON TABLE financial_ratio IS '재무 비율 정보를 저장하는 테이블';
COMMENT ON COLUMN financial_ratio.company_name IS '회사명';
COMMENT ON COLUMN financial_ratio.fiscal_month IS '결산 월 (예: 12월)';
COMMENT ON COLUMN financial_ratio.roa IS '총자산이익률 (Return on Assets)';
COMMENT ON COLUMN financial_ratio.roe IS '자기자본이익률 (Return on Equity)';
COMMENT ON COLUMN financial_ratio.debt_ratio IS '부채비율';
COMMENT ON COLUMN financial_ratio.net_capital_ratio IS '순자본비율';
COMMENT ON COLUMN financial_ratio.industry_type IS '업권 구분 (예: 증권)';
COMMENT ON COLUMN financial_ratio.operating_net_capital_ratio IS '영업용 순자본비율';
COMMENT ON COLUMN financial_ratio.equity_ratio IS '자기자본비율';
COMMENT ON COLUMN financial_ratio.report_period IS '데이터 기준 시점 (예: 2023-Q2)';
