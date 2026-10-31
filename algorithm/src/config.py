# -*- coding: utf-8 -*-
"""算法模块的全局配置。"""
import os

DB_CONF = dict(
    host=os.getenv("DB_HOST", "127.0.0.1"),
    port=int(os.getenv("DB_PORT", "3306")),
    user=os.getenv("DB_USER", "root"),
    password=os.getenv("DB_PASSWORD", "123456"),
    database=os.getenv("DB_NAME", "campus_book"),
    charset="utf8mb4",
)

# 交互权重（隐式反馈）
WEIGHT_BROWSE = 1.0
WEIGHT_FAVORITE = 3.0
WEIGHT_ORDER = 5.0

CATEGORY_IDS = [1, 2, 3, 4, 5, 6, 7, 8]

MODEL_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "models")
os.makedirs(MODEL_DIR, exist_ok=True)

PRICE_MODEL_PATH = os.path.join(MODEL_DIR, "price_model.joblib")
PRICE_META_PATH = os.path.join(MODEL_DIR, "price_meta.json")
CF_MODEL_PATH = os.path.join(MODEL_DIR, "cf_model.joblib")
