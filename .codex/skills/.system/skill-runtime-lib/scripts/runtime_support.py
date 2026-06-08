#!/usr/bin/env python3
from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
from pathlib import Path

HELP_FLAGS = {"-h", "--help", "help"}


def _current_os_key() -> str:
    if sys.platform.startswith("darwin"):
        return "macos"
    if os.name == "nt":
        return "windows"
    return "linux"


def _load_runtime_config(skill_root: Path) -> dict:
    runtime_path = skill_root / "skill.runtime.json"
    if not runtime_path.is_file():
        if (skill_root / "SKILL.md").is_file():
            return {
                "schema_version": 2,
                "skill_name": skill_root.name,
                "execution_mode": "guidance",
                "supported_os": [],
                "unsupported_behavior": "fail_fast",
                "preflight": {"checks": []},
                "tooling": {},
                "commands": [],
            }
        raise SystemExit(f"Missing skill.runtime.json in {skill_root}")
    return json.loads(runtime_path.read_text(encoding="utf-8"))


def _detect_codex_home(skill_root: Path) -> Path:
    for candidate in (skill_root, *skill_root.parents):
        if candidate.name == "skills":
            return candidate.parent
    return skill_root.parent


def _command_map(runtime: dict) -> dict[str, dict]:
    return {
        command["name"]: command
        for command in runtime.get("commands", [])
        if isinstance(command, dict) and command.get("name")
    }


def _has_help_flag(argv: list[str]) -> bool:
    return any(arg in HELP_FLAGS for arg in argv)


def _resolve_command(runtime: dict, argv: list[str]) -> tuple[dict, list[str]]:
    commands = _command_map(runtime)
    default_name = runtime.get("default_command")

    if not commands:
        raise SystemExit("No skill commands are defined in skill.runtime.json")

    if argv and argv[0] in commands:
        return commands[argv[0]], argv[1:]

    if default_name:
        command = commands.get(default_name)
        if command is None:
            raise SystemExit(f"Default command '{default_name}' is not defined")
        return command, argv

    if argv and _has_help_flag(argv):
        first_command = next(iter(commands.values()))
        return first_command, argv

    available = ", ".join(sorted(commands))
    raise SystemExit(f"Unknown skill command. Available commands: {available}")


def _guidance_only_message(skill_root: Path) -> str:
    return (
        f"Skill '{skill_root.name}' is guidance-only. "
        f"Read {skill_root / 'SKILL.md'} for the workflow."
    )


def _tool_candidates(runtime: dict, tool_name: str) -> list[str]:
    tooling = runtime.get("tooling", {})
    tool_entry = tooling.get(tool_name)
    if not isinstance(tool_entry, dict):
        return [tool_name]

    os_key = _current_os_key()
    raw_candidates = tool_entry.get(os_key) or tool_entry.get("default") or [tool_name]
    if raw_candidates and isinstance(raw_candidates[0], str):
        return [raw_candidates[0]]
    return [tool_name]


def _check_preflight(runtime: dict, argv: list[str]) -> None:
    if _has_help_flag(argv):
        return

    missing: list[str] = []
    for check in runtime.get("preflight", {}).get("checks", []):
        if check.get("type") != "tool":
            continue
        tool_name = check.get("name")
        if not tool_name:
            continue
        candidates = _tool_candidates(runtime, tool_name)
        if any(shutil.which(candidate) for candidate in candidates):
            continue
        missing.append(tool_name)

    if not missing:
        return

    details: list[str] = []
    os_key = _current_os_key()
    for check in runtime.get("preflight", {}).get("checks", []):
        tool_name = check.get("name")
        if tool_name not in missing:
            continue
        hint_map = check.get("install_hint", {})
        hint = hint_map.get(os_key) or hint_map.get("default")
        details.append(f"{tool_name}: {hint}" if hint else tool_name)
    raise SystemExit("Missing required tools:\n- " + "\n- ".join(details))


def _shell_command_prefix(target_path: Path) -> list[str]:
    suffix = target_path.suffix.lower()
    if suffix == ".sh":
        return ["bash", str(target_path)]
    if suffix == ".py":
        return [sys.executable, str(target_path)]
    return [str(target_path)]


def _build_exec_command(command: dict, skill_root: Path, passthrough_args: list[str]) -> list[str]:
    target_path = skill_root / command["path"]
    if not target_path.exists():
        raise SystemExit(f"Skill command path does not exist: {target_path}")

    base_args = list(command.get("args", []))
    kind = command.get("kind")
    if kind == "python":
        return [sys.executable, str(target_path), *base_args, *passthrough_args]
    if kind == "shell":
        return [*_shell_command_prefix(target_path), *base_args, *passthrough_args]
    raise SystemExit(f"Unsupported skill command kind: {kind}")


def launch_skill_root(skill_root: Path, argv: list[str]) -> int:
    runtime = _load_runtime_config(skill_root)
    if runtime.get("execution_mode") == "guidance" and not runtime.get("commands"):
        print(_guidance_only_message(skill_root))
        return 0
    _check_preflight(runtime, argv)
    command, passthrough_args = _resolve_command(runtime, argv)
    exec_argv = _build_exec_command(command, skill_root, passthrough_args)

    env = os.environ.copy()
    invocation_cwd = Path.cwd().resolve()
    env.setdefault("CODEX_HOME", str(_detect_codex_home(skill_root)))
    env.setdefault("CODEX_SKILL_INVOKED_CWD", str(invocation_cwd))
    env.setdefault("CODEX_SKILL_INVOKED_BASENAME", invocation_cwd.name)
    try:
        completed = subprocess.run(exec_argv, cwd=skill_root, env=env, check=False)
    except KeyboardInterrupt:
        return 130
    return completed.returncode


def launch_current_skill(script_path: Path, argv: list[str]) -> int:
    skill_root = script_path.resolve().parents[1]
    return launch_skill_root(skill_root, argv)
