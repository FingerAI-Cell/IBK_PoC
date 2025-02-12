require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const fs = require('fs');
const winston = require('winston');

const app = express();
const port = process.env.PORT || 3005;

// ✅ 로그 파일 설정
const logDir = 'logs';
if (!fs.existsSync(logDir)) {
    fs.mkdirSync(logDir);
}

const logger = winston.createLogger({
    level: 'info',
    format: winston.format.combine(
        winston.format.timestamp({ format: 'YYYY-MM-DD HH:mm:ss' }),
        winston.format.printf(info => `${info.timestamp} [${info.level.toUpperCase()}] ${info.message}`)
    ),
    transports: [
        new winston.transports.Console(),
        new winston.transports.File({ filename: `${logDir}/server.log` })
    ]
});

// PostgreSQL 연결 설정
const pool = new Pool({
    host: process.env.DB_HOST || "postgres_postgresql-master_1",
    database: process.env.DB_NAME || "ibk_poc_financial_statements",
    user: process.env.DB_USER || "ibk-manager",
    password: process.env.DB_PASSWORD || "fingerai2024!",
    port: process.env.DB_PORT ? parseInt(process.env.DB_PORT) : 5432
});

// ✅ 요청 로깅 (IP, 요청 시간, 쿼리 파라미터 포함)
app.use((req, res, next) => {
    const clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    logger.info(`Incoming Request: [${req.method}] ${req.originalUrl} from ${clientIp} - Params: ${JSON.stringify(req.query)}`);
    next();
});

// 재무지표 한글-영문 매핑
const financialNameMapping = {
    "자산 대비 이익률": "roa",
    "총 자본": "total_equity",
    "자기자본": "total_equity",
    "순이익": "net_income",
    "자본금": "capital_stock",
    "주식자본": "capital_stock",
    "국내 지점 수": "domestic_locations",
    "레버리지 비율": "leverage_ratio",
    "순자본비율": "net_capital_ratio",
    "순자본비율(ncr)": "net_capital_ratio",
    "ncr": "net_capital_ratio",
    "자기자본이익률": "roe",
    "총자산": "total_assets",
    "총 직원 수": "total_employees"
};

// GET 요청 처리
app.get('/api/financials', async (req, res) => {
    let { report_period, financial_name, company_name, ranking, order_by, limit } = req.query;

    // financial_name 변환 처리
    if (financial_name) {
        const originalNames = financial_name.split(',');
        const mappedNames = originalNames.map(name => {
            // 모든 공백 제거 및 소문자 변환으로 정규화
            const normalizedName = name.trim().replace(/\s+/g, '');
            
            // 정규화된 키로 매핑 찾기
            const mappedValue = Object.entries(financialNameMapping).find(([key, value]) => {
                const normalizedKey = key.replace(/\s+/g, '');
                return normalizedKey === normalizedName;
            });

            if (mappedValue) {
                logger.info(`Financial name mapping: "${name}" -> "${mappedValue[1]}"`);
                return mappedValue[1];
            }
            return name;
        });
        financial_name = mappedNames.join(',');
    }

    let query = `SELECT report_period, financial_name, company_name, data, ranking, difer_data FROM financial_rank_table WHERE 1=1`;
    let values = [];
    try {
        // ✅ 다중 값 OR 처리 (쉼표 `,`로 구분된 값)
        const addFilter = (column, param) => {
            if (!param) return "";
            const items = param.split(',');
            if (items.length === 1) {
                query += ` AND ${column} = $${values.length + 1}`;
                values.push(items[0]);
            } else {
                const placeholders = items.map((_, i) => `$${values.length + i + 1}`).join(', ');
                query += ` AND ${column} IN (${placeholders})`;
                values.push(...items);
            }
        };

        // ✅ 각 필터 적용
        addFilter("report_period", report_period);
        addFilter("financial_name", financial_name);
        addFilter("company_name", company_name);
        addFilter("ranking", ranking ? ranking.split(',').map(r => parseInt(r, 10)).join(',') : null); // 숫자 변환

        // // ✅ 정렬 및 제한
        // if (order_by) query += ` ORDER BY ${order_by} DESC`;
        // ✅ ORDER BY 설정 (기본값: ranking ASC)
        let sortOrder = (order_by && order_by.toUpperCase() === "DESC") ? "DESC" : "ASC";
        query += ` ORDER BY ranking ${sortOrder}`;

        // ✅ LIMIT 적용 (사용자가 명시적으로 지정한 경우만 추가)
        if (limit) {
            query += ` LIMIT $${values.length + 1}`;
            values.push(parseInt(limit));
        }

        // ✅ SQL 실행 전 쿼리 로그 기록
        logger.info(`Executing SQL: ${query} - Params: ${JSON.stringify(values)}`);

        // 데이터 조회
        const result = await pool.query(query, values);

        // ✅ 정상 요청 로그 기록 (응답 개수 포함)
        logger.info(`DB Query Success: [${req.method}] ${req.originalUrl} - Rows Retrieved: ${result.rows.length}`);

        // JSON 응답 생성
        res.json({
            "status": "success",
            "filters": {
                "report_period": report_period || null,
                "financial_name": financial_name || null,  // 매핑된 영문 값만 문자열로 반환
                "company_name": company_name || null,
                "ranking": ranking ? ranking.split(',').map(r => parseInt(r, 10)) : null,
                "order_by": `ranking ${sortOrder}`,
                "limit": limit ? parseInt(limit) : null
            },
            "results": result.rows
        });

    } catch (error) {
        logger.error(`DB Query Error: ${error.message} - SQL: ${query} - Params: ${JSON.stringify(values)}`);
        res.status(500).json({ status: "error", message: "Internal Server Error" });
    }
});

// ✅ 서버 시작 로그 기록
app.listen(port, () => {
    logger.info(`🚀 Server started on http://localhost:${port}`);
});
