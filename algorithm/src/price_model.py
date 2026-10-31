# -*- coding: utf-8 -*-
"""智能定价：多元线性回归 + 随机森林，输出点估计与 80% 价格区间。"""
import json
import os

import joblib
import numpy as np
import pandas as pd
from sklearn.ensemble import RandomForestRegressor
from sklearn.linear_model import LinearRegression
from sklearn.metrics import mean_absolute_error, mean_squared_error, r2_score
from sklearn.model_selection import KFold, cross_val_predict, train_test_split

from .config import PRICE_META_PATH, PRICE_MODEL_PATH
from .features import feature_names, to_frame, to_vector

MODEL_VERSION = "rf-v1.0"
BASELINE_DISCOUNT = 0.40   # 基线：原价 × 固定折扣


def _metrics(y_true, y_pred, orig) -> dict:
    mae = float(mean_absolute_error(y_true, y_pred))
    rmse = float(np.sqrt(mean_squared_error(y_true, y_pred)))
    return {
        "MAE": round(mae, 3),
        "RMSE": round(rmse, 3),
        "R2": round(float(r2_score(y_true, y_pred)), 4),
        "MAE占原价比例": round(mae / float(np.mean(orig)) * 100, 2),
    }


def train(df: pd.DataFrame, random_state: int = 42) -> dict:
    """训练定价模型并落盘，返回评估指标。"""
    X = to_frame(df)
    y = df["sold_price"].astype(float).values
    orig = df["original_price"].astype(float).values

    Xtr, Xte, ytr, yte, otr, ote = train_test_split(
        X.values, y, orig, test_size=0.2, random_state=random_state)

    baseline = ote * BASELINE_DISCOUNT
    metrics = {"baseline": _metrics(yte, baseline, ote)}

    lr = LinearRegression()
    lr.fit(Xtr, ytr)
    metrics["linear"] = _metrics(yte, lr.predict(Xte), ote)

    rf = RandomForestRegressor(
        n_estimators=300, max_depth=14, min_samples_leaf=3,
        random_state=random_state, n_jobs=-1)
    rf.fit(Xtr, ytr)
    metrics["random_forest"] = _metrics(yte, rf.predict(Xte), ote)

    # 用交叉验证残差的分位数构造 80% 价格区间
    kf = KFold(n_splits=5, shuffle=True, random_state=random_state)
    oof = cross_val_predict(rf, X.values, y, cv=kf, n_jobs=-1)
    resid = y - oof
    q10, q90 = float(np.percentile(resid, 10)), float(np.percentile(resid, 90))

    best = "random_forest" if metrics["random_forest"]["MAE"] <= metrics["linear"]["MAE"] else "linear"
    model = rf if best == "random_forest" else lr

    payload = {
        "model": model,
        "model_version": MODEL_VERSION,
        "best": best,
        "feature_names": feature_names(),
        "residual_q10": q10,
        "residual_q90": q90,
        "residual_std": float(np.std(resid)),
        "metrics": metrics,
        "importances": dict(zip(feature_names(), [float(v) for v in rf.feature_importances_])),
        "X_mean": [float(v) for v in X.mean().values],
        "X_std": [float(v) if v > 1e-9 else 1.0 for v in X.std().values],
    }
    joblib.dump(payload, PRICE_MODEL_PATH)
    with open(PRICE_META_PATH, "w", encoding="utf-8") as f:
        json.dump({k: v for k, v in payload.items() if k != "model"}, f,
                  ensure_ascii=False, indent=2)
    return {"model_version": MODEL_VERSION, "chosen": best, "metrics": metrics,
            "residual_q10": q10, "residual_q90": q90}


class PricePredictor:
    """推理封装：加载模型，输出建议价、区间与特征贡献。"""

    def __init__(self):
        self.payload = joblib.load(PRICE_MODEL_PATH) if os.path.exists(PRICE_MODEL_PATH) else None

    @property
    def ready(self) -> bool:
        return self.payload is not None

    # 校园教材的合理边界：低于 1 元或高于 2000 元的原价都属于脏数据
    MIN_PRICE = 1.0
    MAX_ORIGINAL = 2000.0

    @staticmethod
    def sanitize(payload: dict) -> dict:
        """
        输入清洗。回归模型是线性外推的，喂负数会算出负价格，
        喂 99999999 会算出 6000 万——接口不能假设调用方一定传对数据。
        """
        def num(key, default, lo, hi):
            try:
                v = float(payload.get(key, default))
            except (TypeError, ValueError):
                v = default
            if v != v:  # NaN
                v = default
            return max(lo, min(hi, v))

        return {
            "originalPrice": num("originalPrice", 50, PricePredictor.MIN_PRICE, PricePredictor.MAX_ORIGINAL),
            "conditionLevel": int(num("conditionLevel", 3, 1, 5)),
            "wearLevel": int(num("wearLevel", 3, 1, 5)),
            "noteFlag": int(num("noteFlag", 0, 0, 1)),
            "publishYear": int(num("publishYear", 2018, 1950, 2100)),
            "listingCount": int(num("listingCount", 2, 0, 999)),
            "soldCount30d": int(num("soldCount30d", 0, 0, 999)),
            "categoryId": int(num("categoryId", 1, 1, 99)),
            **{k: v for k, v in payload.items()
               if k not in {"originalPrice", "conditionLevel", "wearLevel", "noteFlag",
                            "publishYear", "listingCount", "soldCount30d", "categoryId"}},
        }

    def predict(self, payload: dict) -> dict:
        p = self.payload
        model = p["model"]
        payload = self.sanitize(payload or {})
        x = to_vector(payload)
        pred = float(model.predict(x)[0])

        orig = float(payload.get("originalPrice") or 0)
        # 二手价不可能高于原价，也不会是负数
        pred = max(0.5, min(pred, orig))
        low = pred + p["residual_q10"]
        high = pred + p["residual_q90"]
        low = max(0.5, min(low, pred))
        high = max(pred, min(high, orig))
        low = min(low, high)

        # 特征贡献（重要度 × 标准化偏离），用于前端展示"为什么是这个价"
        names = p["feature_names"]
        imp = p["importances"]
        mean = np.array(p["X_mean"])
        std = np.array(p["X_std"])
        dev = (x[0] - mean) / std
        contrib = {n: float(imp.get(n, 0) * dev[i]) for i, n in enumerate(names)}
        top = dict(sorted(contrib.items(), key=lambda kv: abs(kv[1]), reverse=True)[:5])

        conf = 0.95 if p["residual_std"] < max(3.0, pred * 0.25) else 0.8
        return {
            "suggested_price": round(pred, 2),
            "price_low": round(low, 2),
            "price_high": round(high, 2),
            "model": "RandomForestRegressor" if p["best"] == "random_forest" else "LinearRegression",
            "model_version": p["model_version"],
            "confidence": conf,
            "contributions": {k: round(v, 4) for k, v in top.items()},
        }