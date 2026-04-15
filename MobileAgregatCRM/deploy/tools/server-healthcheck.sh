#!/usr/bin/env bash

set -u
set -o pipefail

DEFAULT_BASE_URL="https://agrapp.agregatka.ru/"
DEFAULT_TIMEOUT="15"
DEFAULT_QR_CODE="TRS111405064"

BASE_URL="${HC_BASE_URL:-$DEFAULT_BASE_URL}"
API_USER="${HC_USER:-}"
API_PASS="${HC_PASS:-}"
API_TOKEN="${HC_TOKEN:-}"
QR_CODE="${HC_QR_CODE:-$DEFAULT_QR_CODE}"
TIMEOUT="${HC_TIMEOUT:-$DEFAULT_TIMEOUT}"
SHOW_JSON="${HC_SHOW_JSON:-0}"

if [[ "$SHOW_JSON" == "true" || "$SHOW_JSON" == "TRUE" ]]; then
  SHOW_JSON=1
fi

print_usage() {
  cat <<'EOF'
Server endpoints health checker

Usage:
  bash deploy/tools/server-healthcheck.sh [options]

Options:
  --base-url URL     API base URL (default: https://agrapp.agregatka.ru/)
  --user USER        API user (for gettoken)
  --pass PASS        API password/hash (for gettoken)
  --token TOKEN      Existing token (skip gettoken if set and user/pass not set)
  --qr-code CODE     QR code for getqrcomlectinfo (default: TRS111405064)
  --timeout SEC      Curl connect/max timeout in seconds (default: 15)
  --show-json        Print JSON response for each checked endpoint
  --help             Show this help

Env equivalents:
  HC_BASE_URL HC_USER HC_PASS HC_TOKEN HC_QR_CODE HC_TIMEOUT HC_SHOW_JSON
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base-url)
      BASE_URL="${2:-}"
      shift 2
      ;;
    --user)
      API_USER="${2:-}"
      shift 2
      ;;
    --pass)
      API_PASS="${2:-}"
      shift 2
      ;;
    --token)
      API_TOKEN="${2:-}"
      shift 2
      ;;
    --qr-code)
      QR_CODE="${2:-}"
      shift 2
      ;;
    --timeout)
      TIMEOUT="${2:-}"
      shift 2
      ;;
    --show-json)
      SHOW_JSON=1
      shift
      ;;
    --help|-h)
      print_usage
      exit 0
      ;;
    *)
      echo "Unknown option: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

if ! command -v curl >/dev/null 2>&1; then
  echo "ERROR: curl is required." >&2
  exit 1
fi

if ! [[ "$TIMEOUT" =~ ^[0-9]+$ ]] || [[ "$TIMEOUT" -le 0 ]]; then
  echo "ERROR: --timeout must be a positive integer." >&2
  exit 1
fi

if [[ -z "$BASE_URL" ]]; then
  echo "ERROR: --base-url cannot be empty." >&2
  exit 1
fi

BASE_URL="${BASE_URL%/}"

ANY_WARN_OR_FAIL=0
declare -a RESULT_ROWS=()

extract_json_error_message() {
  local body="$1"
  printf '%s\n' "$body" | tr -d '\r' | sed -n 's/.*"error"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1
}

extract_json_token() {
  local body="$1"
  printf '%s\n' "$body" | tr -d '\r' | sed -n 's/.*"token"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p' | head -n 1
}

has_json_error_field() {
  local body="$1"
  printf '%s\n' "$body" | grep -qi '"error"[[:space:]]*:'
}

add_result_row() {
  local endpoint="$1"
  local url="$2"
  local http_status="$3"
  local time_ms="$4"
  local verdict="$5"
  local short_error="$6"

  local short_error_compact="$short_error"
  local max_err_len=34
  if [[ ${#short_error_compact} -gt $max_err_len ]]; then
    short_error_compact="${short_error_compact:0:$((max_err_len-3))}..."
  fi

  RESULT_ROWS+=("$(printf '%s\t%s\t%s\t%s\t%s\t%s' \
    "$endpoint" "$http_status" "$verdict" "$time_ms" "$short_error_compact" "$url")")

  if [[ "$verdict" == "WARN" || "$verdict" == "FAIL" ]]; then
    ANY_WARN_OR_FAIL=1
  fi
}

print_json_block() {
  local endpoint="$1"
  local url="$2"
  local body="$3"
  echo
  echo "----- JSON: ${endpoint} -----"
  echo "URL: ${url}"
  printf '%s\n' "$body"
}

run_check() {
  local endpoint="$1"
  shift
  local -a query_pairs=("$@")

  local -a curl_args
  curl_args=(--silent --show-error --insecure --get "$BASE_URL" \
    --connect-timeout "$TIMEOUT" --max-time "$TIMEOUT")

  local pair key value
  for pair in "${query_pairs[@]}"; do
    key="${pair%%=*}"
    value="${pair#*=}"
    curl_args+=(--data-urlencode "${key}=${value}")
  done

  local cmd_url
  # Human-readable URL for table/debug.
  cmd_url="$BASE_URL?"
  for pair in "${query_pairs[@]}"; do
    key="${pair%%=*}"
    value="${pair#*=}"
    if [[ "$cmd_url" != *"?" ]]; then
      cmd_url="${cmd_url}&"
    fi
    cmd_url="${cmd_url}${key}=${value}"
  done

  local output curl_exit http_status time_total body time_ms verdict short_error error_from_json
  output="$(curl "${curl_args[@]}" \
    -w $'\n__HTTP_STATUS__:%{http_code}\n__TIME_TOTAL__:%{time_total}\n' 2>&1)"
  curl_exit=$?

  if [[ $curl_exit -ne 0 ]]; then
    http_status="000"
    time_ms="0"
    verdict="FAIL"
    local cleaned_error_output
    cleaned_error_output="$(printf '%s\n' "$output" | sed '/^__HTTP_STATUS__:/d;/^__TIME_TOTAL__:/d')"
    short_error="$(printf '%s\n' "$cleaned_error_output" | sed '/^$/d' | tail -n 1)"
    [[ -z "$short_error" ]] && short_error="curl failed"
    add_result_row "$endpoint" "$cmd_url" "$http_status" "$time_ms" "$verdict" "$short_error"
    if [[ "$SHOW_JSON" == "1" ]]; then
      print_json_block "$endpoint" "$cmd_url" "$cleaned_error_output"
    fi
    return
  fi

  http_status="$(printf '%s\n' "$output" | sed -n 's/^__HTTP_STATUS__://p' | tail -n 1)"
  time_total="$(printf '%s\n' "$output" | sed -n 's/^__TIME_TOTAL__://p' | tail -n 1)"
  body="$(printf '%s\n' "$output" | sed '/^__HTTP_STATUS__:/,$d')"

  time_ms="$(awk -v t="${time_total:-0}" 'BEGIN { printf "%d", t * 1000 }')"
  if [[ -z "$time_ms" ]]; then
    time_ms="0"
  fi

  if [[ "$http_status" != "200" ]]; then
    verdict="FAIL"
    short_error="HTTP ${http_status:-unknown}"
    error_from_json="$(extract_json_error_message "$body")"
    [[ -n "$error_from_json" ]] && short_error="${short_error}; ${error_from_json}"
  elif has_json_error_field "$body"; then
    verdict="WARN"
    short_error="$(extract_json_error_message "$body")"
    [[ -z "$short_error" ]] && short_error="error field present"
  else
    verdict="PASS"
    short_error="-"
  fi

  add_result_row "$endpoint" "$cmd_url" "$http_status" "$time_ms" "$verdict" "$short_error"

  if [[ "$SHOW_JSON" == "1" ]]; then
    print_json_block "$endpoint" "$cmd_url" "$body"
  fi

  if [[ "$endpoint" == "gettoken" && "$verdict" == "PASS" && -z "${API_TOKEN:-}" ]]; then
    local token_candidate
    token_candidate="$(extract_json_token "$body")"
    if [[ -n "$token_candidate" ]]; then
      API_TOKEN="$token_candidate"
    else
      ANY_WARN_OR_FAIL=1
      add_result_row "gettoken(token-parse)" "$cmd_url" "200" "$time_ms" "WARN" "token not found in response"
    fi
  fi
}

print_header() {
  echo "Server Healthcheck"
  echo "Base URL : $BASE_URL"
  echo "Timeout  : ${TIMEOUT}s"
  echo
}

print_table() {
  printf '%-22s | %-6s | %-5s | %-7s | %-34s | %s\n' \
    "ENDPOINT" "HTTP" "STATE" "TIME" "SHORT_ERROR" "URL"
  printf -- '%.0s-' {1..130}
  echo
  local row endpoint http verdict time_ms short_error url
  for row in "${RESULT_ROWS[@]}"; do
    endpoint="$(printf '%s' "$row" | cut -f1)"
    http="$(printf '%s' "$row" | cut -f2)"
    verdict="$(printf '%s' "$row" | cut -f3)"
    time_ms="$(printf '%s' "$row" | cut -f4)"
    short_error="$(printf '%s' "$row" | cut -f5)"
    url="$(printf '%s' "$row" | cut -f6)"
    printf '%-22s | %-6s | %-5s | %-7s | %-34s | %s\n' \
      "$endpoint" "$http" "$verdict" "${time_ms}ms" "$short_error" "$url"
  done
}

run_token_flow() {
  if [[ -n "${API_TOKEN:-}" && -z "${API_USER:-}" && -z "${API_PASS:-}" ]]; then
    echo "Token-only mode: gettoken check skipped (using provided token)."
    echo
    return
  fi

  if [[ -z "${API_USER:-}" || -z "${API_PASS:-}" ]]; then
    add_result_row "gettoken" "${BASE_URL}?task=gettoken" "-" "0" "FAIL" "missing --user/--pass or --token"
    return
  fi

  run_check "gettoken" \
    "task=gettoken" \
    "user=${API_USER}" \
    "pass=${API_PASS}"
}

run_token_required() {
  local endpoint="$1"
  shift
  local -a params=("$@")
  local task_name=""
  local pair

  for pair in "${params[@]}"; do
    if [[ "$pair" == task=* ]]; then
      task_name="${pair#task=}"
      break
    fi
  done

  if [[ -z "${API_TOKEN:-}" ]]; then
    if [[ -n "$task_name" ]]; then
      add_result_row "$endpoint" "${BASE_URL}?task=${task_name}" "-" "0" "FAIL" "token is empty"
    else
      add_result_row "$endpoint" "${BASE_URL}" "-" "0" "FAIL" "token is empty"
    fi
    return
  fi

  run_check "$endpoint" "${params[@]}"
}

main() {
  print_header

  run_token_flow

  run_token_required "getroles" \
    "task=getroles" \
    "token=${API_TOKEN:-}"

  run_token_required "events" \
    "task=getitemslist" \
    "token=${API_TOKEN:-}" \
    "type=Документ" \
    "name=Событие" \
    "count=1" \
    "ncount=0" \
    "viewtype=onlymy"

  run_token_required "workorders" \
    "task=getitemslist" \
    "token=${API_TOKEN:-}" \
    "type=Документ" \
    "name=ЗаказНаряд" \
    "count=1" \
    "ncount=0" \
    "viewtype=onlymy"

  run_token_required "complaints" \
    "task=getitemslist" \
    "token=${API_TOKEN:-}" \
    "type=Документ" \
    "name=Рекламация" \
    "count=1" \
    "ncount=0" \
    "viewtype=onlymy"

  run_token_required "inner_orders" \
    "task=getitemslist" \
    "token=${API_TOKEN:-}" \
    "type=Документ" \
    "name=ЗаказВнутренний" \
    "count=1" \
    "ncount=0" \
    "viewtype=onlymy"

  run_token_required "cargo" \
    "task=getitemslist" \
    "token=${API_TOKEN:-}" \
    "type=Документ" \
    "name=Груз" \
    "count=1" \
    "ncount=0" \
    "viewtype=onlymy"

  run_token_required "qr_collect_info" \
    "task=getqrcomlectinfo" \
    "token=${API_TOKEN:-}" \
    "code=${QR_CODE}"

  print_table
  echo

  if [[ "$ANY_WARN_OR_FAIL" -eq 1 ]]; then
    echo "Final status: WARN/FAIL detected."
    exit 1
  fi

  echo "Final status: PASS."
  exit 0
}

main
