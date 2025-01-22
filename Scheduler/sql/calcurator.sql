CREATE TABLE calcurator (
    report_period VARCHAR(10) NOT NULL,       -- 분기 정보 (예: 2024-Q1)
    company_name VARCHAR(100) NOT NULL,      -- 회사 이름
    quarter_num INT NOT NULL,                -- 분기 숫자 (1, 2, 3, 4)
    fiscal_month VARCHAR(10) NOT NULL,       -- 결산 월 (예: 3월, 12월)
    total_equity BIGINT NOT NULL,            -- 자본 총계
    total_assets BIGINT NOT NULL,            -- 총자산
    prev_total_equity BIGINT,                -- 전년도 자본 총계
    prev_total_assets BIGINT,                -- 전년도 총자산
    avg_total_assets NUMERIC(20, 2),         -- 평균 총자산 (확장)
    avg_total_equity NUMERIC(20, 2),         -- 평균 자기자본 (확장)
    net_income BIGINT NOT NULL,              -- 당기순이익 (계산된 값)
    current_net_income BIGINT,               -- 현재 분기의 당기순이익
    prev_net_income BIGINT,                  -- 작년 4분기의 당기순이익
    current_q1_net_income BIGINT,            -- 올해 1분기의 당기순이익
    roa NUMERIC(10, 2),                      -- 총자산이익률 (Return on Assets)
    roe NUMERIC(10, 2),                      -- 자기자본이익률 (Return on Equity)
    PRIMARY KEY (report_period, company_name)
);
