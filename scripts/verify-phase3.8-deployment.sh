#!/usr/bin/env bash

# Phase 3.8 部署验证自动化脚本
# 职责：校验数据库 Schema、运行集成测试、检查指标端点，并生成验证报告

set -euo pipefail

PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$PROJECT_ROOT"

TZ_NAME="Pacific/Auckland"
NOW="$(TZ="${TZ_NAME}" date '+%Y-%m-%d %H:%M')"
REPORT_FILE=".claude/phase3.8-verification-report.md"
LOG_DIR=".claude/logs"
mkdir -p "$LOG_DIR"
GRADLE_LOG="${LOG_DIR}/phase3.8-gradle.log"
METRICS_LOG="${LOG_DIR}/phase3.8-metrics.log"

DB_URL="${PHASE38_DB_URL:-${DATABASE_URL:-}}"
DB_SCHEMA="${PHASE38_DB_SCHEMA:-public}"
METRICS_ENDPOINT="${PHASE38_METRICS_ENDPOINT:-http://localhost:8080/q/metrics}"
GRADLE_BIN="${PHASE38_GRADLE_BIN:-./gradlew}"
TEST_PATTERN="${PHASE38_TEST_PATTERN:-io.aster.audit.*}"
REQUIRED_TESTS="${PHASE38_REQUIRED_TESTS:-16}"

overall_status=0
declare -a report_lines

report_lines+=("# Phase 3.8 本地部署验证报告")
report_lines+=("- 日期（NZST）：${NOW}")
report_lines+=("- 执行人：Codex")
report_lines+=("")
report_lines+=("## 检查项")

record_section() {
  report_lines+=("")
  report_lines+=("### $1")
}

record_result() {
  local label="$1"
  local success="$2"
  local detail="$3"
  local icon="✅"

  if [ "$success" = false ]; then
    icon="❌"
    overall_status=1
  fi

  report_lines+=("- ${icon} ${label}：${detail}")
  printf '%s %s：%s\n' "$icon" "$label" "$detail"
}

run_sql() {
  local sql="$1"
  psql --dbname="$DB_URL" -At -c "$sql"
}

check_db_schema() {
  record_section "数据库 Schema"

  if [ -z "$DB_URL" ]; then
    record_result "数据库连接" false "未配置 PHASE38_DB_URL 或 DATABASE_URL 环境变量"
    return
  fi

  if ! command -v psql >/dev/null 2>&1; then
    record_result "数据库客户端" false "未找到 psql，可通过 brew install postgresql 安装"
    return
  fi

  local schema_condition="LOWER(table_schema)=LOWER('${DB_SCHEMA}')"

  local sample_col
  if ! sample_col="$(run_sql "SELECT COUNT(*) FROM information_schema.columns WHERE ${schema_condition} AND table_name='anomaly_reports' AND column_name='sample_workflow_id';" 2>&1)"; then
    record_result "anomaly_reports.sample_workflow_id" false "查询失败：${sample_col}"
  else
    if [ "${sample_col//[[:space:]]/}" -ge 1 ]; then
      record_result "anomaly_reports.sample_workflow_id" true "列存在（schema=${DB_SCHEMA})"
    else
      record_result "anomaly_reports.sample_workflow_id" false "列缺失，请重新执行 Phase 3.8 数据迁移"
    fi
  fi

  local payload_col
  if ! payload_col="$(run_sql "SELECT COUNT(*) FROM information_schema.columns WHERE ${schema_condition} AND table_name='anomaly_actions' AND column_name='payload_json';" 2>&1)"; then
    record_result "anomaly_actions.payload_json" false "查询失败：${payload_col}"
  else
    if [ "${payload_col//[[:space:]]/}" -ge 1 ]; then
      record_result "anomaly_actions.payload_json" true "列存在（schema=${DB_SCHEMA})"
    else
      record_result "anomaly_actions.payload_json" false "列缺失，请重新执行 Phase 3.8 数据迁移"
    fi
  fi

  local sample_idx
  if ! sample_idx="$(run_sql "SELECT COUNT(*) FROM pg_indexes WHERE LOWER(schemaname)=LOWER('${DB_SCHEMA}') AND tablename='anomaly_reports' AND indexdef ILIKE '%sample_workflow_id%';" 2>&1)"; then
    record_result "sample_workflow_id 索引" false "查询失败：${sample_idx}"
  else
    if [ "${sample_idx//[[:space:]]/}" -ge 1 ]; then
      record_result "sample_workflow_id 索引" true "已发现包含 sample_workflow_id 的索引"
    else
      record_result "sample_workflow_id 索引" false "缺少 sample_workflow_id 索引"
    fi
  fi

  local payload_idx
  if ! payload_idx="$(run_sql "SELECT COUNT(*) FROM pg_indexes WHERE LOWER(schemaname)=LOWER('${DB_SCHEMA}') AND tablename='anomaly_actions' AND indexdef ILIKE '%payload_json%';" 2>&1)"; then
    record_result "payload_json 索引" false "查询失败：${payload_idx}"
  else
    if [ "${payload_idx//[[:space:]]/}" -ge 1 ]; then
      record_result "payload_json 索引" true "已发现包含 payload_json 的索引"
    else
      record_result "payload_json 索引" false "缺少 payload_json 索引"
    fi
  fi
}

run_integration_tests() {
  record_section "集成测试"

  if [ ! -x "$GRADLE_BIN" ]; then
    record_result "Gradle 命令" false "无法执行 ${GRADLE_BIN}"
    return
  fi

  echo "▶ 运行 Gradle 测试（日志：${GRADLE_LOG}）"
  if ! "$GRADLE_BIN" :quarkus-policy-api:test --tests "${TEST_PATTERN}" --info >"$GRADLE_LOG" 2>&1; then
    record_result "Phase 3.8 集成测试执行" false "Gradle 失败，详见 ${GRADLE_LOG}"
    return
  fi
  record_result "Phase 3.8 集成测试执行" true "Gradle 成功完成，日志：${GRADLE_LOG}"

  local test_dir="quarkus-policy-api/build/test-results/test"
  if [ ! -d "$test_dir" ]; then
    record_result "测试结果目录" false "未找到 ${test_dir}"
    return
  fi

  shopt -s nullglob
  local audit_reports=("$test_dir"/TEST-io.aster.audit*.xml)
  shopt -u nullglob

  if [ "${#audit_reports[@]}" -eq 0 ]; then
    record_result "Audit 测试产物" false "未找到 ${test_dir}/TEST-io.aster.audit*.xml"
    return
  fi

  if ! command -v python3 >/dev/null 2>&1; then
    record_result "Python 解析器" false "未找到 python3，请安装后重试"
    return
  fi

  local parse_output
  if ! parse_output=$(
    TEST_REPORTS_DIR="$test_dir" python3 - <<'PY'
import glob
import os
import xml.etree.ElementTree as ET

reports = glob.glob(os.path.join(os.environ["TEST_REPORTS_DIR"], "TEST-io.aster.audit*.xml"))
if not reports:
    print("MISSING")
    raise SystemExit(0)

total = 0
failures = 0
errors = 0

for path in reports:
    tree = ET.parse(path)
    root = tree.getroot()
    for case in root.iter("testcase"):
        total += 1
        if case.find("failure") is not None:
            failures += 1
        if case.find("error") is not None:
            errors += 1

print(f"{total}|{failures}|{errors}")
PY
  ); then
    record_result "Audit 测试结果解析" false "无法解析 XML，详见 ${GRADLE_LOG}"
    return
  fi

  if [ "$parse_output" = "MISSING" ]; then
    record_result "Audit 测试产物" false "未生成 audit 相关 XML"
    return
  fi

  IFS='|' read -r total_cases total_failures total_errors <<<"$parse_output"
  total_cases="${total_cases:-0}"
  total_failures="${total_failures:-0}"
  total_errors="${total_errors:-0}"

  local failed=$((total_failures + total_errors))
  if [ "$total_cases" -eq "$REQUIRED_TESTS" ] && [ "$failed" -eq 0 ]; then
    record_result "Audit 测试结果" true "共 ${total_cases}/${REQUIRED_TESTS} 项通过"
  else
    record_result "Audit 测试结果" false "执行结果：${total_cases}/${REQUIRED_TESTS}，失败 ${failed} 项"
  fi
}

check_metrics_endpoint() {
  record_section "指标端点"

  if ! command -v curl >/dev/null 2>&1; then
    record_result "curl 命令" false "未找到 curl"
    return
  fi

  echo "▶ 抓取指标（${METRICS_ENDPOINT}）"
  if ! curl -fsSL "${METRICS_ENDPOINT}" >"${METRICS_LOG}" 2>&1; then
    record_result "指标端点可达性" false "无法访问 ${METRICS_ENDPOINT}（详见 ${METRICS_LOG}）"
    return
  fi

  if grep -q "anomaly.rollback." "${METRICS_LOG}"; then
    record_result "anomaly.rollback.* 指标" true "指标已公开，日志：${METRICS_LOG}"
  else
    record_result "anomaly.rollback.* 指标" false "在 ${METRICS_ENDPOINT} 输出中未找到 anomaly.rollback.*"
  fi
}

finalize_report() {
  {
    for line in "${report_lines[@]}"; do
      printf '%s\n' "$line"
    done
  } >"$REPORT_FILE"

  echo ""
  echo "📄 验证报告已生成：${REPORT_FILE}"
}

check_db_schema
run_integration_tests
check_metrics_endpoint
finalize_report

exit "$overall_status"
