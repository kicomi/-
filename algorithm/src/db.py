# -*- coding: utf-8 -*-
"""数据访问层：直接从 MySQL 读取业务数据与训练样本。"""
import pandas as pd
import pymysql

from .config import DB_CONF, WEIGHT_BROWSE, WEIGHT_FAVORITE, WEIGHT_ORDER


def _conn():
    return pymysql.connect(**DB_CONF)


def read_sql(sql: str, params=None) -> pd.DataFrame:
    conn = _conn()
    try:
        with conn.cursor() as cur:
            cur.execute(sql, params)
            cols = [c[0] for c in cur.description]
            rows = cur.fetchall()
        return pd.DataFrame(list(rows), columns=cols)
    finally:
        conn.close()


def load_price_samples() -> pd.DataFrame:
    """定价模型训练样本。"""
    return read_sql("""
        SELECT id, category_id, original_price, condition_level, wear_level, note_flag,
               publish_year, listing_count, sold_count_30d, seller_history, sold_price
          FROM price_sample
    """)


def load_books() -> pd.DataFrame:
    return read_sql("""
        SELECT id, seller_id, category_id, course_id, major_id, title, price,
               original_price, condition_level, status, view_count
          FROM book
    """)


def load_interactions() -> pd.DataFrame:
    """构建用户-书籍隐式反馈矩阵：浏览=1, 收藏=3, 下单=5，同键累加。"""
    rows = read_sql("""
        SELECT user_id, book_id, %s AS w FROM browse_log
        UNION ALL
        SELECT user_id, book_id, %s AS w FROM favorite
        UNION ALL
        SELECT buyer_id AS user_id, book_id, %s AS w FROM book_order WHERE status IN (1, 2)
    """, (WEIGHT_BROWSE, WEIGHT_FAVORITE, WEIGHT_ORDER))
    if rows.empty:
        return rows
    df = rows.groupby(["user_id", "book_id"], as_index=False)["w"].sum()
    return df.rename(columns={"w": "weight"})


def load_on_sale_books() -> pd.DataFrame:
    return read_sql("""
        SELECT id, seller_id, category_id, course_id, major_id, price, original_price,
               condition_level, note_flag, view_count, favorite_count
          FROM book WHERE status = 1
    """)
