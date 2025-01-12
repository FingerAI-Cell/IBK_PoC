import psycopg2
from abc import abstractmethod
from psycopg2.extras import execute_values
import pandas as pd

class DB:
    def __init__(self, config):
        self.config = config

    @abstractmethod
    def connect(self):
        pass

    @abstractmethod
    def close(self):
        pass


class DBConnection(DB):
    def __init__(self, config):
        super().__init__(config)
        self.conn = None
        self.cur = None

    def connect(self):
        try:
            # 설정값 디버깅
            print("연결 설정:", self.config)
            
            # 모든 설정값을 str()로 변환하고 인코딩/디코딩
            conn_params = {
                'host': str(self.config['host']).encode('ascii', 'ignore').decode('ascii'),
                'dbname': str(self.config['db_name']).encode('ascii', 'ignore').decode('ascii'),
                'user': str(self.config['user_id']).encode('ascii', 'ignore').decode('ascii'),
                'password': str(self.config['user_pw']).encode('ascii', 'ignore').decode('ascii'),
                'port': self.config['port']
            }
            
            print("정제된 연결 설정:", conn_params)
            
            self.conn = psycopg2.connect(**conn_params)
            self.cur = self.conn.cursor()
            print("DB 연결 성공")
            
        except Exception as e:
            print(f"DB 연결 실패: {e}")
            print(f"설정값: {conn_params}")
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
            print(f"DB 삽입 오류: {e}")
            raise