"""端到端冒烟验收脚本。

在服务已启动（默认 127.0.0.1:8013）且 MySQL/Redis 可用时运行，
一次性验证 AI 客服模块的 7 类关键能力：

  1. 健康检查（存活 / 就绪）
  2. 非流式对话 + 多轮记忆
  3. 流式 SSE（thinking / token / done / usage）
  4. 工具调用打到真实 MySQL
  5. 会话落库（事件日志完整、序号连续、派生状态正确）
  6. 越权隔离（他人读不到、改不了、归档不了我的会话）
  7. 服务间 RSA 签名鉴权（缺失 / 篡改 / 过期）

用法：
    set PYTHONIOENCODING=utf-8
    venv\Scripts\python.exe scripts\smoke_test.py

说明：会真实调用 DeepSeek 并写入 MySQL，属于集成验收，不放在 pytest 里跑。
"""

import asyncio
import base64
import hashlib
import json
import os
import sys
import time
import uuid
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
import httpx
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import padding
from app.core.security import canonical_request

PRIVATE_KEY_FILE = os.getenv("AI_AGENT_TEST_PRIVATE_KEY_FILE", "")
BASE = "http://127.0.0.1:8013"
out = []
ok = 0
fail = 0

def check(label, cond, detail=""):
    global ok, fail
    if cond: ok += 1; out.append(f"  PASS  {label}")
    else: fail += 1; out.append(f"  FAIL  {label}  {detail}")

def hdr(b, method="POST", path="/v1/chat/completions", query="", timestamp=None, request_id=None):
    """构造签名头（覆盖 method+path+query+body）。"""
    if not PRIVATE_KEY_FILE:
        raise RuntimeError("请设置 AI_AGENT_TEST_PRIVATE_KEY_FILE 指向 Java RSA 私钥，供本地冒烟测试签名")
    ts = timestamp or str(int(time.time()))
    rid = request_id or "ver" + uuid.uuid4().hex[:8]
    canonical = canonical_request(method, path, query, b)
    digest = hashlib.sha256(canonical.encode("utf-8")).hexdigest()
    payload = f"{ts}.{rid}.{digest}".encode("utf-8")
    key = serialization.load_pem_private_key(Path(PRIVATE_KEY_FILE).read_bytes(), password=None)
    signature = key.sign(payload, padding.PKCS1v15(), hashes.SHA256())
    return {"Content-Type":"application/json","X-Timestamp":ts,"X-Request-Id":rid,
            "X-Sign":base64.b64encode(signature).decode("ascii")}

async def ask(c, q, conv, uid="15"):
    b = json.dumps({"messages":[{"role":"user","content":q}],"user_id":uid,
                    "conversation_id":conv,"stream":False}, ensure_ascii=False).encode()
    r = await c.post(BASE+"/v1/chat/completions", content=b,
                     headers=hdr(b, "POST", "/v1/chat/completions", ""))
    j = r.json()
    return r.status_code, (j.get("choices") or [{}])[0].get("message",{}).get("content","")

async def stream(c, q, conv, uid="15"):
    b = json.dumps({"messages":[{"role":"user","content":q}],"user_id":uid,
                    "conversation_id":conv,"stream":True}, ensure_ascii=False).encode()
    counts, done, err = {}, None, None
    async with c.stream("POST", BASE+"/v1/chat/stream", content=b,
                            headers=hdr(b, "POST", "/v1/chat/stream", "")) as r:
        ctype = r.headers.get("content-type","")
        async for line in r.aiter_lines():
            if not line.startswith("data:"): continue
            p = line[5:].strip()
            if p == "[DONE]": break
            ev = json.loads(p)
            counts[ev["type"]] = counts.get(ev["type"],0)+1
            if ev["type"]=="done": done = ev
            if ev["type"]=="error": err = ev
    return counts, done, err, ctype

async def llm_reachable() -> tuple[bool, str]:
    """预检模型服务可达性。

    上游抖动时这些用例会整片失败，但那不是代码问题。
    提前探一下，把结论区分成"代码缺陷"与"容器外部环境问题"。
    """
    probe = json.dumps({"messages": [{"role": "user", "content": "hi"}],
                        "user_id": "15", "stream": True}, ensure_ascii=False).encode()
    try:
        async with httpx.AsyncClient(timeout=60) as c:
            r = await c.post(BASE + "/v1/chat/stream",
                             content=probe,
                             headers=hdr(probe, "POST", "/v1/chat/stream", ""))
            async for line in r.aiter_lines():
                if line.startswith("data:") and '"type":"error"' in line:
                    return False, line[:160]
                if line.startswith("data:") and '"type":"done"' in line:
                    return True, "ok"
            return False, "stream ended without done"
    except Exception as exc:  # noqa: BLE001
        return False, f"{type(exc).__name__}: {exc}"


async def main():
    reachable, why = await llm_reachable()
    out.append("== 0. 模型服务预检 ==")
    out.append(f"  {'PASS' if reachable else 'WARN'}  LLM 可达性: {why}")
    if not reachable:
        out.append("  注意：下方 2/3/4/5 组用例依赖真实模型，")
        out.append("        上游不可达时这些失败属于外部环境问题，非代码缺陷。")
        out.append("")

    async with httpx.AsyncClient(timeout=200) as c:
        # ---------- 1. 健康检查 ----------
        r = await c.get(BASE+"/healthz"); out.append("== 1. 健康检查 ==")
        check("healthz 200", r.status_code==200)
        r = await c.get(BASE+"/readyz")
        deps = r.json().get("dependencies",{})
        check("readyz mysql+redis", deps.get("mysql") and deps.get("redis"), str(deps))

        # ---------- 2. 非流式 + 多轮记忆 ----------
        out.append("== 2. 非流式对话（多轮记忆） ==")
        conv = "conv_ver_" + uuid.uuid4().hex[:10]
        code, a1 = await ask(c, "我的积分是多少？", conv)
        check("第1轮 200", code==200, str(code))
        check("第1轮命中真实数据(51分)", "51" in a1, a1[:60])
        code, a2 = await ask(c, "我上一句问了你什么？", conv)
        check("第2轮记住上文", "积分" in a2, a2[:60])

        # ---------- 3. 流式 ----------
        out.append("== 3. 流式对话（SSE） ==")
        conv2 = "conv_ver_" + uuid.uuid4().hex[:10]
        counts, done, err, ctype = await stream(c, "签到规则是什么？", conv2)
        check("SSE ctype", "text/event-stream" in ctype, ctype)
        check("收到 token 事件", counts.get("token",0) > 0, str(counts))
        check("收到 thinking 事件(推理模型)", counts.get("thinking",0) > 0, str(counts))
        check("有 done 且正文非空", bool(done and done.get("text")), str(err))
        check("无 error 事件", err is None, str(err))
        check("done 带 usage", bool((done or {}).get("usage")), "")

        # ---------- 4. 工具调用 ----------
        out.append("== 4. 工具调用（真实 MySQL） ==")
        conv3 = "conv_ver_" + uuid.uuid4().hex[:10]
        counts, done, err, ctype = await stream(c, "平台上有没有 Java 相关的资料？", conv3)
        check("触发 tool 事件", counts.get("tool",0) > 0, str(counts))
        check("答案含真实资源", done and ("Java" in done.get("text","")), (done or {}).get("text","")[:70])

        # ---------- 5. 会话持久化 ----------
        out.append("== 5. 会话说落库（MySQL 事件日志） ==")
        sq = "userId=15&limit=5"
        r = await c.get(BASE+"/v1/sessions?"+sq,
                        headers=hdr(b"", "GET", "/v1/sessions", sq))
        sessions = r.json().get("sessions",[])
        ids = [s["sessionId"] for s in sessions]
        check("会话列表含本次会话", conv2 in ids, str(ids[:3]))
        me = [s for s in sessions if s["sessionId"]==conv2]
        check("轮次/用量已派生", me and me[0]["turnCount"]>=1 and me[0]["totalTokens"]>0, str(me[:1]))
        check("标题自动取自首问", me and me[0]["title"], str(me[:1]))

        dq = "userId=15"
        r = await c.get(BASE+f"/v1/sessions/{conv3}?"+dq,
                        headers=hdr(b"", "GET", f"/v1/sessions/{conv3}", dq))
        evs = r.json().get("events",[])
        types = [e["type"] for e in evs]
        check("事件日志齐全", "user_message" in types and "assistant_message" in types and "tool_call" in types, str(types))
        check("序号连续", [e["ordinal"] for e in evs]==list(range(1,len(evs)+1)), str([e["ordinal"] for e in evs]))

        # ---------- 6. 越权隔离 ----------
        out.append("== 6. 越权隔离 ==")
        cq = "userId=99"
        r = await c.get(BASE+f"/v1/sessions/{conv2}?"+cq,
                        headers=hdr(b"", "GET", f"/v1/sessions/{conv2}", cq))
        check("他人读会话 404", r.status_code==404, str(r.status_code))
        lq = "userId=99&limit=5"
        r = await c.get(BASE+"/v1/sessions?"+lq, headers=hdr(b"", "GET", "/v1/sessions", lq))
        check("他人列表不含我的会话", conv2 not in [s["sessionId"] for s in r.json()["sessions"]], "")
        r = await c.delete(BASE+f"/v1/sessions/{conv2}?"+cq,
                           headers=hdr(b"", "DELETE", f"/v1/sessions/{conv2}", cq))
        check("他人归档 404", r.status_code==404, str(r.status_code))
        # 用一个只存在于该会话上文里的"暗号"做确定性判断：
        # 先用暗号在 conv2 里建立一轮记忆，再让 userId=99 借用同一个会话 ID 追问暗号。
        # 隔离正确时，对方不可能知道这个暗号。
        secret_token = "暗号" + uuid.uuid4().hex[:6].upper()
        code, _ = await ask(c, f"请记住这个编号：{secret_token}，只回复“已记住”。", conv2, uid="15")
        code, a3 = await ask(c, "刚才让你记住的编号是什么？", conv2, uid="99")
        check("他人借用同一会话ID 被拒绝", code == 404, f"HTTP {code}: {a3[:70]}")

        # ---------- 7. 签名鉴权 ----------
        out.append("== 7. 服务间 RSA 签名鉴权 ==")
        b = json.dumps({"messages":[{"role":"user","content":"hi"}],"user_id":"15"}, ensure_ascii=False).encode()
        r = await c.post(BASE+"/v1/chat/completions", content=b, headers={"Content-Type":"application/json"})
        check("缺失签名 401", r.status_code==401, str(r.status_code))

        # 会话接口是 GET、无 body，必须也受签名保护（曾经的越权漏洞）
        r = await c.get(BASE+"/v1/sessions?userId=15&limit=5")
        check("会话列表无签名 401", r.status_code==401, str(r.status_code))
        r = await c.get(BASE+"/v1/sessions/conv_x?userId=15")
        check("会话详情无签名 401", r.status_code==401, str(r.status_code))

        # 拿合法签名去篡改查询参数（userId=15 -> userId=1）必须失效
        signed_headers = hdr(b"", "GET", "/v1/sessions", "userId=15&limit=5")
        r = await c.get(BASE+"/v1/sessions?userId=1&limit=5", headers={
            "X-Timestamp": signed_headers["X-Timestamp"],
            "X-Request-Id": signed_headers["X-Request-Id"],
            "X-Sign": signed_headers["X-Sign"]})
        check("篡改查询参数 401", r.status_code==401, str(r.status_code))
        ts = str(int(time.time()))
        r = await c.post(BASE+"/v1/chat/completions", content=b, headers={"Content-Type":"application/json",
            "X-Timestamp":ts,"X-Request-Id":"bad","X-Sign":"deadbeef"})
        check("错误签名 401", r.status_code==401, str(r.status_code))
        old = str(int(time.time())-4000)
        expired_headers = hdr(b, "POST", "/v1/chat/completions", "", timestamp=old, request_id="old")
        r = await c.post(BASE+"/v1/chat/completions", content=b, headers={"Content-Type":"application/json",
            "X-Timestamp":old,"X-Request-Id":"old","X-Sign":expired_headers["X-Sign"]})
        check("过期签名 401 (防重放)", r.status_code==401, str(r.status_code))

    out.append("")
    summary = f"===== 结果: {ok} 通过 / {fail} 失败 ====="
    if not reachable:
        summary += "  (模型服务不可达，部分失败属外部环境)"
    out.append(summary)
    report = Path(__file__).resolve().parent / "smoke_test_report.txt"
    report.write_text("\n".join(out), encoding="utf-8")
    print("\n".join(out))
    print(f"\n报告已写入: {report}")

asyncio.run(main())
