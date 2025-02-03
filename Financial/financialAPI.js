require('dotenv').config();
const express = require('express');
const { Pool } = require('pg');
const fs = require('fs');
const winston = require('winston');

const app = express();
const port = 3005;

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
        new winston.transports.Console(), // 콘솔에도 출력
        new winston.transports.File({ filename: `${logDir}/server.log` }) // 파일 기록
    ]
});

// PostgreSQL 연결 설정
const pool = new Pool({
    host: "p", //localhost
    database: "ibk_poc_financial_statements",
    user: "ibk-manager",
    password: "fingerai2024!",
    port: 5432
});

// ✅ 요청 로깅 (IP, 요청 시간, 쿼리 파라미터 포함)
app.use((req, res, next) => {
    const clientIp = req.headers['x-forwarded-for'] || req.socket.remoteAddress;
    logger.info(`Incoming Request: [${req.method}] ${req.originalUrl} from ${clientIp} - Params: ${JSON.stringify(req.query)}`);
    next();
});

// GET 요청 처리
app.get('/api/financials', async (req, res) => {
    const { report_period, financial_name, company_name, ranking, order_by, limit } = req.query;

    try {
        let query = `SELECT report_period, financial_name, company_name, data, ranking, difer_data FROM financial_rank_table WHERE 1=1`;
        let values = [];

        if (report_period) {
            query += ` AND report_period = $${values.length + 1}`;
            values.push(report_period);
        }
        if (financial_name) {
            query += ` AND financial_name = $${values.length + 1}`;
            values.push(financial_name);
        }
        if (company_name) {
            query += ` AND company_name = $${values.length + 1}`;
            values.push(company_name);
        }
        if (ranking) {
            query += ` AND ranking = $${values.length + 1}`;
            values.push(ranking);
        }

        // 정렬 및 제한
        if (order_by) {
            query += ` ORDER BY ${order_by} DESC`;
        }
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
        const responseJson = {
            "status": "success",
            "filters": {
                "report_period": report_period || null,
                "financial_name": financial_name || null,
                "company_name": company_name || null,
                "ranking": ranking || null,
                "order_by": order_by || null,
                "limit": limit ? parseInt(limit) : null
            },
            "results": result.rows
        };

        res.json(responseJson);
    } catch (error) {
        // ✅ 에러 발생 시 SQL 문과 에러 메시지를 함께 기록
        logger.error(`DB Query Error: ${error.message} - SQL: ${query} - Params: ${JSON.stringify(values)}`);
        res.status(500).json({ status: "error", message: "Internal Server Error" });
    }
});

// ✅ 서버 시작 로그 기록
app.listen(port, () => {
    logger.info(`🚀 Server started on http://localhost:${port}`);
});
