#!/usr/bin/env python3
"""
Capturo Backend Orchestrator Engine
Professional CLI for managing the Capturo Photography Platform Backend.
Stack: Python · FastAPI · SQLAlchemy · MySQL · Alembic
"""

import os
import sys
import subprocess
import socket
import argparse
import time
import shutil
import platform
from datetime import datetime
from typing import List, Optional, Tuple

# Force UTF-8 encoding on Windows to prevent UnicodeEncodeError in PowerShell/CMD
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8', errors='replace')
    sys.stderr = io.TextIOWrapper(sys.stderr.buffer, encoding='utf-8', errors='replace')

# ─────────────────────────────────────────────────────────────────────────────
# TERMINAL COLOR & STYLE ENGINE
# ─────────────────────────────────────────────────────────────────────────────

class C:
    """Capturo Brand Terminal Color Palette"""
    # Brand Colors
    PURPLE       = "\033[38;2;123;47;190m"    # #7B2FBE — Primary brand
    PURPLE_DARK  = "\033[38;2;74;18;128m"     # #4A1280 — Dark purple
    MAGENTA      = "\033[38;2;224;64;251m"    # #E040FB — Accent magenta
    VIOLET       = "\033[38;2;156;79;222m"    # #9C4FDE — Mid purple

    # Semantic Colors
    SUCCESS      = "\033[38;2;0;230;118m"     # #00E676 — Green
    WARNING      = "\033[38;2;255;179;0m"     # #FFB300 — Amber
    ERROR        = "\033[38;2;255;82;82m"     # #FF5252 — Red
    INFO         = "\033[38;2;64;196;255m"    # #40C4FF — Blue
    MUTED        = "\033[38;2;179;157;219m"   # #B39DDB — Light purple-gray

    # Neutral
    WHITE        = "\033[38;2;255;255;255m"
    GRAY         = "\033[38;2;100;100;130m"
    DIM_GRAY     = "\033[38;2;70;70;90m"
    DIM          = "\033[2m"

    # Backgrounds
    BG_PURPLE    = "\033[48;2;61;20;115m"     # #3D1473
    BG_DARK      = "\033[48;2;26;0;51m"       # #1A0033
    BG_SUCCESS   = "\033[48;2;0;78;40m"
    BG_ERROR     = "\033[48;2;80;0;0m"
    BG_WARNING   = "\033[48;2;80;55;0m"
    BG_INFO      = "\033[48;2;0;50;80m"

    # Text Styles
    BOLD         = "\033[1m"
    DIM          = "\033[2m"
    ITALIC       = "\033[3m"
    UNDERLINE    = "\033[4m"
    RESET        = "\033[0m"


# Detect terminal width
TERM_WIDTH = min(shutil.get_terminal_size((100, 24)).columns, 110)


# ─────────────────────────────────────────────────────────────────────────────
# DRAWING PRIMITIVES
# ─────────────────────────────────────────────────────────────────────────────

def _strip_ansi(text: str) -> str:
    """Strip ANSI codes to get real visible length."""
    import re
    return re.sub(r'\033\[[0-9;]*m', '', text)

def _pad(text: str, width: int, char: str = " ") -> str:
    visible = len(_strip_ansi(text))
    return text + char * max(0, width - visible)

def hr(char: str = "─", color: str = C.PURPLE) -> str:
    return f"{color}{char * TERM_WIDTH}{C.RESET}"

def hr_thin(char: str = "·") -> str:
    return f"{C.GRAY}{char * TERM_WIDTH}{C.RESET}"

def section_header(title: str, subtitle: str = "", icon: str = "◈") -> None:
    print()
    print(hr("═"))
    title_line = f"{C.BOLD}{C.MAGENTA}{icon}  {title.upper()}{C.RESET}"
    if subtitle:
        title_line += f"  {C.MUTED}{C.ITALIC}{subtitle}{C.RESET}"
    print(f"  {title_line}")
    print(hr("─", C.PURPLE_DARK))

def panel(lines: List[str], style: str = "default", title: str = "") -> None:
    """Draw a bordered panel with content lines."""
    styles = {
        "default": (C.PURPLE,       C.BG_DARK,    "┌", "─", "┐", "│", "└", "┘"),
        "success": (C.SUCCESS,      C.BG_SUCCESS,  "╔", "═", "╗", "║", "╚", "╝"),
        "error":   (C.ERROR,        C.BG_ERROR,    "╔", "═", "╗", "║", "╚", "╝"),
        "warning": (C.WARNING,      C.BG_WARNING,  "┌", "─", "┐", "│", "└", "┘"),
        "info":    (C.INFO,         C.BG_INFO,     "┌", "─", "┐", "│", "└", "┘"),
        "brand":   (C.MAGENTA,      C.BG_DARK,     "╔", "═", "╗", "║", "╚", "╝"),
    }
    bc, bg, tl, th, tr, ml, bl, br = styles.get(style, styles["default"])
    inner_w = TERM_WIDTH - 4

    top_bar = th * inner_w
    if title:
        label = f" {title} "
        pad_l = (inner_w - len(_strip_ansi(label))) // 2
        pad_r = inner_w - len(_strip_ansi(label)) - pad_l
        top_bar = th * pad_l + label + th * pad_r

    print(f"  {bc}{tl}{top_bar}{tr}{C.RESET}")
    for line in lines:
        print(f"  {bc}{ml}{C.RESET} {_pad(line, inner_w - 2)} {bc}{ml}{C.RESET}")
    print(f"  {bc}{bl}{th * inner_w}{br}{C.RESET}")


# ─────────────────────────────────────────────────────────────────────────────
# LOGGING ENGINE
# ─────────────────────────────────────────────────────────────────────────────

class Log:
    @staticmethod
    def _timestamp() -> str:
        return f"{C.GRAY}{C.DIM}{datetime.now().strftime('%H:%M:%S')}{C.RESET}"

    @staticmethod
    def info(msg: str, detail: str = "") -> None:
        ts = Log._timestamp()
        badge = f"{C.BG_INFO}{C.WHITE}{C.BOLD}  INFO  {C.RESET}"
        line = f"{ts}  {badge}  {C.WHITE}{msg}{C.RESET}"
        if detail:
            line += f"  {C.MUTED}{C.ITALIC}({detail}){C.RESET}"
        print(line)

    @staticmethod
    def success(msg: str, detail: str = "") -> None:
        ts = Log._timestamp()
        badge = f"{C.BG_SUCCESS}{C.WHITE}{C.BOLD}  OK   {C.RESET}"
        line = f"{ts}  {badge}  {C.SUCCESS}{C.BOLD}{msg}{C.RESET}"
        if detail:
            line += f"  {C.MUTED}{detail}{C.RESET}"
        print(line)

    @staticmethod
    def warn(msg: str, detail: str = "") -> None:
        ts = Log._timestamp()
        badge = f"{C.BG_WARNING}{C.WHITE}{C.BOLD}  WARN {C.RESET}"
        line = f"{ts}  {badge}  {C.WARNING}{msg}{C.RESET}"
        if detail:
            line += f"  {C.MUTED}{detail}{C.RESET}"
        print(line)

    @staticmethod
    def error(msg: str, detail: str = "") -> None:
        ts = Log._timestamp()
        badge = f"{C.BG_ERROR}{C.WHITE}{C.BOLD} ERROR {C.RESET}"
        line = f"{ts}  {badge}  {C.ERROR}{C.BOLD}{msg}{C.RESET}"
        if detail:
            line += f"  {C.MUTED}{detail}{C.RESET}"
        print(line)

    @staticmethod
    def step(n: int, total: int, msg: str) -> None:
        ts = Log._timestamp()
        counter = f"{C.BG_PURPLE}{C.MAGENTA}{C.BOLD} {n:02d}/{total:02d} {C.RESET}"
        print(f"{ts}  {counter}  {C.WHITE}{msg}{C.RESET}")

    @staticmethod
    def cmd(command: List[str]) -> None:
        cmd_str = " ".join(command)
        print(f"  {C.GRAY}  ❯  {C.PURPLE}{C.ITALIC}{cmd_str}{C.RESET}")

    @staticmethod
    def blank() -> None:
        print()


# ─────────────────────────────────────────────────────────────────────────────
# SPINNER / PROGRESS
# ─────────────────────────────────────────────────────────────────────────────

class Spinner:
    FRAMES = ["⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏"]

    def __init__(self, message: str):
        self.message = message
        self._running = False
        self._thread = None

    def __enter__(self):
        import threading
        self._running = True
        def spin():
            i = 0
            while self._running:
                frame = self.FRAMES[i % len(self.FRAMES)]
                print(f"\r  {C.MAGENTA}{frame}{C.RESET}  {C.WHITE}{self.message}...{C.RESET}  ", end="", flush=True)
                time.sleep(0.08)
                i += 1
        self._thread = threading.Thread(target=spin, daemon=True)
        self._thread.start()
        return self

    def __exit__(self, *args):
        self._running = False
        if self._thread:
            self._thread.join()
        print("\r" + " " * (TERM_WIDTH - 2) + "\r", end="", flush=True)


# ─────────────────────────────────────────────────────────────────────────────
# BANNER
# ─────────────────────────────────────────────────────────────────────────────

BANNER_LINES = [
    f"{C.PURPLE}{C.BOLD}   ██████╗ █████╗ ██████╗ ████████╗██╗   ██╗██████╗  ██████╗ {C.RESET}",
    f"{C.PURPLE}{C.BOLD}  ██╔════╝██╔══██╗██╔══██╗╚══██╔══╝██║   ██║██╔══██╗██╔═══██╗{C.RESET}",
    f"{C.MAGENTA}{C.BOLD}  ██║     ███████║██████╔╝   ██║   ██║   ██║██████╔╝██║   ██║{C.RESET}",
    f"{C.VIOLET}{C.BOLD}  ██║     ██╔══██║██╔═══╝    ██║   ██║   ██║██╔══██╗██║   ██║{C.RESET}",
    f"{C.PURPLE_DARK}{C.BOLD}  ╚██████╗██║  ██║██║        ██║   ╚██████╔╝██║  ██║╚██████╔╝{C.RESET}",
    f"{C.PURPLE_DARK}{C.BOLD}   ╚═════╝╚═╝  ╚═╝╚═╝        ╚═╝    ╚═════╝ ╚═╝  ╚═╝ ╚═════╝ {C.RESET}",
]

def print_banner() -> None:
    print()
    print(hr("═"))
    print()
    for line in BANNER_LINES:
        center_pad = max(0, (TERM_WIDTH - 62) // 2)
        print(" " * center_pad + line)
    print()
    # Tagline row
    tagline   = f"{C.MUTED}{C.ITALIC}Photography & Videography Booking Platform{C.RESET}"
    version   = f"{C.BG_PURPLE}{C.MAGENTA}{C.BOLD}  v1.0.0  {C.RESET}"
    env_label = f"{C.BG_DARK}{C.MUTED}  Python · FastAPI · MySQL · Alembic  {C.RESET}"
    print(f"  {tagline}    {version}  {env_label}")
    print()
    print(hr("─", C.PURPLE_DARK))

    # Quick system info strip
    py_ver  = platform.python_version()
    os_name = platform.system()
    cwd     = os.path.basename(os.getcwd())
    ts      = datetime.now().strftime("%A, %d %b %Y · %H:%M:%S")
    print(
        f"  {C.GRAY}Python {py_ver}{C.RESET}"
        f"  {C.GRAY}│{C.RESET}  "
        f"{C.GRAY}{os_name}{C.RESET}"
        f"  {C.GRAY}│{C.RESET}  "
        f"{C.GRAY}cwd: {C.PURPLE}{cwd}{C.RESET}"
        f"  {C.GRAY}│{C.RESET}  "
        f"{C.GRAY}{ts}{C.RESET}"
    )
    print(hr("─", C.PURPLE_DARK))
    print()


# ─────────────────────────────────────────────────────────────────────────────
# UTILITIES
# ─────────────────────────────────────────────────────────────────────────────

def is_port_open(port: int, host: str = "127.0.0.1") -> bool:
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.settimeout(1)
        return s.connect_ex((host, port)) == 0

def check_binary(name: str) -> Optional[str]:
    return shutil.which(name)

def run_command(command: List[str], description: str) -> Tuple[bool, int]:
    """Execute a subprocess with live output streaming."""
    print()
    print(f"  {C.PURPLE}╭─{C.RESET} {C.BOLD}{C.WHITE}{description}{C.RESET}")
    Log.cmd(command)
    print(f"  {C.PURPLE}╰─{C.RESET} {C.MUTED}{'─' * (TERM_WIDTH - 6)}{C.RESET}")
    print()

    try:
        process = subprocess.Popen(
            command,
            shell=(os.name == 'nt'),
            stdout=sys.stdout,
            stderr=sys.stderr
        )
        process.communicate()
        rc = process.returncode
        print()
        print(hr_thin())
        if rc == 0:
            Log.success(f"Completed: {description}", f"exit code {rc}")
        else:
            Log.error(f"Failed: {description}", f"exit code {rc}")
        return rc == 0, rc
    except FileNotFoundError as e:
        Log.error(f"Command not found", str(e))
        return False, 127
    except Exception as e:
        Log.error(f"Unexpected error during: {description}", str(e))
        return False, 1


# ─────────────────────────────────────────────────────────────────────────────
# ENVIRONMENT CHECK
# ─────────────────────────────────────────────────────────────────────────────

def check_environment() -> bool:
    section_header("Environment Diagnostics", "Validating runtime configuration", "⚙")

    all_passed = True
    checks = []

    # 1 — .env file
    if os.path.exists(".env"):
        checks.append((True, ".env config file", "Found"))
    elif os.path.exists(".env.example"):
        Log.warn(".env not found — generating from .env.example template")
        try:
            shutil.copy(".env.example", ".env")
            Log.success("Generated .env", "Review credentials before starting server")
            checks.append((True, ".env config file", "Auto-generated from .env.example"))
        except Exception as e:
            checks.append((False, ".env config file", f"Copy failed: {e}"))
            all_passed = False
    else:
        checks.append((False, ".env config file", "Missing — create .env manually"))
        all_passed = False

    # 2 — Virtual environment
    in_venv = hasattr(sys, 'real_prefix') or sys.base_prefix != sys.prefix
    checks.append((in_venv, "Virtual environment", "Active" if in_venv else "Not detected — using global Python"))
    if not in_venv:
        Log.warn("No virtual environment active", "Global Python packages will be used")

    # 3 — Required binaries
    for binary in ["uvicorn", "alembic", "pytest"]:
        path = check_binary(binary)
        checks.append((path is not None, f"Binary: {binary}", path or "NOT FOUND — run: pip install " + binary))
        if not path:
            all_passed = False

    # 4 — Core packages
    pkg_checks = [
        ("fastapi",    "fastapi"),
        ("sqlalchemy", "sqlalchemy"),
        ("alembic",    "alembic"),
        ("pymysql",    "pymysql"),
        ("pydantic",   "pydantic"),
    ]
    for import_name, pkg_name in pkg_checks:
        try:
            mod = __import__(import_name)
            ver = getattr(mod, "__version__", "installed")
            checks.append((True, f"Package: {pkg_name}", ver))
        except ImportError:
            checks.append((False, f"Package: {pkg_name}", "NOT INSTALLED — pip install " + pkg_name))
            all_passed = False

    # 5 — Migrations folder
    alembic_ok = os.path.exists("alembic") and os.path.exists("alembic.ini")
    checks.append((alembic_ok, "Alembic migrations folder", "Configured" if alembic_ok else "Missing alembic/ or alembic.ini"))
    if not alembic_ok:
        all_passed = False

    # 6 — Uploads directory
    uploads_ok = os.path.isdir("uploads")
    if not uploads_ok:
        try:
            os.makedirs("uploads", exist_ok=True)
            uploads_ok = True
            Log.info("Created missing uploads/ directory")
        except Exception:
            pass
    checks.append((uploads_ok, "Uploads directory", "Ready" if uploads_ok else "Cannot create"))

    # — Render results table —
    Log.blank()
    col_status = 8
    col_item   = 30
    col_detail = TERM_WIDTH - col_status - col_item - 12

    header = (
        f"  {C.BG_PURPLE}{C.WHITE}{C.BOLD}"
        f"  {'STATUS':<{col_status}}  {'CHECK':<{col_item}}  {'DETAIL':<{col_detail}}  "
        f"{C.RESET}"
    )
    print(header)
    print(f"  {C.PURPLE}{'─' * (TERM_WIDTH - 4)}{C.RESET}")

    for ok, item, detail in checks:
        icon   = f"{C.SUCCESS}  ✔  {C.RESET}" if ok else f"{C.ERROR}  ✘  {C.RESET}"
        status = f"{C.SUCCESS}PASS{C.RESET}  " if ok else f"{C.ERROR}FAIL{C.RESET}  "
        item_s = f"{C.WHITE}{item:<{col_item}}{C.RESET}"
        det_s  = f"{C.MUTED}{detail:<{col_detail}}{C.RESET}"
        print(f"  {icon}  {status}  {item_s}  {det_s}")

    print(f"  {C.PURPLE}{'─' * (TERM_WIDTH - 4)}{C.RESET}")
    Log.blank()

    if all_passed:
        Log.success("All environment checks passed", "Ready to operate")
    else:
        Log.error("Some environment checks failed", "Resolve issues above before proceeding")

    return all_passed


# ─────────────────────────────────────────────────────────────────────────────
# COMMANDS
# ─────────────────────────────────────────────────────────────────────────────

def command_server(port: int, host: str) -> None:
    section_header("Starting Server", f"FastAPI · Uvicorn · {host}:{port}", "🚀")

    if is_port_open(port, "127.0.0.1"):
        panel([
            f"{C.ERROR}{C.BOLD}Port {port} is already in use.{C.RESET}",
            "",
            f"  {C.MUTED}Another process is listening on {host}:{port}.{C.RESET}",
            f"  {C.MUTED}Either stop that process or start on a different port:{C.RESET}",
            "",
            f"  {C.ITALIC}{C.PURPLE}python capturo.py start --port 8001{C.RESET}",
        ], style="error", title=" PORT CONFLICT ")
        sys.exit(1)

    # Output startup banner matching the app logger exactly
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
        f"  {C.BG_PURPLE}{C.MAGENTA}{C.BOLD}  v1.0.0  {C.RESET}"
    )
    print(thin)
    row("Endpoint",    f"http://{host}:{port}",           C.WHITE)
    row("Swagger UI",  f"http://{host}:{port}/docs",      C.INFO)
    row("Health",      f"http://{host}:{port}/health",    C.SUCCESS)
    row("Mode",        "Development  (--reload enabled)", C.WARNING)
    row("Started at",  datetime.now().strftime("%Y-%m-%d  %H:%M:%S"), C.MUTED)
    print(thin)
    print(
        f"  {C.DIM_GRAY}  Press  "
        f"{C.BOLD}{C.WHITE}Ctrl+C{C.RESET}"
        f"{C.DIM_GRAY}  to stop the server{C.RESET}"
    )
    print(bar)
    print()

    cmd = ["uvicorn", "app.main:app", "--reload", "--host", host, "--port", str(port),
           "--log-level", "info"]
    run_command(cmd, "Uvicorn ASGI Development Server")


def command_migrate() -> None:
    section_header("Database Migrations", "Alembic · MySQL · XAMPP", "🗄")

    Log.info("Checking pending Alembic migrations...")
    Log.blank()

    panel([
        f"  {C.MUTED}Tool       {C.RESET}  {C.WHITE}Alembic (SQLAlchemy migrations){C.RESET}",
        f"  {C.MUTED}Target     {C.RESET}  {C.WHITE}HEAD (latest revision){C.RESET}",
        f"  {C.MUTED}Driver     {C.RESET}  {C.WHITE}PyMySQL → MySQL 8.x (XAMPP){C.RESET}",
        f"  {C.MUTED}Config     {C.RESET}  {C.INFO}alembic.ini{C.RESET}",
    ], style="info", title=" MIGRATION CONTEXT ")

    ok, _ = run_command(["alembic", "upgrade", "head"], "Apply all pending Alembic migrations")

    if ok:
        panel([
            f"  {C.SUCCESS}{C.BOLD}All migrations applied successfully.{C.RESET}",
            f"  {C.MUTED}Your database schema is now at HEAD revision.{C.RESET}",
        ], style="success", title=" MIGRATIONS COMPLETE ")
    else:
        panel([
            f"  {C.ERROR}{C.BOLD}Migration failed.{C.RESET}",
            "",
            f"  {C.MUTED}Common causes:{C.RESET}",
            f"  {C.MUTED}  ·  XAMPP MySQL is not running (start via XAMPP Control Panel){C.RESET}",
            f"  {C.MUTED}  ·  Invalid DATABASE_URL in .env file{C.RESET}",
            f"  {C.MUTED}  ·  Database 'capturo_db' does not exist yet{C.RESET}",
            "",
            f"  {C.ITALIC}{C.YELLOW}Create the DB first: mysql -u root -e \"CREATE DATABASE capturo_db;\"{C.RESET}",
        ], style="error", title=" MIGRATION ERROR ")


def command_seed() -> None:
    section_header("Database Seeder", "Populating development data", "🌱")

    seed_paths = [
        os.path.join("scripts", "seed_db.py"),
        os.path.join("scripts", "seed_data.py"),
        "seed.py",
    ]
    seed_script = next((p for p in seed_paths if os.path.exists(p)), None)

    if not seed_script:
        panel([
            f"  {C.ERROR}{C.BOLD}Seeder script not found.{C.RESET}",
            "",
            f"  {C.MUTED}Searched locations:{C.RESET}",
            *[f"  {C.MUTED}  ·  {p}{C.RESET}" for p in seed_paths],
            "",
            f"  {C.MUTED}Create your seeder at:  {C.WHITE}scripts/seed_db.py{C.RESET}",
        ], style="error", title=" SEEDER NOT FOUND ")
        return

    panel([
        f"  {C.MUTED}Script     {C.RESET}  {C.WHITE}{seed_script}{C.RESET}",
        f"  {C.MUTED}Records    {C.RESET}  {C.WHITE}Creators · Attendees · Bookings · Payments · Reviews{C.RESET}",
        f"  {C.MUTED}Idempotent {C.RESET}  {C.WHITE}Skips if data already exists{C.RESET}",
    ], style="info", title=" SEED CONFIGURATION ")

    ok, _ = run_command([sys.executable, seed_script], "Seed database with mock data")

    if ok:
        panel([
            f"  {C.SUCCESS}{C.BOLD}Database seeded successfully.{C.RESET}",
            f"  {C.MUTED}Your development database is ready with realistic test data.{C.RESET}",
        ], style="success", title=" SEED COMPLETE ")


def command_test() -> None:
    section_header("Test Suite Runner", "pytest · Integration · Unit", "🧪")

    if not check_binary("pytest"):
        Log.error("pytest not found", "Install with: pip install pytest pytest-asyncio httpx")
        return

    panel([
        f"  {C.MUTED}Runner     {C.RESET}  {C.WHITE}pytest (verbose mode){C.RESET}",
        f"  {C.MUTED}Coverage   {C.RESET}  {C.WHITE}If pytest-cov installed, use:  pytest --cov=app{C.RESET}",
        f"  {C.MUTED}Scope      {C.RESET}  {C.WHITE}tests/ directory — unit + integration{C.RESET}",
    ], style="info", title=" TEST CONFIGURATION ")

    test_args = ["pytest", "-v", "--tb=short", "--no-header", "-rN"]
    if os.path.isdir("tests"):
        test_args.append("tests/")

    ok, _ = run_command(test_args, "Execute Pytest Test Suite")

    if ok:
        panel([
            f"  {C.SUCCESS}{C.BOLD}All tests passed.{C.RESET}",
        ], style="success", title=" TESTS PASSED ")
    else:
        panel([
            f"  {C.ERROR}{C.BOLD}Test failures detected.{C.RESET}",
            f"  {C.MUTED}Review the output above and fix failing tests before deploying.{C.RESET}",
        ], style="error", title=" TESTS FAILED ")


def command_setup() -> None:
    section_header("Full Environment Setup", "Migrate + Seed in one command", "⚡")

    steps = [
        "Environment diagnostics",
        "Apply database migrations",
        "Seed database with mock data",
    ]

    panel([
        f"  {C.WHITE}{C.BOLD}This command will:{C.RESET}",
        "",
        *[f"  {C.MUTED}  {i+1}.  {C.WHITE}{s}{C.RESET}" for i, s in enumerate(steps)],
        "",
        f"  {C.MUTED}Estimated time: 15–30 seconds{C.RESET}",
    ], style="brand", title=" SETUP PLAN ")
    Log.blank()

    # Step 1 — Environment check
    Log.step(1, 3, "Running environment diagnostics...")
    if not check_environment():
        panel([
            f"  {C.ERROR}{C.BOLD}Setup aborted.{C.RESET}",
            f"  {C.MUTED}Fix the environment issues listed above and retry.{C.RESET}",
        ], style="error", title=" SETUP ABORTED ")
        sys.exit(1)

    # Step 2 — Migrations
    Log.step(2, 3, "Applying database migrations...")
    ok, _ = run_command(["alembic", "upgrade", "head"], "Alembic migrations")
    if not ok:
        Log.error("Migration step failed — aborting setup")
        sys.exit(1)
    time.sleep(0.5)

    # Step 3 — Seed
    Log.step(3, 3, "Seeding database with development data...")
    command_seed()

    Log.blank()
    panel([
        f"  {C.SUCCESS}{C.BOLD}Capturo backend is fully set up and ready!{C.RESET}",
        "",
        f"  {C.MUTED}Start the server:{C.RESET}  {C.ITALIC}{C.PURPLE}python capturo.py start{C.RESET}",
        f"  {C.MUTED}Open API Docs:  {C.RESET}  {C.INFO}http://localhost:8000/docs{C.RESET}",
        f"  {C.MUTED}Health check:  {C.RESET}   {C.SUCCESS}http://localhost:8000/health{C.RESET}",
    ], style="success", title=" SETUP COMPLETE ")


def command_status(port: int, host: str) -> None:
    section_header("System Status Dashboard", "Runtime · Ports · Packages", "📊")

    # ── Server Status ──────────────────────────────────────────────────────
    server_up = is_port_open(port, "127.0.0.1")
    mysql_up  = is_port_open(3306, "127.0.0.1")

    print(f"  {C.BOLD}{C.MUTED}  SERVICE STATUS{C.RESET}")
    print(f"  {C.PURPLE}  {'─' * (TERM_WIDTH - 6)}{C.RESET}")

    def status_row(label: str, ok: bool, ok_msg: str, fail_msg: str, hint: str = "") -> None:
        dot   = f"{C.SUCCESS}●{C.RESET}" if ok else f"{C.ERROR}●{C.RESET}"
        state = f"{C.SUCCESS}{C.BOLD}{ok_msg}{C.RESET}" if ok else f"{C.ERROR}{fail_msg}{C.RESET}"
        row   = f"  {dot}  {C.WHITE}{label:<28}{C.RESET}  {state}"
        if not ok and hint:
            row += f"  {C.MUTED}{C.ITALIC}→ {hint}{C.RESET}"
        print(row)

    status_row("FastAPI Server",   server_up, f"Running on :{port}", f"Offline (port {port} not open)", "python capturo.py start")
    status_row("MySQL / XAMPP",    mysql_up,  "Running on :3306",   "Offline (port 3306 not open)",   "Start via XAMPP Control Panel")
    status_row(".env Config",      os.path.exists(".env"),      "Present", "Missing",                  "cp .env.example .env")
    status_row("alembic.ini",      os.path.exists("alembic.ini"), "Present", "Missing",               "alembic init alembic")
    status_row("uploads/ folder",  os.path.isdir("uploads"),    "Ready",   "Missing",                  "mkdir uploads")
    status_row("tests/ folder",    os.path.isdir("tests"),      "Found",   "Not created yet",          "mkdir tests")
    status_row("Virtual Env",
               hasattr(sys, 'real_prefix') or sys.base_prefix != sys.prefix,
               "Active", "Not active", "python -m venv .venv && source .venv/bin/activate")

    # ── Package Versions ───────────────────────────────────────────────────
    Log.blank()
    print(f"  {C.BOLD}{C.MUTED}  INSTALLED PACKAGES{C.RESET}")
    print(f"  {C.PURPLE}  {'─' * (TERM_WIDTH - 6)}{C.RESET}")

    packages = [
        ("fastapi",       "FastAPI"),
        ("uvicorn",       "Uvicorn"),
        ("sqlalchemy",    "SQLAlchemy"),
        ("alembic",       "Alembic"),
        ("pydantic",      "Pydantic"),
        ("pymysql",       "PyMySQL"),
        ("passlib",       "Passlib"),
        ("jose",          "python-jose"),
        ("razorpay",      "Razorpay"),
        ("PIL",           "Pillow"),
        ("firebase_admin","Firebase Admin"),
        ("structlog",     "Structlog"),
        ("pytest",        "Pytest"),
        ("httpx",         "HTTPX"),
    ]

    row_buf = []
    for import_name, display_name in packages:
        try:
            mod = __import__(import_name)
            ver = getattr(mod, "__version__", "✔")
            row_buf.append((True, display_name, ver))
        except ImportError:
            row_buf.append((False, display_name, "not installed"))

    col_w = (TERM_WIDTH - 6) // 3
    for i in range(0, len(row_buf), 3):
        row = row_buf[i:i+3]
        parts = []
        for ok, name, ver in row:
            icon  = f"{C.SUCCESS}✔{C.RESET}" if ok else f"{C.ERROR}✘{C.RESET}"
            nstr  = f"{C.WHITE}{name}{C.RESET}"
            vstr  = f"{C.MUTED}{ver}{C.RESET}" if ok else f"{C.ERROR}{ver}{C.RESET}"
            cell  = f"  {icon}  {_pad(nstr + '  ' + vstr, col_w - 4)}"
            parts.append(cell)
        print("".join(parts))

    # ── Runtime Info ───────────────────────────────────────────────────────
    Log.blank()
    print(f"  {C.BOLD}{C.MUTED}  RUNTIME INFORMATION{C.RESET}")
    print(f"  {C.PURPLE}  {'─' * (TERM_WIDTH - 6)}{C.RESET}")

    info_rows = [
        ("Python Version",   platform.python_version()),
        ("Operating System", f"{platform.system()} {platform.release()}"),
        ("Working Directory", os.getcwd()),
        ("Current Time",     datetime.now().strftime("%Y-%m-%d %H:%M:%S")),
        ("Hostname",         socket.gethostname()),
    ]
    for label, value in info_rows:
        print(f"  {C.MUTED}  {label:<22}{C.RESET}  {C.WHITE}{value}{C.RESET}")

    Log.blank()


def command_help_full() -> None:
    """Render a rich help screen."""
    section_header("Command Reference", "All available Capturo CLI commands", "📖")

    commands = [
        ("start",   "--port 8000 --host 0.0.0.0",  "Boot the FastAPI development server with live reload"),
        ("migrate", "",                              "Apply all pending Alembic database migrations"),
        ("seed",    "",                              "Populate the database with realistic mock data"),
        ("test",    "",                              "Execute the full pytest test suite"),
        ("setup",   "",                              "Full setup: run migrations + seed in sequence"),
        ("status",  "--port 8000 --host 127.0.0.1", "Display system status: server, MySQL, packages"),
        ("help",    "",                              "Show this command reference"),
    ]

    header_line = (
        f"  {C.BG_PURPLE}{C.WHITE}{C.BOLD}"
        f"  {'COMMAND':<12}  {'OPTIONS':<36}  {'DESCRIPTION':<40}  "
        f"{C.RESET}"
    )
    print(header_line)
    print(f"  {C.PURPLE}{'─' * (TERM_WIDTH - 4)}{C.RESET}")

    for i, (cmd, opts, desc) in enumerate(commands):
        bg = C.BG_DARK if i % 2 == 0 else ""
        cmd_s  = f"{C.MAGENTA}{C.BOLD}{cmd:<12}{C.RESET}"
        opts_s = f"{C.MUTED}{opts:<36}{C.RESET}"
        desc_s = f"{C.WHITE}{desc}{C.RESET}"
        print(f"  {bg}  {cmd_s}  {opts_s}  {desc_s}{C.RESET}")

    print(f"  {C.PURPLE}{'─' * (TERM_WIDTH - 4)}{C.RESET}")
    Log.blank()

    panel([
        f"  {C.BOLD}{C.WHITE}Quick Start Workflow:{C.RESET}",
        "",
        f"  {C.MUTED}1.{C.RESET}  {C.ITALIC}{C.PURPLE}python capturo.py setup{C.RESET}"
        f"          {C.MUTED}# migrate + seed in one step{C.RESET}",
        f"  {C.MUTED}2.{C.RESET}  {C.ITALIC}{C.PURPLE}python capturo.py start{C.RESET}"
        f"          {C.MUTED}# launch dev server on :8000{C.RESET}",
        f"  {C.MUTED}3.{C.RESET}  {C.ITALIC}{C.INFO}open http://localhost:8000/docs{C.RESET}"
        f"   {C.MUTED}# interactive API explorer{C.RESET}",
    ], style="brand", title=" QUICK START ")


# ─────────────────────────────────────────────────────────────────────────────
# MAIN CLI ENTRY POINT
# ─────────────────────────────────────────────────────────────────────────────

def main() -> None:
    print_banner()

    parser = argparse.ArgumentParser(
        prog="capturo",
        description="Capturo Backend Orchestrator — Professional CLI Engine",
        add_help=False,
        formatter_class=argparse.RawDescriptionHelpFormatter,
    )

    subparsers = parser.add_subparsers(dest="command")

    # start
    p_start = subparsers.add_parser("start",   add_help=False)
    p_start.add_argument("--port", type=int,  default=8000,      help="Port number")
    p_start.add_argument("--host", type=str,  default="0.0.0.0", help="Bind host")

    # migrate
    subparsers.add_parser("migrate", add_help=False)

    # seed
    subparsers.add_parser("seed", add_help=False)

    # test
    subparsers.add_parser("test", add_help=False)

    # setup
    subparsers.add_parser("setup", add_help=False)

    # status
    p_status = subparsers.add_parser("status", add_help=False)
    p_status.add_argument("--port", type=int, default=8000,      help="Port to check")
    p_status.add_argument("--host", type=str, default="127.0.0.1",help="Host to check")

    # help
    subparsers.add_parser("help", add_help=False)

    # No args → full help
    if len(sys.argv) == 1:
        command_help_full()
        sys.exit(0)

    args = parser.parse_args()

    if args.command == "start":
        if check_environment():
            command_server(args.port, args.host)
    elif args.command == "migrate":
        if check_environment():
            command_migrate()
    elif args.command == "seed":
        if check_environment():
            command_seed()
    elif args.command == "test":
        if check_environment():
            command_test()
    elif args.command == "setup":
        command_setup()
    elif args.command == "status":
        command_status(args.port, args.host)
    elif args.command == "help":
        command_help_full()
    else:
        command_help_full()
        sys.exit(1)


if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print()
        print()
        panel([
            f"  {C.WARNING}{C.BOLD}Interrupted by user.{C.RESET}",
            f"  {C.MUTED}Capturo CLI exited cleanly.{C.RESET}",
        ], style="warning", title=" INTERRUPTED ")
        sys.exit(0)