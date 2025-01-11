import pandas as pd
import psycopg2
from psycopg2.extras import execute_values
import os
import datetime
from sqlalchemy import create_engine
from abc import abstractmethod

class DB:
    def __init__(self, config):
        self.config = config

    @abstractmethod
    def connect(self):
        pass

class DBConnection(DB):
    def __init__(self, config):
        super().__init__(config)
        self.conn = None
        self.cur = None
    
    def connect(self):
        try:
            dsn = f"host={self.config['host']} dbname={self.config['db_name']} user={self.config['user_id']} password={self.config['user_pw']} port={self.config['port']}"
            print("DSN:", dsn)  # DSN 확인용 로그 출력
            self.conn = psycopg2.connect(dsn=dsn)

            self.cur = self.conn.cursor()
            print("DB 연결 성공")
        except Exception as e:
            print(f"DB 연결 실패: {str(e)}")
            raise

    def close(self):
        if self.cur:
            self.cur.close()
        if self.conn:
            self.conn.close()

class FinancialDataInserter:
    def __init__(self, db_connection):
        self.db_connection = db_connection

    def insert_data(self, table_name, df, column_mapping, report_period):
        try:
            print(f"\n--- DB 삽입 시작: {table_name} ---")
            db_columns = list(column_mapping.values()) + ["report_period"]
            excel_columns = list(column_mapping.keys())
            
            print(f"DB 컬럼: {db_columns}")
            print(f"엑셀 컬럼: {excel_columns}")

            df["report_period"] = report_period
            rows = df[excel_columns + ["report_period"]].where(pd.notnull(df), None).values.tolist()
            print(f"삽입할 행 수: {len(rows)}")

            execute_values(
                self.db_connection.cur,
                f"INSERT INTO {table_name} ({', '.join(db_columns)}) VALUES %s;",
                rows
            )
            self.db_connection.conn.commit()
            print(f"{table_name}에 데이터 삽입 성공")
            
        except Exception as e:
            self.db_connection.conn.rollback()
            print(f"DB 삽입 오류: {str(e)}")
            raise

# DB 설정
DB_CONFIG = {
    "host": "localhost",
    "db_name": "ibk_poc_financial_statements",  # dbname
    "user_id": "ibk-manager",
    "user_pw": "figerai2024",
    "port": "5432"
}

# 옵션별 테이블 이름
OPTION_TABLES = {
    1: "manpower_status",
    2: "financial_condition",
    3: "financial_ratio",
    4: "investment_company_announcement",
    5: "organization_structure"
}

# 테이블별 컬럼 맵핑 (엑셀 컬럼명 -> DB 컬럼명)
COLUMN_MAPPINGS = {
    'manpower_status': {
        '감사': 'auditor',
        '경영이사': 'executive_director',
        '계약직원': 'contract_employee',
        '기타': 'others',
        '비등기임원': 'unregistered_executive',
        '사외이사': 'outside_director',
        '업권구분': 'sector',
        '임직원합계': 'total_employees',
        '정규직원': 'regular_employee',
        '투자권유대행인': 'company_name'  # 이 부분 수정
    },
    "financial_condition": {
        "회사명": "company_name",
        "결산": "fiscal_month",
        "당기순이익": "net_income",
        "부채총계": "total_liabilities",
        "업권구분": "industry_type",
        "영업수익": "operating_revenue",
        "영업비용": "operating_expenses",
        "영업이익": "operating_profit",
        "자본금": "capital_stock",
        "자본총계": "total_equity",
        "자산총계": "total_assets"
    },
    "financial_ratio": {
        "회사명": "company_name",
        "결산": "fiscal_month",
        "ROA": "roa",
        "ROE": "roe",
        "부채비율": "debt_ratio",
        "순자본비율": "net_capital_ratio",
        "업권구분": "industry_type",
        "영업용순자본비율": "operating_net_capital_ratio",
        "자기자본비율": "equity_ratio"
    },
    "investment_company_announcement": {
        "공시대상": "company_name",
        "기준일자": "reference_date",
        "레버리지비율/전분기말": "leverage_ratio",
        "정정여부": "correction_status"
    },
    "organization_structure": {
        "회사명": "company_name",
        "국내영업소": "domestic_sales_offices",
        "국내지점": "domestic_branches",
        "본부부서": "headquarters_departments",
        "업권구분": "industry_type",
        "합계": "total_units",
        "해외사무소": "overseas_offices",
        "해외지점": "overseas_branches",
        "해외현지법인": "overseas_local_entities"
    }
}

# 연도 및 분기 계산 함수
def get_quarter_dates(custom_year=None, custom_quarter=None):
    if custom_year and custom_quarter:
        return custom_year, custom_quarter

    today = datetime.datetime.now()
    year = today.year
    month = today.month
    quarter = (month - 1) // 3  # 현재 달의 분기 계산

    if quarter == 0:
        year -= 1
        quarter = 4

    return year, quarter

# 문제 있는 데이터 확인
def check_invalid_data(df):
    for index, row in df.iterrows():
        for col_name, value in row.items():
            try:
                # 문자열인 경우 UTF-8로 변환 시도
                if isinstance(value, str):
                    value.encode('utf-8').decode('utf-8')
            except UnicodeDecodeError as e:
                print(f"Invalid data at row {index + 1}, column '{col_name}': {value}")
                print(f"Error: {e}")

# 메인 처리 함수
def process_excel_files(custom_year=None, custom_quarter=None):
    db_conn = DBConnection(DB_CONFIG)
    try:
        db_conn.connect()
        inserter = FinancialDataInserter(db_conn)
        
        print("=== process_excel_files 시작 ===")
        year, quarter = get_quarter_dates(custom_year, custom_quarter)
        report_period = f"{year}-Q{quarter}"
        print(f"처리할 기간: {report_period}")

        for option, table_name in OPTION_TABLES.items():
            print(f"\n--- 테이블 {table_name} 처리 시작 ---")
            excel_file = f"./{table_name}/{year}Q{quarter}_{table_name}.xlsx"

            if not os.path.exists(excel_file):
                print(f"파일을 찾을 수 없음: {excel_file}")
                continue

            print(f"파일 발견: {excel_file}")
            column_mapping = COLUMN_MAPPINGS[table_name]
            
            try:
                print("엑셀 파일 읽기 시작...")
                df = pd.read_excel(excel_file, engine="openpyxl")
                print(df.head())
                print(f"엑셀 파일 읽기 완료. 데이터 크기: {df.shape}")
                
                print("인코딩 처리 시작...")
                df = df.applymap(lambda x: str(x).encode("utf-8", "ignore").decode("utf-8") if isinstance(x, str) else x)
                print("인코딩 처리 완료")
                
                print(f"사용 가능한 컬럼: {df.columns.tolist()}")
                print(f"매핑할 컬럼: {column_mapping.keys()}")
                
                print("데이터 유효성 검사 시작...")
                check_invalid_data(df)
                print("데이터 유효성 검사 완료")
                
                print("DB 삽입 시작...")
                inserter.insert_data(table_name, df, column_mapping, report_period)
                print("DB 삽입 완료")
                
            except Exception as e:
                print(f"처리 중 오류 발생: {str(e)}")
                print(f"오류 타입: {type(e).__name__}")
                import traceback
                print(f"상세 오류: {traceback.format_exc()}")

        print("=== process_excel_files 완료 ===")
        
    finally:
        db_conn.close()

if __name__ == "__main__":
    process_excel_files()
