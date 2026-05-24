#!/usr/bin/env bash
# dev.sh — start / stop Qalytix in development mode
#
# Usage:
#   ./dev.sh start    — start postgres, backend, and frontend
#   ./dev.sh stop     — stop all three
#   ./dev.sh restart  — stop then start
#   ./dev.sh status   — show what is running
#   ./dev.sh logs     — tail backend + frontend logs

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/qalytix-backend"
UI_DIR="$SCRIPT_DIR/qalytix-ui"
LOGS_DIR="$SCRIPT_DIR/.dev-logs"

BACKEND_PID_FILE="$LOGS_DIR/backend.pid"
UI_PID_FILE="$LOGS_DIR/ui.pid"
BACKEND_LOG="$LOGS_DIR/backend.log"
UI_LOG="$LOGS_DIR/ui.log"

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[qalytix]${RESET} $*"; }
success() { echo -e "${GREEN}[qalytix]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[qalytix]${RESET} $*"; }
error()   { echo -e "${RED}[qalytix]${RESET} $*" >&2; }

# ── helpers ──────────────────────────────────────────────────────────────────

is_running() {
  local pid_file="$1"
  [[ -f "$pid_file" ]] && kill -0 "$(cat "$pid_file")" 2>/dev/null
}

stop_process() {
  local name="$1"
  local pid_file="$2"
  if is_running "$pid_file"; then
    local pid
    pid=$(cat "$pid_file")
    info "Stopping $name (pid $pid)…"
    kill "$pid" 2>/dev/null || true
    # wait up to 10 s for clean exit
    local i=0
    while kill -0 "$pid" 2>/dev/null && (( i < 20 )); do
      sleep 0.5; (( i++ ))
    done
    if kill -0 "$pid" 2>/dev/null; then
      warn "$name did not exit cleanly, sending SIGKILL"
      kill -9 "$pid" 2>/dev/null || true
    fi
    rm -f "$pid_file"
    success "$name stopped."
  else
    warn "$name is not running."
    rm -f "$pid_file"
  fi
}

wait_for_port() {
  local port="$1"
  local name="$2"
  local retries=60   # 30 s
  local i=0
  while ! nc -z localhost "$port" 2>/dev/null; do
    if (( i >= retries )); then
      error "$name did not become ready on port $port within 30 s."
      return 1
    fi
    sleep 0.5; (( i++ ))
  done
}

# ── postgres (docker) ─────────────────────────────────────────────────────────

start_postgres() {
  if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$'; then
    info "Postgres already running."
    return
  fi
  info "Starting Postgres container…"
  docker compose -f "$SCRIPT_DIR/docker-compose.yml" up -d postgres
  info "Waiting for Postgres to be healthy…"
  local i=0
  until docker inspect --format='{{.State.Health.Status}}' qalytix-postgres 2>/dev/null | grep -q healthy; do
    if (( i >= 60 )); then
      error "Postgres did not become healthy in time."
      exit 1
    fi
    sleep 0.5; (( i++ ))
  done
  success "Postgres ready."
}

stop_postgres() {
  if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$'; then
    info "Stopping Postgres container…"
    docker compose -f "$SCRIPT_DIR/docker-compose.yml" stop postgres
    success "Postgres stopped."
  else
    warn "Postgres container is not running."
  fi
}

# ── backend ───────────────────────────────────────────────────────────────────

start_backend() {
  if is_running "$BACKEND_PID_FILE"; then
    warn "Backend is already running (pid $(cat "$BACKEND_PID_FILE"))."
    return
  fi
  info "Starting backend (Spring Boot) on :8080…"
  mkdir -p "$LOGS_DIR"
  (
    cd "$BACKEND_DIR"
    ./mvnw spring-boot:run \
      -Dspring-boot.run.profiles=dev \
      -q \
      > "$BACKEND_LOG" 2>&1
  ) &
  echo $! > "$BACKEND_PID_FILE"
  info "Waiting for backend to be ready…"
  if wait_for_port 8080 "Backend"; then
    success "Backend ready — http://localhost:8080  (log: $BACKEND_LOG)"
  else
    error "Backend failed to start. Check $BACKEND_LOG"
    rm -f "$BACKEND_PID_FILE"
    exit 1
  fi
}

stop_backend() {
  # mvnw forks a child JVM; kill the process group so both die
  if is_running "$BACKEND_PID_FILE"; then
    local pid
    pid=$(cat "$BACKEND_PID_FILE")
    info "Stopping backend (pid $pid)…"
    # kill the Maven wrapper and its child Spring Boot JVM
    pkill -TERM -P "$pid" 2>/dev/null || true
    kill -TERM "$pid"     2>/dev/null || true
    local i=0
    while kill -0 "$pid" 2>/dev/null && (( i < 20 )); do
      sleep 0.5; (( i++ ))
    done
    kill -9 "$pid" 2>/dev/null || true
    rm -f "$BACKEND_PID_FILE"
    success "Backend stopped."
  else
    warn "Backend is not running."
    rm -f "$BACKEND_PID_FILE"
  fi
}

# ── frontend ──────────────────────────────────────────────────────────────────

start_ui() {
  if is_running "$UI_PID_FILE"; then
    warn "Frontend is already running (pid $(cat "$UI_PID_FILE"))."
    return
  fi
  info "Starting frontend (Vite) on :3000…"
  mkdir -p "$LOGS_DIR"
  (
    cd "$UI_DIR"
    npm run dev > "$UI_LOG" 2>&1
  ) &
  echo $! > "$UI_PID_FILE"
  info "Waiting for frontend to be ready…"
  if wait_for_port 3000 "Frontend"; then
    success "Frontend ready — http://localhost:3000  (log: $UI_LOG)"
  else
    error "Frontend failed to start. Check $UI_LOG"
    rm -f "$UI_PID_FILE"
    exit 1
  fi
}

stop_ui() {
  stop_process "Frontend" "$UI_PID_FILE"
}

# ── commands ──────────────────────────────────────────────────────────────────

cmd_start() {
  start_postgres
  start_backend
  start_ui
  echo ""
  success "All services running."
  echo -e "  Frontend  →  ${CYAN}http://localhost:3000${RESET}"
  echo -e "  Backend   →  ${CYAN}http://localhost:8080${RESET}"
  echo -e "  Swagger   →  ${CYAN}http://localhost:8080/swagger-ui/index.html${RESET}"
  echo -e "  Logs dir  →  ${CYAN}$LOGS_DIR${RESET}"
}

cmd_stop() {
  stop_ui
  stop_backend
  stop_postgres
  success "All services stopped."
}

cmd_status() {
  echo ""
  # Postgres
  if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$'; then
    echo -e "  Postgres  ${GREEN}running${RESET}  (docker: qalytix-postgres)"
  else
    echo -e "  Postgres  ${RED}stopped${RESET}"
  fi

  # Backend
  if is_running "$BACKEND_PID_FILE"; then
    echo -e "  Backend   ${GREEN}running${RESET}  (pid $(cat "$BACKEND_PID_FILE"))  :8080"
  else
    echo -e "  Backend   ${RED}stopped${RESET}"
  fi

  # Frontend
  if is_running "$UI_PID_FILE"; then
    echo -e "  Frontend  ${GREEN}running${RESET}  (pid $(cat "$UI_PID_FILE"))  :3000"
  else
    echo -e "  Frontend  ${RED}stopped${RESET}"
  fi
  echo ""
}

cmd_logs() {
  if [[ ! -f "$BACKEND_LOG" && ! -f "$UI_LOG" ]]; then
    warn "No log files found in $LOGS_DIR — have you run './dev.sh start'?"
    exit 1
  fi
  info "Tailing logs (Ctrl-C to exit)…"
  local files=()
  [[ -f "$BACKEND_LOG" ]] && files+=("$BACKEND_LOG")
  [[ -f "$UI_LOG"      ]] && files+=("$UI_LOG")
  tail -n 40 -f "${files[@]}"
}

# ── entrypoint ────────────────────────────────────────────────────────────────

case "${1:-}" in
  start)   cmd_start   ;;
  stop)    cmd_stop    ;;
  restart) cmd_stop; echo ""; cmd_start ;;
  status)  cmd_status  ;;
  logs)    cmd_logs    ;;
  *)
    echo "Usage: $0 {start|stop|restart|status|logs}"
    exit 1
    ;;
esac
