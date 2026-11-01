# 本地开发启动脚本
# 用法：.\scripts\run.ps1
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root
$env:PYTHONPATH = $root
$python = Join-Path $root "venv\Scripts\python.exe"
if (-not (Test-Path $python)) { $python = "python" }
& $python -m app.main