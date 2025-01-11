import psycopg2

DB_CONFIG = {
    "dbname": "ibk_poc_financial_statements",
    "user": "ibk-manager",
    "password": "figerai2024",
    "host": "localhost",
    "port": "5432"
}

try:
    conn = psycopg2.connect(**DB_CONFIG)
    print("DB 연결 성공")
    conn.close()
except Exception as e:
    print(f"DB 연결 오류: {e}")