# -*- coding: utf-8 -*-
"""定价模型的特征工程。"""
import numpy as np
import pandas as pd

from .config import CATEGORY_IDS

NUMERIC_FEATURES = [
    "original_price", "condition_level", "wear_level", "note_flag",
    "book_age", "listing_count", "sold_count_30d",
    "seller_history", "history_ratio",
]


def to_frame(df: pd.DataFrame, current_year: int = None) -> pd.DataFrame:
    """把原始样本转换成模型输入的特征表（保持列顺序稳定）。"""
    if current_year is None:
        current_year = 2026

    d = pd.DataFrame()
    d["original_price"] = df["original_price"].astype(float)
    d["condition_level"] = df["condition_level"].astype(float)
    d["wear_level"] = df["wear_level"].astype(float)
    d["note_flag"] = df["note_flag"].astype(float)
    d["book_age"] = (current_year - df["publish_year"].astype(float)).clip(lower=0)
    d["listing_count"] = df["listing_count"].astype(float)
    d["sold_count_30d"] = df["sold_count_30d"].astype(float)
    hist = df.get("seller_history", pd.Series([0.0] * len(df))).astype(float)
    d["seller_history"] = hist
    d["history_ratio"] = (hist / d["original_price"].replace(0, np.nan)).fillna(0.4)

    for cid in CATEGORY_IDS:
        d[f"cat_{cid}"] = (df["category_id"].astype(float) == cid).astype(float)

    return d


def feature_names() -> list:
    return NUMERIC_FEATURES + [f"cat_{c}" for c in CATEGORY_IDS]


def to_vector(payload: dict, current_year: int = None) -> np.ndarray:
    """单条预测：把接口传入的 dict 转成模型输入向量。"""
    if current_year is None:
        current_year = 2026
    orig = float(payload.get("originalPrice") or 0)
    hist = float(payload.get("sellerHistory") or orig * 0.4)
    row = {
        "original_price": orig,
        "condition_level": float(payload.get("conditionLevel") or 3),
        "wear_level": float(payload.get("wearLevel") or 3),
        "note_flag": float(payload.get("noteFlag") or 0),
        "book_age": max(0.0, current_year - float(payload.get("publishYear") or current_year - 3)),
        "listing_count": float(payload.get("listingCount") or 0),
        "sold_count_30d": float(payload.get("soldCount30d") or 0),
        "seller_history": hist,
        "history_ratio": (hist / orig) if orig else 0.4,
    }
    for cid in CATEGORY_IDS:
        row[f"cat_{cid}"] = 1.0 if float(payload.get("categoryId") or 0) == cid else 0.0
    return np.array([[row[n] for n in feature_names()]], dtype=float)
