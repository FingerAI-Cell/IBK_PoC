-- Table: financial_condition
CREATE TABLE financial_condition (
    id SERIAL PRIMARY KEY,  -- 고유 ID
    company_name VARCHAR(255) NOT NULL,  -- 회사명
    fiscal_month VARCHAR(10),  -- 결산 월 (예: 12월)
    net_income BIGINT,  -- 당기순이익
    total_liabilities BIGINT,  -- 부채 총계
    industry_type VARCHAR(50),  -- 업권 구분 (예: 증권)
    operating_revenue BIGINT,  -- 영업 수익
    operating_expenses BIGINT,  -- 영업 비용
    operating_profit BIGINT,  -- 영업 이익
    capital_stock BIGINT,  -- 자본금
    total_equity BIGINT,  -- 자본 총계
    total_assets BIGINT,  -- 자산 총계
    report_period VARCHAR(10) NOT NULL  -- 데이터 기준 시점 (예: 2023-Q3)
);

-- Add comments for each column
COMMENT ON TABLE financial_condition IS '재무 상태 정보를 저장하는 테이블';
COMMENT ON COLUMN financial_condition.company_name IS '회사명';
COMMENT ON COLUMN financial_condition.fiscal_month IS '결산 월 (예: 12월)';
COMMENT ON COLUMN financial_condition.net_income IS '당기순이익';
COMMENT ON COLUMN financial_condition.total_liabilities IS '부채 총계';
COMMENT ON COLUMN financial_condition.industry_type IS '업권 구분 (예: 증권)';
COMMENT ON COLUMN financial_condition.operating_revenue IS '영업 수익';
COMMENT ON COLUMN financial_condition.operating_expenses IS '영업 비용';
COMMENT ON COLUMN financial_condition.operating_profit IS '영업 이익';
COMMENT ON COLUMN financial_condition.capital_stock IS '자본금';
COMMENT ON COLUMN financial_condition.total_equity IS '자본 총계';
COMMENT ON COLUMN financial_condition.total_assets IS '자산 총계';
COMMENT ON COLUMN financial_condition.report_period IS '데이터 기준 시점 (예: 2023-Q3)';