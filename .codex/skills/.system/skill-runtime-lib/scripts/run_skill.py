#!/usr/bin/env python3
from __future__ import annotations

import os
import sys
from pathlib import Path

from runtime_support import launch_skill_root


def _usage() -> str:
    return "Usage: run-skill <skill-name> [args...]"


def _default_codex_home() -> Path:
    for candidate in Path(__file__).resolve().parents:
        if candidate.name == ".codex":
            return candidate
    return Path.cwd().resolve() / ".codex"


def _resolve_skill_root(skill_name: str) -> Path:
    codex_home = Path(os.environ.get("CODEX_HOME", str(_default_codex_home()))).expanduser()
    candidates = [
        codex_home / "skills" / skill_name,
        codex_home / "skills" / ".system" / skill_name,
    ]
    for candidate in candidates:
        if candidate.is_dir():
            return candidate
    raise SystemExit(f"Skill not found: {skill_name}")


def main(argv: list[str]) -> int:
    if len(argv) < 2 or argv[1] in {"-h", "--help"}:
        print(_usage(), file=sys.stderr)
        return 2 if len(argv) < 2 else 0

    skill_root = _resolve_skill_root(argv[1])
    return launch_skill_root(skill_root, argv[2:])


if __name__ == "__main__":
    raise SystemExit(main(sys.argv))
