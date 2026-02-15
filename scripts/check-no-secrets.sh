#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

cd "$ROOT_DIR"

if ! git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
    echo "ERROR: Το script πρέπει να τρέχει μέσα σε git repository." >&2
    exit 1
fi

forbidden_file_regex='(^|/)(\.env(\..*)?|key\.properties|.*\.(jks|keystore|p12|pfx|pem))$'
tracked_forbidden="$(git ls-files | grep -nE "$forbidden_file_regex" || true)"

if [[ -n "$tracked_forbidden" ]]; then
    echo "ERROR: Βρέθηκαν tracked αρχεία που δεν πρέπει να υπάρχουν στο repository:" >&2
    echo "$tracked_forbidden" >&2
    echo "ERROR: Μετέφερε τα ευαίσθητα δεδομένα σε GitHub Secrets/Environment Variables." >&2
    exit 1
fi

suspicious_pattern='AKIA[0-9A-Z]{16}|ASIA[0-9A-Z]{16}|ghp_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{20,}|xox[baprs]-[A-Za-z0-9-]{20,}|-----BEGIN (RSA|EC|OPENSSH|PRIVATE KEY)-----'
suspicious_hits="$(git grep -nE "$suspicious_pattern" -- . ':!scripts/check-no-secrets.sh' || true)"

if [[ -n "$suspicious_hits" ]]; then
    echo "ERROR: Βρέθηκαν ύποπτα μυστικά/κλειδιά σε tracked περιεχόμενο:" >&2
    echo "$suspicious_hits" >&2
    echo "ERROR: Αφαίρεσέ τα από τον κώδικα και φόρτωσέ τα μέσω GitHub Secrets." >&2
    exit 1
fi

echo "[security] OK: Δεν εντοπίστηκαν tracked secrets."
