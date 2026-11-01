"""DeepSeek 模型能力探针。

用途：换模型或升级 SDK 后，快速确认以下硬前提是否仍然成立——
  1. Key 是否有效、有哪些可用模型
  2. 是否支持原生 function calling（LangGraph 工具路线的前提）
  3. 是否支持流式 SSE
  4. 是否为推理模型（是否返回 reasoning_content）
  5. usage 字段结构（用于 token 计量）
  6. Prompt 缓存是否生效

运行：python scripts/probe_model.py
"""

import json
import os
import sys
import time
import urllib.request
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

TOOLS = [
    {
        "type": "function",
        "function": {
            "name": "search_resources",
            "description": "搜索平台资源",
            "parameters": {
                "type": "object",
                "properties": {"keyword": {"type": "string", "description": "搜索关键词"}},
                "required": ["keyword"],
            },
        },
    }
]


def _post(base: str, path: str, key: str, payload: dict, stream: bool = False, timeout: int = 90):
    req = urllib.request.Request(
        base + path,
        data=json.dumps(payload).encode("utf-8"),
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
    )
    return urllib.request.urlopen(req, timeout=timeout)


def main() -> int:
    from app.core.settings import get_settings

    s = get_settings()
    key = s.deepseek_api_key.get_secret_value()
    base = s.deepseek_api_base.rstrip("/")
    model = s.deepseek_model

    print(f"base_url = {base}")
    print(f"model    = {model}")
    print(f"key      = loaded (len={len(key)})")
    print()

    # 1) 模型列表
    print("[1/6] 可用模型 ...")
    try:
        with _post(base, "/models", key, {}, timeout=30) if False else urllib.request.urlopen(
            urllib.request.Request(base + "/models", headers={"Authorization": f"Bearer {key}"}), timeout=30
        ) as r:
            data = json.loads(r.read().decode("utf-8"))
        ids = [m.get("id") for m in data.get("data", [])]
        print(f"      OK  -> {ids}")
        if model not in ids:
            print(f"      !! 警告：{model} 不在可用列表中")
    except Exception as exc:  # noqa: BLE001
        print(f"      FAIL -> {type(exc).__name__}: {exc}")
        return 1

    # 2) 原生 tool calling
    print("[2/6] 原生 function calling ...")
    try:
        with _post(
            base,
            "/chat/completions",
            key,
            {
                "model": model,
                "messages": [{"role": "user", "content": "帮我搜一下平台上 Java 相关的资源"}],
                "tools": TOOLS,
                "tool_choice": "auto",
                "max_tokens": 512,
            },
        ) as r:
            d = json.loads(r.read().decode("utf-8"))
        choice = d["choices"][0]
        calls = choice["message"].get("tool_calls") or []
        ok = choice.get("finish_reason") == "tool_calls" and calls
        print(f"      {'OK' if ok else 'FAIL'}  -> finish_reason={choice.get('finish_reason')}, calls={len(calls)}")
        if calls:
            print(f"      name={calls[0]['function']['name']} args={calls[0]['function']['arguments']}")
        print(f"      usage={d.get('usage')}")
    except Exception as exc:  # noqa: BLE001
        print(f"      FAIL -> {type(exc).__name__}: {exc}")

    # 3) 流式 SSE
    print("[3/6] 流式 SSE ...")
    try:
        started = time.time()
        first_token_at = None
        kinds, reasoning_chunks, content_chunks = set(), 0, 0
        with _post(
            base,
            "/chat/completions",
            key,
            {"model": model, "messages": [{"role": "user", "content": "你好，用一句话介绍你自己"}], "stream": True, "max_tokens": 400},
        ) as r:
            for raw in r:
                line = raw.decode("utf-8").strip()
                if not line.startswith("data: "):
                    continue
                payload = line[6:]
                if payload == "[DONE]":
                    break
                delta = json.loads(payload)["choices"][0].get("delta", {})
                kinds |= set(delta.keys())
                if delta.get("reasoning_content"):
                    reasoning_chunks += 1
                if delta.get("content"):
                    content_chunks += 1
                    if first_token_at is None:
                        first_token_at = time.time() - started
        print(f"      OK  -> delta keys={sorted(kinds)}")
        print(f"      reasoning_chunks={reasoning_chunks} content_chunks={content_chunks}")
        print(f"      首字延迟≈{first_token_at:.2f}s" if first_token_at else "      首字延迟: 未捕获")
    except Exception as exc:  # noqa: BLE001
        print(f"      FAIL -> {type(exc).__name__}: {exc}")

    # 4) 推理模型特征
    print("[4/6] 推理模型特征(reasoning_content) ...")
    print(f"      {'是推理模型' if reasoning_chunks else '非推理模型或未返回 reasoning_content'}")

    # 5) Prompt 缓存
    print("[5/6] Prompt 缓存 ...")
    try:
        long_system = "你是客服。" + ("平台规则：积分、签到、上传、下载。" * 60)
        for i in (1, 2):
            with _post(
                base,
                "/chat/completions",
                key,
                {
                    "model": model,
                    "messages": [{"role": "system", "content": long_system}, {"role": "user", "content": "积分规则"}],
                    "max_tokens": 50,
                },
            ) as r:
                u = json.loads(r.read().decode("utf-8")).get("usage", {})
            hit = u.get("prompt_cache_hit_tokens", u.get("prompt_tokens_details", {}).get("cached_tokens"))
            print(f"      第{i}次 prompt_tokens={u.get('prompt_tokens')} cache_hit={hit}")
    except Exception as exc:  # noqa: BLE001
        print(f"      FAIL -> {type(exc).__name__}: {exc}")

    # 6) LangSmith
    print("[6/6] LangSmith 追踪 ...")
    if s.langsmith_enabled:
        try:
            from langsmith import Client

            os.environ["LANGSMITH_TRACING"] = "true"
            os.environ["LANGSMITH_API_KEY"] = s.langsmith_api_key.get_secret_value()
            os.environ["LANGSMITH_ENDPOINT"] = s.langsmith_endpoint
            client = Client()
            projects = [p.name for p in client.list_projects()]
            print(f"      OK  -> project={s.langsmith_project}, 可见项目={projects}")
        except Exception as exc:  # noqa: BLE001
            print(f"      FAIL -> {type(exc).__name__}: {exc}")
    else:
        print("      已关闭（LANGSMITH_TRACING=false 或未配置 Key）")

    print("\n探针完成。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
