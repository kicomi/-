# -*- coding: utf-8 -*-
"""供需匹配：ItemCF / UserCF / 内容匹配 的加权融合与离线评估。"""
import numpy as np
import pandas as pd
from scipy.sparse import csr_matrix
from sklearn.metrics.pairwise import cosine_similarity

MODEL_VERSION = "itemcf-v1.0"


def _safe_int(value, default: int = 0) -> int:
    """把可能为 None / NaN / 浮点的字段安全地转成 int。"""
    if value is None:
        return default
    try:
        if isinstance(value, float) and np.isnan(value):
            return default
        return int(value)
    except (TypeError, ValueError):
        return default


class HybridRecommender:
    """
    隐式反馈推荐：
      - ItemCF：以用户-物品交互矩阵计算物品相似度
      - 内容匹配：分类/课程/专业的 one-hot 余弦相似
      - 两路加权融合后按热度做轻微多样性重排
    """

    def __init__(self, alpha: float = 0.65, topk: int = 50):
        self.alpha = alpha            # ItemCF 权重，1-alpha 为内容权重
        self.topk = topk
        self.item_ids = []
        self.item_index = {}
        self.user_ids = []
        self.user_index = {}
        self.R = None                 # 用户-物品矩阵
        self.item_sim = None          # 物品相似度
        self.content_sim = None
        self.popularity = None

    # ------------------------------------------------------------ 训练

    def fit(self, interactions: pd.DataFrame, books: pd.DataFrame):
        self.item_ids = sorted(books["id"].astype(int).tolist())
        self.item_index = {v: i for i, v in enumerate(self.item_ids)}
        self.user_ids = sorted(interactions["user_id"].astype(int).unique().tolist())
        self.user_index = {v: i for i, v in enumerate(self.user_ids)}

        rows = interactions["user_id"].astype(int).map(self.user_index)
        cols = interactions["book_id"].astype(int).map(self.item_index)
        mask = cols.notna()
        rows, cols = rows[mask].values, cols[mask].values
        vals = interactions["weight"].astype(float)[mask].values

        self.R = csr_matrix((vals, (rows, cols)),
                            shape=(len(self.user_ids), len(self.item_ids)))

        # ItemCF 相似度：物品向量余弦
        item_user = self.R.T.tocsr()
        sim = cosine_similarity(item_user)
        np.fill_diagonal(sim, 0.0)
        self.item_sim = sim

        # 内容相似度：分类 + 课程 + 专业 三段 one-hot
        book_map = books.set_index("id")
        feats = []
        for iid in self.item_ids:
            row = book_map.loc[iid] if iid in book_map.index else None
            v = np.zeros(8 + 12 + 6, dtype=float)
            if row is not None:
                cat = _safe_int(row.get("category_id"))
                if 1 <= cat <= 8:
                    v[cat - 1] = 1.0
                # 课程/专业可能没关联上（种子数据里部分书是空的），
                # 直接 int(NaN) 会抛 ValueError，这里统一兜底成 0
                course = _safe_int(row.get("course_id")) % 12
                v[8 + course] = 1.0
                major = _safe_int(row.get("major_id")) % 6
                v[20 + major] = 1.0
            feats.append(v)
        self.content_sim = cosine_similarity(np.array(feats))
        np.fill_diagonal(self.content_sim, 0.0)

        self.popularity = np.asarray(self.R.sum(axis=0)).ravel()

    # ------------------------------------------------------------ 推荐

    def _user_vector(self, user_id: int):
        idx = self.user_index.get(int(user_id))
        if idx is None:
            return None
        return self.R[idx].toarray().ravel()

    def score_items(self, user_id: int, exclude: set = None) -> np.ndarray:
        uv = self._user_vector(user_id)
        n = len(self.item_ids)
        if uv is None or uv.sum() == 0:
            return self.popularity / (self.popularity.max() or 1)

        # ItemCF 打分
        cf_score = self.item_sim.dot(uv)
        denom = np.abs(self.item_sim).dot((uv > 0).astype(float))
        cf_score = cf_score / np.where(denom > 0, denom, 1.0)

        # 内容打分
        prof = self.content_sim.dot((uv > 0).astype(float))
        prof = prof / (np.linalg.norm(prof) or 1.0)

        def norm(a):
            mx = a.max()
            return a / mx if mx > 0 else a

        fused = self.alpha * norm(cf_score) + (1 - self.alpha) * norm(prof)
        fused = fused + 0.02 * norm(self.popularity)

        if exclude:
            for iid in exclude:
                j = self.item_index.get(int(iid))
                if j is not None:
                    fused[j] = -1
        return fused

    def recommend(self, user_id: int, n: int = 10, exclude_owned: bool = True) -> list:
        exclude = set()
        if exclude_owned:
            uv = self._user_vector(user_id)
            if uv is not None:
                exclude = {self.item_ids[i] for i in np.nonzero(uv)[0]}
        scores = self.score_items(user_id, exclude)
        order = np.argsort(-scores)[:n]
        out = []
        for j in order:
            if scores[j] <= 0:
                continue
            out.append({"bookId": int(self.item_ids[j]), "score": float(min(0.99, scores[j]))})
        return out

    def similar(self, book_id: int, n: int = 6) -> list:
        j = self.item_index.get(int(book_id))
        if j is None:
            return []
        blended = self.alpha * self.item_sim[j] + (1 - self.alpha) * self.content_sim[j]
        order = np.argsort(-blended)[:n]
        return [{"bookId": int(self.item_ids[k]), "score": float(max(0, blended[k]))} for k in order]

    # ------------------------------------------------------------ 评估

    def evaluate(self, interactions: pd.DataFrame, k: int = 10, seed: int = 42) -> dict:
        """留一法：每个用户留出一个正样本，计算 Recall@K（对热门基线做对比）。"""
        rng = np.random.default_rng(seed)
        grouped = interactions.groupby("user_id")["book_id"].apply(list)
        positives = {int(u): [int(b) for b in bs] for u, bs in grouped.items() if len(bs) >= 5}
        users = list(positives.keys())
        if not users:
            return {}

        pop_rank = np.argsort(-self.popularity)
        pop_top = [self.item_ids[i] for i in pop_rank[:k]]

        hits_cf = hits_content = hits_fused = hits_pop = 0
        for u in users:
            items = positives[u]
            target = int(rng.choice(items))
            exclude = set(items) - {target}

            # 融合
            scores = self.score_items(u, exclude)
            top = [self.item_ids[i] for i in np.argsort(-scores)[:k]]
            if target in top:
                hits_fused += 1

            # 纯 ItemCF
            uv = self._user_vector(u)
            cf = self.item_sim.dot(uv)
            cf = cf / np.where(np.abs(self.item_sim).dot((uv > 0).astype(float)) > 0,
                               np.abs(self.item_sim).dot((uv > 0).astype(float)), 1.0)
            for j in exclude:
                idx = self.item_index.get(j)
                if idx is not None:
                    cf[idx] = -1
            if target in [self.item_ids[i] for i in np.argsort(-cf)[:k]]:
                hits_cf += 1

            # 纯内容
            prof = self.content_sim.dot((uv > 0).astype(float))
            for j in exclude:
                idx = self.item_index.get(j)
                if idx is not None:
                    prof[idx] = -1
            if target in [self.item_ids[i] for i in np.argsort(-prof)[:k]]:
                hits_content += 1

            if target in pop_top:
                hits_pop += 1

        n = len(users)
        res = {
            "users": n,
            "k": k,
            "recall_itemcf": round(hits_cf / n, 4),
            "recall_content": round(hits_content / n, 4),
            "recall_fused": round(hits_fused / n, 4),
            "recall_popularity": round(hits_pop / n, 4),
        }
        base = res["recall_popularity"] or 1e-9
        res["lift_vs_popularity"] = round((res["recall_fused"] - res["recall_popularity"]) / base * 100, 2)
        return res
