-- 새로 테이블 생성
CREATE TABLE financial_rank_table (
    report_period VARCHAR(10) NOT NULL,       -- 데이터가 수집된 분기 (예: 2025-Q1)
    financial_name VARCHAR(50) NOT NULL,     -- 재무타이틀 이름 (예: 자기자본, ROE 등)
    company_name VARCHAR(100) NOT NULL,      -- 회사 이름 (예: 삼성전자, LG화학 등)
    data NUMERIC(25, 2) NOT NULL,            -- 재무 데이터 값 (소수점 2자리까지 허용)
    ranking INT,                             -- 순위 (NULL로 초기화)
    differ_sign CHAR(1),                     -- 전분기 기호 (예: +, -)
    difer_data NUMERIC(15, 2),               -- 전분기 차이
    PRIMARY KEY (report_period, financial_name, company_name)
);

-- 테이블 설명 추가
COMMENT ON TABLE financial_rank_table IS '각 분기별 회사 재무 데이터 순위를 저장하는 테이블';

-- 컬럼별 설명 추가
COMMENT ON COLUMN financial_rank_table.report_period IS '데이터가 수집된 분기 (예: 2025-Q1)';
COMMENT ON COLUMN financial_rank_table.financial_name IS '재무타이틀 이름 (예: 자기자본, ROE 등)';
COMMENT ON COLUMN financial_rank_table.company_name IS '회사 이름 (예: 삼성전자, LG화학 등)';
COMMENT ON COLUMN financial_rank_table.data IS '재무 데이터 값 (소수점 2자리까지 허용)';
COMMENT ON COLUMN financial_rank_table.ranking IS '순위 (NULL로 초기화)';
COMMENT ON COLUMN financial_rank_table.differ_sign IS '전분기 기호 (예: +, -)';
COMMENT ON COLUMN financial_rank_table.difer_data IS '전분기 차이';