-- Table: investment_company_announcement
CREATE TABLE investment_company_announcement (
    id SERIAL PRIMARY KEY,  -- 고유 ID
    company_name VARCHAR(255) NOT NULL,  -- 공시 대상 회사명
    reference_date VARCHAR(8) NOT NULL,  -- 기준 일자 (YYYYMMDD)
    leverage_ratio DECIMAL(10,2),  -- 레버리지 비율
    correction_status VARCHAR(1),  -- 정정 여부
    report_period VARCHAR(10) NOT NULL  -- 데이터 기준 시점 (예: 2023-Q3)
);

-- Add comments
COMMENT ON TABLE investment_company_announcement IS '투자회사 공시 정보를 저장하는 테이블';
COMMENT ON COLUMN investment_company_announcement.company_name IS '공시 대상 회사명';
COMMENT ON COLUMN investment_company_announcement.reference_date IS '기준 일자 (YYYYMMDD 형식)';
COMMENT ON COLUMN investment_company_announcement.leverage_ratio IS '레버리지 비율 (전 분기 말 기준)';
COMMENT ON COLUMN investment_company_announcement.correction_status IS '정정 여부 (예: Y/N)';
COMMENT ON COLUMN investment_company_announcement.report_period IS '데이터 기준 시점 (예: 2023-Q3)';