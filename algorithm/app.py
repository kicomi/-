# -*- coding: utf-8 -*-
"""
算法服务：回归定价 + 协同过滤推荐。
启动： E:\dev\pyenv\Scripts\python.exe app.py
接口： GET /health, POST /predict, POST /recommend, POST /similar,
      POST /potential-buyers, POST /reload
"""
import json
import os
import sys

sys.stdout.reconfigure(encoding="utf-8")

import joblib
import pandas as pd
from flask import Flask, jsonify, request
from flask_cors import CORS

from src.cf import HybridRecommender
from src.config import CF_MODEL_PATH, MODEL_DIR
from src.db import load_books, load_interactions, load_on_sale_books, load_price_samples
from src.price_model import PricePredictor, train as train_price

app = Flask(__name__)
CORS(app)

STATE = {"price": None, "cf": None, "price_metrics": None, "recall": None}

METRICS_PATH = os.path.join(MODEL_DIR, "metrics.json")


def build_price():
    df = load_price_samples()
    if df.empty:
        raise RuntimeError("price_sample 表为空，请先执行种子数据脚本")
    metrics = train_price(df)
    STATE["price"] = PricePredictor()
    STATE["price_metrics"] = metrics
    print("[price] trained:", metrics["chosen"], metrics["metrics"][metrics["chosen"]])


def build_cf():
    inter = load_interactions()
    books = load_books()
    if inter.empty or books.empty:
        raise RuntimeError("交互数据为空")
    rec = HybridRecommender(alpha=0.65)
    rec.fit(inter, books)
    STATE["cf"] = rec
    STATE["recall"] = rec.evaluate(inter, k=10)
    joblib.dump(rec, CF_MODEL_PATH)
    print("[cf] trained, recall:", STATE["recall"])


def ensure_models():
    if STATE["price"] is None:
        build_price()
    if STATE["cf"] is None:
        if os.path.exists(CF_MODEL_PATH):
            try:
                STATE["cf"] = joblib.load(CF_MODEL_PATH)
            except Exception:
                build_cf()
        else:
            build_cf()

    # CF 是从磁盘加载的，没有现成的评估结果，直接读 train.py 落盘的指标，
    # 免得 /health 每次都重新跑一遍留一法评估（200 用户要十几秒）。
    if STATE["recall"] is None:
        try:
            with open(METRICS_PATH, "r", encoding="utf-8") as f:
                STATE["recall"] = json.load(f).get("recall")
        except Exception:
            pass


@app.get("/health")
def health():
    return jsonify({
        "status": "ok",
        "price_ready": STATE["price"] is not None and STATE["price"].ready,
        "cf_ready": STATE["cf"] is not None,
        "price_metrics": STATE["price_metrics"],
        "recall": STATE["recall"],
    })


@app.post("/predict")
def predict():
    ensure_models()
    payload = request.get_json(force=True, silent=True) or {}
    try:
        return jsonify(STATE["price"].predict(payload))
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.post("/recommend")
def recommend():
    ensure_models()
    body = request.get_json(force=True, silent=True) or {}
    user_id = int(body.get("userId") or 0)
    n = int(body.get("n") or 10)
    rec = STATE["cf"]
    try:
        pool = rec.recommend(user_id, n=max(n * 4, 20))
        active = set(load_on_sale_books()["id"].astype(int).tolist())
        items = [i for i in pool if i["bookId"] in active][:n]
        return jsonify({
            "model": "ItemCF+Content",
            "model_version": "itemcf-v1.0",
            "reason": "根据你的浏览与收藏推荐",
            "items": items,
        })
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.post("/similar")
def similar():
    ensure_models()
    body = request.get_json(force=True, silent=True) or {}
    book_id = int(body.get("bookId") or 0)
    n = int(body.get("n") or 6)
    try:
        items = STATE["cf"].similar(book_id, n=n)
        active = set(load_on_sale_books()["id"].astype(int).tolist())
        items = [i for i in items if i["bookId"] in active][:n]
        return jsonify({"model": "ItemCF", "items": items})
    except Exception as e:
        return jsonify({"error": str(e)}), 500


@app.post("/potential-buyers")
def potential_buyers():
    """卖家视角：找最可能想买这本书的同学（演示用简化版）。"""
    ensure_models()
    body = request.get_json(force=True, silent=True) or {}
    seller_id = int(body.get("sellerId") or 0)
    n = int(body.get("n") or 5)
    try:
        books = load_books()
        mine = books[books["seller_id"] == seller_id]
        if mine.empty:
            return jsonify({"userIds": []})
        rec = STATE["cf"]
        cat_set = set(mine["category_id"].dropna().astype(int).tolist())
        inter = load_interactions()
        inter = inter.merge(books[["id", "category_id"]], left_on="book_id", right_on="id", how="left")
        cand = inter[inter["category_id"].isin(cat_set) & (inter["user_id"] != seller_id)]
        top = cand.groupby("user_id")["weight"].sum().sort_values(ascending=False).head(n)
        return jsonify({"userIds": [int(u) for u in top.index.tolist()]})
    except Exception as e:
        return jsonify({"userIds": [], "error": str(e)})


@app.post("/reload")
def reload_models():
    build_price()
    build_cf()
    return jsonify({"status": "reloaded"})


if __name__ == "__main__":
    print("算法服务启动中，先训练/加载模型...")
    ensure_models()
    print("算法服务已就绪: http://127.0.0.1:5001")
    app.run(host="127.0.0.1", port=5001, debug=False, threaded=True)
