#!/usr/bin/env bash
# dev.sh — start / stop Qalytix in development mode
#
# Usage:
#   ./dev.sh start      — start postgres (auto-mode), backend, and frontend
#   ./dev.sh stop       — stop all three
#   ./dev.sh restart    — stop then start
#   ./dev.sh status     — show what is running
#   ./dev.sh logs       — tail backend + frontend logs
#   ./dev.sh setup-db   — create local postgres DB + user (no Docker required)
#
# Postgres mode (auto-detected, or force with env var):
#   QALYTIX_DB=docker   — always use Docker container  (default when Docker is available)
#   QALYTIX_DB=local    — always use local PostgreSQL   (default when Docker is absent)

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/qalytix-backend"
UI_DIR="$SCRIPT_DIR/qalytix-ui"
LOGS_DIR="$SCRIPT_DIR/.dev-logs"

BACKEND_PID_FILE="$LOGS_DIR/backend.pid"
UI_PID_FILE="$LOGS_DIR/ui.pid"
BACKEND_LOG="$LOGS_DIR/backend.log"
UI_LOG="$LOGS_DIR/ui.log"

# Local-postgres defaults (override via env vars)
DB_NAME="${DB_NAME:-qalytix}"
DB_USER="${DB_USER:-qalytix}"
DB_PASS="${DB_PASS:-qalytix}"
DB_PORT="${DB_PORT:-5432}"

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
RESET='\033[0m'

info()    { echo -e "${CYAN}[qalytix]${RESET} $*"; }
success() { echo -e "${GREEN}[qalytix]${RESET} $*"; }
warn()    { echo -e "${YELLOW}[qalytix]${RESET} $*"; }
error()   { echo -e "${RED}[qalytix]${RESET} $*" >&2; }

# ── postgres mode detection ───────────────────────────────────────────────────

detect_db_mode() {
  if [[ -n "${QALYTIX_DB:-}" ]]; then
    echo "$QALYTIX_DB"
    return
  fi
  # Auto-detect: use Docker only if daemon is responsive
  if command -v docker &>/dev/null && docker info &>/dev/null 2>&1; then
    echo "docker"
  else
    echo "local"
  fi
}

DB_MODE="$(detect_db_mode)"

# ── helpers ───────────────────────────────────────────────────────────────────

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
    local i=0
    while kill -0 "$pid" 2>/dev/null && (( i < 20 )); do
      sleep 0.5; (( i++ )) || true
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
  local max="${3:-60}"   # default 30 s (60 × 0.5 s)
  local i=0
  while ! nc -z localhost "$port" 2>/dev/null; do
    if (( i >= max )); then
      error "$name did not become ready on port $port within $((max / 2)) s."
      return 1
    fi
    sleep 0.5; (( i++ )) || true
  done
}

# ── postgres — Docker mode ────────────────────────────────────────────────────

start_postgres_docker() {
  if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$'; then
    info "Postgres already running (Docker)."
    return
  fi
  info "Starting Postgres container…"
  JWT_SECRET="${JWT_SECRET:-dev-placeholder}" \
    docker-compose -f "$SCRIPT_DIR/docker-compose.yml" up -d postgres
  info "Waiting for Postgres to be healthy…"
  local i=0
  until [[ "$(docker inspect --format='{{.State.Health.Status}}' qalytix-postgres 2>/dev/null)" == "healthy" ]]; do
    if (( i >= 60 )); then
      error "Postgres container did not become healthy in time."
      exit 1
    fi
    sleep 0.5; (( i++ )) || true
  done
  success "Postgres ready (Docker)."
}

stop_postgres_docker() {
  if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$'; then
    info "Stopping Postgres container…"
    JWT_SECRET="${JWT_SECRET:-dev-placeholder}" \
      docker-compose -f "$SCRIPT_DIR/docker-compose.yml" stop postgres
    success "Postgres stopped."
  else
    warn "Postgres container is not running."
  fi
}

# ── postgres — local mode ─────────────────────────────────────────────────────

start_postgres_local() {
  # Just verify the local instance is reachable — we don't manage its lifecycle
  if pg_isready -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" &>/dev/null; then
    info "Local Postgres is reachable on :${DB_PORT}."
    return
  fi

  # Try to start via system service (Linux)
  if command -v pg_ctlcluster &>/dev/null; then
    info "Attempting to start PostgreSQL via pg_ctlcluster…"
    local cluster
    cluster=$(pg_lsclusters -h | awk 'NR==1{print $1, $2}')
    sudo pg_ctlcluster $cluster start 2>/dev/null || true
  elif command -v systemctl &>/dev/null && systemctl list-units --type=service | grep -q postgresql; then
    info "Attempting to start PostgreSQL via systemctl…"
    sudo systemctl start postgresql 2>/dev/null || true
  elif command -v brew &>/dev/null && brew services list | grep -q postgresql; then
    info "Attempting to start PostgreSQL via Homebrew…"
    brew services start postgresql@17 2>/dev/null || \
    brew services start postgresql   2>/dev/null || true
  fi

  # Final check
  local i=0
  until pg_isready -h localhost -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" &>/dev/null; do
    if (( i >= 20 )); then
      error "Local Postgres is not reachable on port $DB_PORT."
      error "Run './dev.sh setup-db' to create the database, or start PostgreSQL manually."
      exit 1
    fi
    sleep 0.5; (( i++ )) || true
  done
  success "Local Postgres is ready on :${DB_PORT}."
}

stop_postgres_local() {
  info "Local Postgres is managed by your system — not stopped by dev.sh."
  info "To stop it: sudo systemctl stop postgresql  (Linux)"
  info "            brew services stop postgresql   (macOS)"
}

# ── postgres dispatcher ───────────────────────────────────────────────────────

start_postgres() {
  if [[ "$DB_MODE" == "docker" ]]; then
    start_postgres_docker
  else
    start_postgres_local
  fi
}

stop_postgres() {
  if [[ "$DB_MODE" == "docker" ]]; then
    stop_postgres_docker
  else
    stop_postgres_local
  fi
}

# ── setup-db (local-only, first-time) ────────────────────────────────────────

cmd_setup_db() {
  info "Setting up local PostgreSQL database for Qalytix…"

  if ! command -v psql &>/dev/null; then
    error "psql not found. Install PostgreSQL first:"
    error "  Ubuntu/Debian : sudo apt install postgresql postgresql-contrib"
    error "  macOS (brew)  : brew install postgresql@17"
    error "  Fedora/RHEL   : sudo dnf install postgresql-server"
    exit 1
  fi

  # Detect superuser — try postgres, then current user
  local SU
  if sudo -u postgres psql -c '\q' &>/dev/null 2>&1; then
    SU="sudo -u postgres"
  else
    SU=""
  fi

  info "Creating user '${DB_USER}'…"
  $SU psql -c "CREATE USER ${DB_USER} WITH PASSWORD '${DB_PASS}';" 2>/dev/null \
    || warn "User '${DB_USER}' may already exist — continuing."

  info "Creating database '${DB_NAME}'…"
  $SU psql -c "CREATE DATABASE ${DB_NAME} OWNER ${DB_USER};" 2>/dev/null \
    || warn "Database '${DB_NAME}' may already exist — continuing."

  $SU psql -c "GRANT ALL PRIVILEGES ON DATABASE ${DB_NAME} TO ${DB_USER};" 2>/dev/null || true

  success "Database setup complete."
  success "You can now run: ./dev.sh start"
}

# ── backend ───────────────────────────────────────────────────────────────────

start_backend() {
  if is_running "$BACKEND_PID_FILE"; then
    warn "Backend is already running (pid $(cat "$BACKEND_PID_FILE"))."
    return
  fi
  info "Starting backend (Spring Boot) on :8081…"
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
  if wait_for_port 8081 "Backend" 120; then
    success "Backend ready — http://localhost:8081  (log: $BACKEND_LOG)"
  else
    error "Backend failed to start. Check $BACKEND_LOG"
    rm -f "$BACKEND_PID_FILE"
    exit 1
  fi
}

stop_backend() {
  if is_running "$BACKEND_PID_FILE"; then
    local pid
    pid=$(cat "$BACKEND_PID_FILE")
    info "Stopping backend (pid $pid)…"
    pkill -TERM -P "$pid" 2>/dev/null || true
    kill -TERM "$pid"     2>/dev/null || true
    local i=0
    while kill -0 "$pid" 2>/dev/null && (( i < 20 )); do
      sleep 0.5; (( i++ )) || true
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
  info "Postgres mode: ${DB_MODE}"
  start_postgres
  start_backend
  start_ui
  echo ""
  success "All services running."
  echo -e "  Frontend  →  ${CYAN}http://localhost:3000${RESET}"
  echo -e "  Backend   →  ${CYAN}http://localhost:8081${RESET}"
  echo -e "  Swagger   →  ${CYAN}http://localhost:8081/swagger-ui/index.html${RESET}"
  echo -e "  Logs dir  →  ${CYAN}$LOGS_DIR${RESET}"
  echo -e "  DB mode   →  ${CYAN}${DB_MODE}${RESET}"
}

cmd_stop() {
  stop_ui
  stop_backend
  stop_postgres
  success "All services stopped."
}

cmd_status() {
  echo ""
  echo -e "  DB mode   ${CYAN}${DB_MODE}${RESET}"

  # Postgres
  if [[ "$DB_MODE" == "docker" ]]; then
    if docker ps --format '{{.Names}}' | grep -q '^qalytix-postgres$' 2>/dev/null; then
      echo -e "  Postgres  ${GREEN}running${RESET}  (docker: qalytix-postgres)"
    else
      echo -e "  Postgres  ${RED}stopped${RESET}"
    fi
  else
    if pg_isready -h localhost -p "$DB_PORT" -q 2>/dev/null; then
      echo -e "  Postgres  ${GREEN}running${RESET}  (local :${DB_PORT})"
    else
      echo -e "  Postgres  ${RED}not reachable${RESET}  (local :${DB_PORT})"
    fi
  fi

  # Backend
  if is_running "$BACKEND_PID_FILE"; then
    echo -e "  Backend   ${GREEN}running${RESET}  (pid $(cat "$BACKEND_PID_FILE"))  :8081"
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
  start)    cmd_start   ;;
  stop)     cmd_stop    ;;
  restart)  cmd_stop; echo ""; cmd_start ;;
  status)   cmd_status  ;;
  logs)     cmd_logs    ;;
  setup-db) cmd_setup_db ;;
  *)
    echo ""
    echo "  Usage: $0 {start|stop|restart|status|logs|setup-db}"
    echo ""
    echo "  start      Start all services (Postgres + backend + frontend)"
    echo "  stop       Stop all services"
    echo "  restart    Restart all services"
    echo "  status     Show what is running"
    echo "  logs       Tail backend + frontend logs"
    echo "  setup-db   Create local PostgreSQL database + user (no Docker needed)"
    echo ""
    echo "  Postgres mode is auto-detected:"
    echo "    Docker available  → uses Docker container  (override: QALYTIX_DB=docker)"
    echo "    Docker absent     → uses local PostgreSQL   (override: QALYTIX_DB=local)"
    echo ""
    exit 1
    ;;
esac
