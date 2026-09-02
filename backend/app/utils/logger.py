"""
Capturo Professional Logging System
app/utils/logger.py

Intercepts and beautifies ALL log output:
  - Structlog (app events)
  - Uvicorn access/error logs
  - SQLAlchemy engine logs
  - Application startup/shutdown
"""

import logging
import sys
import os
import re
import time
from datetime import datetime, timezone
from typing import Any
import structlog


# ─────────────────────────────────────────────────────────────────────────────
# TERMINAL COLOR ENGINE (matches Capturo brand palette)
# ─────────────────────────────────────────────────────────────────────────────

class C:
    PURPLE       = "\033[38;2;123;47;190m"
    PURPLE_DARK  = "\033[38;2;74;18;128m"
    MAGENTA      = "\033[38;2;224;64;251m"
    VIOLET       = "\033[38;2;156;79;222m"
    MUTED        = "\033[38;2;179;157;219m"

    SUCCESS      = "\033[38;2;0;230;118m"
    WARNING      = "\033[38;2;255;179;0m"
    ERROR        = "\033[38;2;255;82;82m"
    INFO         = "\033[38;2;64;196;255m"
    WHITE        = "\033[38;2;255;255;255m"
    GRAY         = "\033[38;2;110;110;140m"
    DIM_GRAY     = "\033[38;2;70;70;90m"

    BG_PURPLE    = "\033[48;2;45;10;85m"
    BG_SUCCESS   = "\033[48;2;0;55;25m"
    BG_ERROR     = "\033[48;2;80;10;10m"
    BG_WARNING   = "\033[48;2;70;45;0m"
    BG_INFO      = "\033[48;2;0;40;70m"
    BG_SQL       = "\033[48;2;20;10;50m"
    BG_UVICORN   = "\033[48;2;10;30;60m"

    BOLD         = "\033[1m"
    DIM          = "\033[2m"
    ITALIC       = "\033[3m"
    RESET        = "\033[0m"

    @staticmethod
    def strip(text: str) -> str:
        return re.sub(r'\033\[[0-9;]*m', '', text)


# ─────────────────────────────────────────────────────────────────────────────
# SHARED HELPERS
# ─────────────────────────────────────────────────────────────────────────────

def _now() -> str:
    """Current time formatted as HH:MM:SS.mmm"""
    n = datetime.now()
    return f"{n.strftime('%H:%M:%S')}.{n.microsecond // 1000:03d}"

def _now_iso() -> str:
    """ISO-8601 UTC timestamp for structured log fields"""
    return datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.%f")[:-3] + "Z"

def _badge(text: str, bg: str, fg: str = C.WHITE) -> str:
    return f"{bg}{fg}{C.BOLD} {text} {C.RESET}"

def _level_badge(level: str) -> str:
    level = level.upper().strip()
    badges = {
        "DEBUG":    _badge(" DBG ", C.BG_SQL,     C.MUTED),
        "INFO":     _badge("INFO ", C.BG_INFO,    C.INFO),
        "SUCCESS":  _badge(" OK  ", C.BG_SUCCESS, C.SUCCESS),
        "WARNING":  _badge("WARN ", C.BG_WARNING, C.WARNING),
        "WARN":     _badge("WARN ", C.BG_WARNING, C.WARNING),
        "ERROR":    _badge(" ERR ", C.BG_ERROR,   C.ERROR),
        "CRITICAL": _badge("CRIT ", C.BG_ERROR,   C.ERROR),
        "SQL":      _badge(" SQL ", C.BG_SQL,      C.VIOLET),
        "HTTP":     _badge("HTTP ", C.BG_UVICORN,  C.INFO),
        "BOOT":     _badge("BOOT ", C.BG_PURPLE,   C.MAGENTA),
        "DOWN":     _badge("DOWN ", C.BG_ERROR,    C.ERROR),
        "RELOAD":   _badge(" ↺  ", C.BG_WARNING,  C.WARNING),
    }
    return badges.get(level, _badge(f"{level:<5}", C.BG_PURPLE, C.MUTED))

def _ts() -> str:
    return f"{C.DIM_GRAY}{_now()}{C.RESET}"

def _separator(char: str = "·", color: str = C.DIM_GRAY) -> str:
    return f"{color} {char} {C.RESET}"

SEP = _separator()


# ─────────────────────────────────────────────────────────────────────────────
# HTTP METHOD & STATUS COLORS
# ─────────────────────────────────────────────────────────────────────────────

def _method_color(method: str) -> str:
    colors = {
        "GET":    C.SUCCESS,
        "POST":   C.INFO,
        "PATCH":  C.WARNING,
        "PUT":    C.WARNING,
        "DELETE": C.ERROR,
        "HEAD":   C.MUTED,
        "OPTIONS":C.GRAY,
    }
    return colors.get(method.upper(), C.WHITE)

def _status_color(code: int) -> str:
    if code < 300:  return C.SUCCESS
    if code < 400:  return C.INFO
    if code < 500:  return C.WARNING
    return C.ERROR

def _status_icon(code: int) -> str:
    if code < 300:  return f"{C.SUCCESS}✔{C.RESET}"
    if code < 400:  return f"{C.INFO}→{C.RESET}"
    if code < 500:  return f"{C.WARNING}⚠{C.RESET}"
    return f"{C.ERROR}✘{C.RESET}"

def _duration_color(ms: float) -> str:
    if ms < 50:   return C.SUCCESS
    if ms < 200:  return C.INFO
    if ms < 500:  return C.WARNING
    return C.ERROR


# ─────────────────────────────────────────────────────────────────────────────
# UVICORN ACCESS LOG FORMATTER
# Format: 127.0.0.1:54321 - "GET /api/v1/creators HTTP/1.1" 200 OK
# ─────────────────────────────────────────────────────────────────────────────

ACCESS_RE = re.compile(
    r'(?P<client>\S+) - "(?P<method>[A-Z]+) (?P<path>\S+) HTTP/[\d.]+" (?P<status>\d+)'
)

def _format_access(msg: str) -> str | None:
    m = ACCESS_RE.search(msg)
    if not m:
        return None

    method  = m.group("method")
    path    = m.group("path")
    status  = int(m.group("status"))
    client  = m.group("client")

    # Split path and query string
    if "?" in path:
        path_clean, query = path.split("?", 1)
        query_str = f"{C.DIM_GRAY}?{query}{C.RESET}"
    else:
        path_clean, query_str = path, ""

    method_str  = f"{_method_color(method)}{C.BOLD}{method:<7}{C.RESET}"
    path_str    = f"{C.WHITE}{path_clean}{C.RESET}{query_str}"
    status_str  = f"{_status_color(status)}{C.BOLD}{status}{C.RESET}"
    icon        = _status_icon(status)
    client_str  = f"{C.DIM_GRAY}{client}{C.RESET}"

    return (
        f"  {_ts()}"
        f"  {_level_badge('HTTP')}"
        f"  {icon}"
        f"  {method_str}"
        f"  {path_str}"
        f"  {SEP}"
        f"  {status_str}"
        f"  {SEP}"
        f"  {client_str}"
    )


# ─────────────────────────────────────────────────────────────────────────────
# SQLAlchemy engine logs to suppress or syntax-highlight
# ─────────────────────────────────────────────────────────────────────────────

SQL_SUPPRESS = {
    "SELECT DATABASE()",
    "SELECT @@sql_mode",
    "SELECT @@lower_case_table_names",
    "BEGIN (implicit)",
    "ROLLBACK",
    "COMMIT",
}

SQL_SUPPRESS_RE = re.compile(
    r"(\[generated in|"
    r"\[cached since|"
    r"\[raw sql\]|"
    r"\(\)$)"
)

SQL_KEYWORD_RE = re.compile(
    r'\b(SELECT|INSERT|UPDATE|DELETE|CREATE|DROP|ALTER|FROM|WHERE|JOIN|'
    r'LEFT|RIGHT|INNER|OUTER|ON|AND|OR|NOT|IN|IS|NULL|ORDER BY|GROUP BY|'
    r'HAVING|LIMIT|OFFSET|SET|VALUES|INTO|TABLE|INDEX|UNIQUE|CONSTRAINT|'
    r'FOREIGN KEY|PRIMARY KEY|REFERENCES|CASCADE)\b',
    re.IGNORECASE
)

def _colorize_sql(sql: str) -> str:
    """Apply syntax highlighting to a SQL statement."""
    sql = sql.strip()
    def replace_kw(m):
        return f"{C.MAGENTA}{C.BOLD}{m.group(0).upper()}{C.RESET}"
    sql = SQL_KEYWORD_RE.sub(replace_kw, sql)
    sql = re.sub(r"(FROM|JOIN|INTO|UPDATE)\s+(\w+)",
                 lambda m: f"{m.group(1)} {C.VIOLET}{m.group(2)}{C.RESET}", sql)
    sql = re.sub(r"'[^']*'", lambda m: f"{C.SUCCESS}{m.group(0)}{C.RESET}", sql)
    sql = re.sub(r'\b(\d+)\b', lambda m: f"{C.INFO}{m.group(0)}{C.RESET}", sql)
    return sql

def _format_sql(msg: str) -> str | None:
    msg = msg.strip()

    for sup in SQL_SUPPRESS:
        if sup in msg:
            return None
    if SQL_SUPPRESS_RE.search(msg):
        return None
    if not msg or len(msg) < 5:
        return None

    display = msg[:180] + "…" if len(msg) > 180 else msg
    colored_sql = _colorize_sql(display)

    return (
        f"  {_ts()}"
        f"  {_level_badge('SQL')}"
        f"  {C.DIM_GRAY}❯{C.RESET}"
        f"  {colored_sql}"
    )


# ─────────────────────────────────────────────────────────────────────────────
# UVICORN SERVER LIFECYCLE FORMATTER
# ─────────────────────────────────────────────────────────────────────────────

LIFECYCLE_PATTERNS = {
    r"Uvicorn running on (http://\S+)": (
        "BOOT",
        lambda m: (
            f"  {_ts()}  {_level_badge('BOOT')}"
            f"  {C.SUCCESS}{C.BOLD}Server live{C.RESET}"
            f"  {SEP}"
            f"  {C.WHITE}{m.group(1)}{C.RESET}"
            f"  {SEP}"
            f"  {C.INFO}http://localhost:8000/docs{C.RESET}"
            f"  {C.MUTED}(Swagger){C.RESET}"
        )
    ),
    r"Press CTRL\+C to quit": (
        None, lambda m: None
    ),
    r"Started reloader process \[(\d+)\]": (
        "BOOT",
        lambda m: (
            f"  {_ts()}  {_level_badge('BOOT')}"
            f"  {C.MUTED}File watcher started{C.RESET}"
            f"  {SEP}"
            f"  {C.DIM_GRAY}pid {m.group(1)}{C.RESET}"
        )
    ),
    r"Started server process \[(\d+)\]": (
        "BOOT",
        lambda m: (
            f"  {_ts()}  {_level_badge('BOOT')}"
            f"  {C.SUCCESS}Worker process started{C.RESET}"
            f"  {SEP}"
            f"  {C.DIM_GRAY}pid {m.group(1)}{C.RESET}"
        )
    ),
    r"Waiting for application startup": (
        "BOOT",
        lambda m: (
            f"  {_ts()}  {_level_badge('BOOT')}"
            f"  {C.MUTED}Initializing application…{C.RESET}"
        )
    ),
    r"Application startup complete": (
        "BOOT",
        lambda m: (
            f"  {_ts()}  {_level_badge('BOOT')}"
            f"  {C.SUCCESS}{C.BOLD}✔  Application ready{C.RESET}"
            f"  {SEP}"
            f"  {C.DIM_GRAY}All startup hooks completed{C.RESET}"
        )
    ),
    r"WatchFiles detected changes in '(.+?)'": (
        "RELOAD",
        lambda m: (
            f"  {_ts()}  {_level_badge('RELOAD')}"
            f"  {C.WARNING}File changed — reloading{C.RESET}"
            f"  {SEP}"
            f"  {C.MUTED}{m.group(1)}{C.RESET}"
        )
    ),
    r"Shutting down": (
        "DOWN",
        lambda m: (
            f"  {_ts()}  {_level_badge('DOWN')}"
            f"  {C.ERROR}Server shutting down{C.RESET}"
        )
    ),
    r"Waiting for application shutdown": (
        "DOWN",
        lambda m: (
            f"  {_ts()}  {_level_badge('DOWN')}"
            f"  {C.MUTED}Running shutdown hooks…{C.RESET}"
        )
    ),
    r"Application shutdown complete": (
        "DOWN",
        lambda m: (
            f"  {_ts()}  {_level_badge('DOWN')}"
            f"  {C.WARNING}✔  Application stopped cleanly{C.RESET}"
        )
    ),
    r"Finished server process \[(\d+)\]": (
        "DOWN",
        lambda m: (
            f"  {_ts()}  {_level_badge('DOWN')}"
            f"  {C.DIM_GRAY}Worker process exited{C.RESET}"
            f"  {SEP}"
            f"  {C.DIM_GRAY}pid {m.group(1)}{C.RESET}"
        )
    ),
    r"Will watch for changes in these directories": (
        None, lambda m: None
    ),
}

def _format_uvicorn_lifecycle(msg: str) -> str | None:
    for pattern, (_, formatter) in LIFECYCLE_PATTERNS.items():
        m = re.search(pattern, msg)
        if m:
            return formatter(m)
    return None


# ─────────────────────────────────────────────────────────────────────────────
# STRUCTLOG PROCESSOR — App events
# ─────────────────────────────────────────────────────────────────────────────

EVENT_STYLES: dict[str, tuple[str, str]] = {
    "capturo_api_startup":     ("🚀", C.SUCCESS),
    "capturo_api_shutdown":    ("🛑", C.WARNING),
    "db_connection_verified":  ("🗄 ", C.SUCCESS),
    "user_registered":         ("👤", C.SUCCESS),
    "user_login":              ("🔑", C.INFO),
    "user_logout":             ("🔒", C.MUTED),
    "token_refresh":           ("🔄", C.INFO),
    "login_failed":            ("⛔", C.ERROR),
    "booking_created":         ("📅", C.INFO),
    "booking_confirmed":       ("✅", C.SUCCESS),
    "booking_cancelled":       ("❌", C.WARNING),
    "booking_completed":       ("🎉", C.SUCCESS),
    "booking_disputed":        ("⚠ ", C.ERROR),
    "availability_checked":    ("🔍", C.MUTED),
    "payment_order_created":   ("💳", C.INFO),
    "payment_captured":        ("💰", C.SUCCESS),
    "payment_failed":          ("💸", C.ERROR),
    "refund_initiated":        ("↩ ", C.WARNING),
    "webhook_received":        ("📨", C.INFO),
    "fcm_sent":                ("🔔", C.SUCCESS),
    "fcm_failed":              ("🔕", C.WARNING),
    "notification_created":    ("📣", C.INFO),
    "file_uploaded":           ("📤", C.SUCCESS),
    "file_upload_failed":      ("📛", C.ERROR),
    "thumbnail_generated":     ("🖼 ", C.MUTED),
    "creator_profile_created": ("📷", C.SUCCESS),
    "nearby_search":           ("📍", C.INFO),
    "http_request":            ("→ ", C.INFO),
    "rate_limit_exceeded":     ("🚫", C.WARNING),
}

def _format_extra_fields(event_dict: dict) -> str:
    skip_keys = {"event", "level", "timestamp", "_record"}
    parts = []
    for k, v in event_dict.items():
        if k in skip_keys:
            continue
        key_str = f"{C.MUTED}{k}{C.RESET}"
        if isinstance(v, bool):
            val_str = f"{C.SUCCESS if v else C.ERROR}{str(v)}{C.RESET}"
        elif isinstance(v, (int, float)):
            val_str = f"{C.INFO}{v}{C.RESET}"
        elif isinstance(v, str) and v.startswith("http"):
            val_str = f"{C.INFO}{v}{C.RESET}"
        elif isinstance(v, str) and len(v) == 36 and v.count("-") == 4:
            val_str = f"{C.DIM_GRAY}{v}{C.RESET}"
        else:
            val_str = f"{C.WHITE}{v}{C.RESET}"
        parts.append(f"{key_str}{C.DIM_GRAY}={C.RESET}{val_str}")
    if not parts:
        return ""
    return f"  {C.DIM_GRAY}│{C.RESET}  " + f"  {C.DIM_GRAY}·{C.RESET}  ".join(parts)


class CapturoConsoleRenderer:
    def __call__(self, logger: Any, method: str, event_dict: dict) -> str:
        event   = event_dict.get("event", "")
        level   = event_dict.get("level", method).upper()
        ts_raw  = event_dict.get("timestamp", _now())

        try:
            if "T" in str(ts_raw):
                dt = datetime.fromisoformat(str(ts_raw).replace("Z", "+00:00"))
                ts_str = dt.strftime("%H:%M:%S") + f".{dt.microsecond // 1000:03d}"
            else:
                ts_str = str(ts_raw)[:12]
        except Exception:
            ts_str = _now()

        ts_display = f"{C.DIM_GRAY}{ts_str}{C.RESET}"
        icon, event_color = EVENT_STYLES.get(event, ("◈ ", C.WHITE))
        badge = _level_badge(level)
        event_str = f"{event_color}{C.BOLD}{icon}  {event}{C.RESET}"
        extras = _format_extra_fields(event_dict)

        return f"  {ts_display}  {badge}  {event_str}{extras}"


# ─────────────────────────────────────────────────────────────────────────────
# STANDARD LOGGING HANDLER — intercepts uvicorn / sqlalchemy / root loggers
# ─────────────────────────────────────────────────────────────────────────────

class CapturoHandler(logging.Handler):
    def emit(self, record: logging.LogRecord) -> None:
        try:
            msg = record.getMessage().strip()
            name = record.name
            level = record.levelname

            if name == "sqlalchemy.engine.Engine":
                formatted = _format_sql(msg)
                if formatted:
                    print(formatted, flush=True)
                return

            if name == "uvicorn.access":
                formatted = _format_access(msg)
                if formatted:
                    print(formatted, flush=True)
                return

            if name in ("uvicorn", "uvicorn.error"):
                formatted = _format_uvicorn_lifecycle(msg)
                if formatted:
                    print(formatted, flush=True)
                    return
                formatted = _format_access(msg)
                if formatted:
                    print(formatted, flush=True)
                    return
                if msg.strip():
                    badge = _level_badge(level)
                    print(
                        f"  {_ts()}  {badge}"
                        f"  {C.MUTED}[uvicorn]{C.RESET}"
                        f"  {C.WHITE}{msg}{C.RESET}",
                        flush=True
                    )
                return

            if name == "watchfiles.main":
                formatted = _format_uvicorn_lifecycle(msg)
                if formatted:
                    print(formatted, flush=True)
                return

            if level in ("WARNING", "ERROR", "CRITICAL"):
                badge = _level_badge(level)
                color = C.WARNING if level == "WARNING" else C.ERROR
                src   = f"{C.DIM_GRAY}[{name}]{C.RESET}"
                print(
                    f"  {_ts()}  {badge}  {src}  {color}{msg}{C.RESET}",
                    flush=True
                )

        except Exception:
            pass


# ─────────────────────────────────────────────────────────────────────────────
# STARTUP BANNER printed when server boots
# ─────────────────────────────────────────────────────────────────────────────

def print_startup_banner(host: str, port: int, version: str, debug: bool) -> None:
    w = 72
    bar  = f"{C.PURPLE}{'═' * w}{C.RESET}"
    thin = f"{C.PURPLE_DARK}{'─' * w}{C.RESET}"

    def row(label: str, value: str, val_color: str = C.WHITE) -> None:
        lbl = f"{C.MUTED}  {label:<18}{C.RESET}"
        val = f"{val_color}{value}{C.RESET}"
        print(f"  {lbl}  {val}")

    print()
    print(bar)
    print(
        f"  {C.PURPLE}{C.BOLD}  ●  {C.RESET}"
        f"{C.MAGENTA}{C.BOLD}CAPTURO API{C.RESET}"
        f"  {C.DIM_GRAY}—{C.RESET}"
        f"  {C.MUTED}Photography & Videography Platform{C.RESET}"
        f"  {C.BG_PURPLE}{C.MAGENTA}{C.BOLD}  v{version}  {C.RESET}"
    )
    print(thin)
    row("Endpoint",    f"http://{host}:{port}",           C.WHITE)
    row("Swagger UI",  f"http://{host}:{port}/docs",      C.INFO)
    row("Health",      f"http://{host}:{port}/health",    C.SUCCESS)
    row("Mode",
        "Development  (--reload enabled)" if debug else "Production",
        C.WARNING if debug else C.SUCCESS)
    row("Started at",  datetime.now().strftime("%Y-%m-%d  %H:%M:%S"), C.MUTED)
    print(thin)
    print(
        f"  {C.DIM_GRAY}  Press  "
        f"{C.BOLD}{C.WHITE}Ctrl+C{C.RESET}"
        f"{C.DIM_GRAY}  to stop the server{C.RESET}"
    )
    print(bar)
    print()


# ─────────────────────────────────────────────────────────────────────────────
# SECTION DIVIDER 
# ─────────────────────────────────────────────────────────────────────────────

def print_reload_divider(changed_file: str = "") -> None:
    ts  = datetime.now().strftime("%H:%M:%S")
    msg = f"  ↺  RELOADING"
    if changed_file:
        msg += f"  ·  {changed_file}"
    msg += f"  ·  {ts}"
    w   = 72
    pad = max(0, w - len(C.strip(msg)) - 4)
    print()
    print(
        f"  {C.BG_WARNING}{C.WHITE}{C.BOLD}"
        f"{msg}{'─' * pad}"
        f"{C.RESET}"
    )
    print()


# ─────────────────────────────────────────────────────────────────────────────
# SETUP FUNCTION
# ─────────────────────────────────────────────────────────────────────────────

def setup_logging(debug: bool = False) -> None:
    structlog.configure(
        processors=[
            structlog.contextvars.merge_contextvars,
            structlog.processors.add_log_level,
            structlog.processors.TimeStamper(fmt="iso", utc=True),
            CapturoConsoleRenderer(),
        ],
        context_class=dict,
        logger_factory=structlog.PrintLoggerFactory(sys.stdout),
        cache_logger_on_first_use=True,
    )

    capturo_handler = CapturoHandler()
    capturo_handler.setLevel(logging.DEBUG if debug else logging.INFO)

    root = logging.getLogger()
    root.handlers.clear()
    root.addHandler(capturo_handler)
    root.setLevel(logging.DEBUG if debug else logging.INFO)

    for name in ("uvicorn", "uvicorn.error", "uvicorn.access"):
        lg = logging.getLogger(name)
        lg.handlers.clear()
        lg.addHandler(capturo_handler)
        lg.propagate = False
        lg.setLevel(logging.DEBUG if debug else logging.INFO)

    sa_level = logging.DEBUG if debug else logging.WARNING
    for name in ("sqlalchemy.engine.Engine", "sqlalchemy.engine", "sqlalchemy"):
        lg = logging.getLogger(name)
        lg.handlers.clear()
        lg.addHandler(capturo_handler)
        lg.propagate = False
        lg.setLevel(sa_level)

    for name in ("watchfiles.main", "watchfiles"):
        lg = logging.getLogger(name)
        lg.handlers.clear()
        lg.addHandler(capturo_handler)
        lg.propagate = False

    for name in ("multipart", "passlib", "jose", "urllib3", "httpcore", "httpx"):
        logging.getLogger(name).setLevel(logging.WARNING)


# ─────────────────────────────────────────────────────────────────────────────
# CONVENIENCE LOGGER
# ─────────────────────────────────────────────────────────────────────────────

logger = structlog.get_logger("capturo")

def get_logger(name: str = "capturo") -> Any:
    return structlog.get_logger(name)
