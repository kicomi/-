# -*- coding: utf-8 -*-
"""一键训练：定价回归模型 + 协同过滤模型，输出指标。"""
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

import joblib

from src.cf import HybridRecommender
from src.config import CF_MODEL_PATH, MODEL_DIR
from src.db import load_books, load_interactions, load_price_samples
from src.price_model import train as train_price


def main():
    print("=" * 60)
    print(" 1) 训练智能定价模型（多元线性回归 vs 随机森林）")
    print("=" * 60)
    df = load_price_samples()
    print(f"样本数: {len(df)}")
    res = train_price(df)
    for name, m in res["metrics"].items():
        print(f"  {name:<14} MAE={m['MAE']:<8} RMSE={m['RMSE']:<8} R2={m['R2']:<8} MAE/原价={m['MAE占原价比例']}%")
    print(f"  选中模型: {res['chosen']}")

    print()
    print("=" * 60)
    print(" 2) 训练协同过滤推荐模型（ItemCF + 内容匹配）")
    print("=" * 60)
    inter = load_interactions()
    books = load_books()
    print(f"交互记录: {len(inter)} 条, 用户 {inter['user_id'].nunique()} 人, 书籍 {books['id'].nunique()} 本")
    rec = HybridRecommender(alpha=0.65)
    rec.fit(inter, books)
    recall = rec.evaluate(inter, k=10)
    for k, v in recall.items():
        print(f"  {k}: {v}")
    joblib.dump(rec, CF_MODEL_PATH)

    summary = {"price": res, "recall": recall}
    with open(os.path.join(MODEL_DIR, "metrics.json"), "w", encoding="utf-8") as f:
        json.dump(summary, f, ensure_ascii=False, indent=2)
    print()
    print("模型与指标已保存到", MODEL_DIR)


if __name__ == "__main__":
    main()
