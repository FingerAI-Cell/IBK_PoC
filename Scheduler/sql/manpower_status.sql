-- Table: manpower_status
CREATE TABLE manpower_status (
    id SERIAL PRIMARY KEY,  -- 고유 ID
    company_name VARCHAR(255) NOT NULL,  -- 회사명
    industry_type VARCHAR(50),  -- 업권 구분 (예: 증권)
    auditor_count INT,  -- 감사 인원 수
    executive_count INT,  -- 경영이사 수
    contract_staff_count INT,  -- 계약직원 수
    other_staff_count INT,  -- 기타 인원 수
    non_registered_executive_count INT,  -- 비등기임원 수
    outside_director_count INT,  -- 사외이사 수
    total_employees INT,  -- 전체 임직원 수
    regular_staff_count INT,  -- 정규직원 수
    investment_advisor_count INT,  -- 투자권유대행인 수
    report_period VARCHAR(10) NOT NULL  -- 데이터 기준 시점 (예: 2023-Q1)
);

-- Add comments for each column
COMMENT ON TABLE manpower_status IS '인력 현황 정보를 저장하는 테이블';
COMMENT ON COLUMN manpower_status.company_name IS '회사명';
COMMENT ON COLUMN manpower_status.industry_type IS '업권 구분 (예: 증권)';
COMMENT ON COLUMN manpower_status.auditor_count IS '감사 인원 수';
COMMENT ON COLUMN manpower_status.executive_count IS '경영이사 수';
COMMENT ON COLUMN manpower_status.contract_staff_count IS '계약직원 수';
COMMENT ON COLUMN manpower_status.other_staff_count IS '기타 인원 수';
COMMENT ON COLUMN manpower_status.non_registered_executive_count IS '비등기임원 수';
COMMENT ON COLUMN manpower_status.outside_director_count IS '사외이사 수';
COMMENT ON COLUMN manpower_status.total_employees IS '전체 임직원 수';
COMMENT ON COLUMN manpower_status.regular_staff_count IS '정규직원 수';
COMMENT ON COLUMN manpower_status.investment_advisor_count IS '투자권유대행인 수';
COMMENT ON COLUMN manpower_status.report_period IS '데이터 기준 시점 (예: 2023-Q1)';