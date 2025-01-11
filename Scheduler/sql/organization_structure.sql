-- Table: organization_structure
CREATE TABLE organization_structure (
    id SERIAL PRIMARY KEY,  -- 고유 ID
    company_name VARCHAR(255) NOT NULL,  -- 회사명
    domestic_sales_offices INT,  -- 국내 영업소 수
    domestic_branches INT,  -- 국내 지점 수
    headquarters_departments INT,  -- 본부 부서 수
    industry_type VARCHAR(50),  -- 업권 구분 (예: 증권)
    total_units INT,  -- 합계 (국내외 모든 조직 합)
    overseas_offices INT,  -- 해외 사무소 수
    overseas_branches INT,  -- 해외 지점 수
    overseas_local_entities INT,  -- 해외 현지 법인 수
    report_period VARCHAR(10) NOT NULL  -- 데이터 기준 시점 (예: 2023-Q2)
);

-- Add comments for each column
COMMENT ON TABLE organization_structure IS '조직 구조 정보를 저장하는 테이블';
COMMENT ON COLUMN organization_structure.company_name IS '회사명';
COMMENT ON COLUMN organization_structure.domestic_sales_offices IS '국내 영업소 수';
COMMENT ON COLUMN organization_structure.domestic_branches IS '국내 지점 수';
COMMENT ON COLUMN organization_structure.headquarters_departments IS '본부 부서 수';
COMMENT ON COLUMN organization_structure.industry_type IS '업권 구분 (예: 증권)';
COMMENT ON COLUMN organization_structure.total_units IS '합계 (국내외 모든 조직 합)';
COMMENT ON COLUMN organization_structure.overseas_offices IS '해외 사무소 수';
COMMENT ON COLUMN organization_structure.overseas_branches IS '해외 지점 수';
COMMENT ON COLUMN organization_structure.overseas_local_entities IS '해외 현지 법인 수';
COMMENT ON COLUMN organization_structure.report_period IS '데이터 기준 시점 (예: 2023-Q2)';