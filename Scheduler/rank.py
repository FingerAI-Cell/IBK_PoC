import psycopg2
from psycopg2.extras import execute_values
from src.database import DBConnection  # DB 연결 클래스 사용
import json
import os

def update_rankings(db_conn, report_period):
    try:
        with db_conn.cursor() as cur:
            print("Updating rankings for each financial_name and report_period...")  # 로그 추가

            # RANK()를 사용하여 순위 계산
            rank_query = """
                WITH ranked_data AS (
                    SELECT
                        report_period,
                        financial_name,
                        company_name,
                        data,
                        RANK() OVER (PARTITION BY report_period, financial_name ORDER BY data DESC) AS calculated_rank
                    FROM financial_rank_table
                    WHERE report_period = %s
                )
                UPDATE financial_rank_table fr
                SET ranking = rd.calculated_rank
                FROM ranked_data rd
                WHERE fr.report_period = rd.report_period
                  AND fr.financial_name = rd.financial_name
                  AND fr.company_name = rd.company_name;
            """
            cur.execute(rank_query, (report_period,))
            db_conn.commit()
            print("Rankings updated successfully.")
    except Exception as e:
        db_conn.rollback()
        print(f"Error during ranking update: {e}")
        raise

def update_diff_data(db_conn, report_period):
    try:
        with db_conn.cursor() as cur:
            print("Updating difer_data based on ranking changes...")  # 로그 추가

            # 이전 분기 계산
            year, quarter = map(int, report_period.split('-Q'))
            prev_report_period = f"{year - 1}-Q{quarter}"  # 전년도 같은 분기

            # 이전 분기의 순위와 현재 분기의 순위 비교
            diff_query = f"""
                WITH previous_rank AS (
                    SELECT
                        report_period,
                        financial_name,
                        company_name,
                        ranking AS prev_ranking
                    FROM financial_rank_table
                    WHERE report_period = %s
                ),
                current_rank AS (
                    SELECT
                        report_period,
                        financial_name,
                        company_name,
                        ranking AS current_ranking
                    FROM financial_rank_table
                    WHERE report_period = %s
                )
                UPDATE financial_rank_table fr
                SET
                    difer_data = pr.prev_ranking - cr.current_ranking
                FROM current_rank cr
                JOIN previous_rank pr
                ON cr.financial_name = pr.financial_name
                   AND cr.company_name = pr.company_name
                WHERE fr.report_period = cr.report_period
                  AND fr.financial_name = cr.financial_name
                  AND fr.company_name = cr.company_name;
            """
            cur.execute(diff_query, (prev_report_period, report_period))
            db_conn.commit()
            print("Difer_data updated successfully.")
    except Exception as e:
        db_conn.rollback()
        print(f"Error during difer_data update: {e}")
        raise


def main(year=None, quarter=None):
    try:
        # db_config.json 파일 읽기
        config_path = os.path.join('./config', 'db_config.json')
        try:
            with open(config_path, 'r', encoding='cp949') as f:
                db_config = json.load(f)
        except UnicodeDecodeError:
            with open(config_path, 'r', encoding='utf-8') as f:
                db_config = json.load(f)

        print("DB 설정:", db_config)  # 설정 내용 확인

        db_conn = DBConnection(db_config)
        try:
            db_conn.connect()
            print("Database connection established.")  # 로그 추가

            # Report period 조합
            report_period = f"{year}-Q{quarter}"

            # Update rankings
            update_rankings(db_conn.conn, report_period)
            
            # 3. 이전 분기와 순위 차이 업데이트
            update_diff_data(db_conn.conn, report_period)
        finally:
            db_conn.close()
            print("Database connection closed.")  # 로그 추가

    except FileNotFoundError as e:
        print(f"Configuration file not found: {e}")
    except Exception as e:
        print(f"Unexpected error: {e}")

if __name__ == "__main__":
    import sys
    print(f"Command line arguments: {sys.argv}")
    year = int(sys.argv[1]) if len(sys.argv) > 1 else None
    quarter = int(sys.argv[2]) if len(sys.argv) > 2 else None
    main(year, quarter)