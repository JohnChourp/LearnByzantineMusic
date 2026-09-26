#!/usr/bin/env bash
# Tests για το scripts/check-release-signer.sh με ψεύτικα apksigner/keytool: κάθε κλάδος, μαζί και
# όσοι πρέπει να αποτυγχάνουν «κλειστά» (λείπει εργαλείο, αρχείο ή κωδικός → exit 2, ποτέ 0).
#
# Τρέχει με: bash scripts/tests/test_check_release_signer.sh
set -uo pipefail

HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
CHECK="$HERE/../check-release-signer.sh"
PIN="f14d6379ddc512cc1b05de57553144c9ae373ef7e09edeb8256b4191bb4537de"
OTHER="89cfbf95a5ef70b1122ccc03d7a97d8ea238819720790cc574a3d250c0b70939"
PIN_COLONS="F1:4D:63:79:DD:C5:12:CC:1B:05:DE:57:55:31:44:C9:AE:37:3E:F7:E0:9E:DE:B8:25:6B:41:91:BB:45:37:DE"

tmp="$(mktemp -d)"
trap 'rm -rf "$tmp"' EXIT
touch "$tmp/app.apk" "$tmp/release.jks" "$tmp/not-executable"

cat > "$tmp/apksigner" <<'EOF'
#!/usr/bin/env bash
printf '%b' "${FAKE_OUT:-}"
exit "${FAKE_EXIT:-0}"
EOF
# Το ψεύτικο keytool απαντά μόνο όταν ο κωδικός έρχεται από env (-storepass:env), όχι από τη γραμμή εντολών.
cat > "$tmp/keytool" <<'EOF'
#!/usr/bin/env bash
case " $* " in
    *" -storepass:env ANDROID_SIGNING_STORE_PASSWORD "*) ;;
    *) echo "fake keytool: password not passed by env" >&2; exit 3 ;;
esac
printf '%b' "${FAKE_OUT:-}"
exit "${FAKE_EXIT:-0}"
EOF
chmod +x "$tmp/apksigner" "$tmp/keytool"

failures=0
run_case() {
    local name="$1" want_exit="$2" want_text="$3"
    shift 3
    local output status
    output="$("$@" 2>&1)"
    status=$?
    if [[ "$status" != "$want_exit" ]]; then
        echo "FAIL $name: exit $status, expected $want_exit"
        printf '%s\n' "$output" | sed 's/^/    /'
        failures=$((failures + 1))
    elif [[ "$output" != *"$want_text"* ]]; then
        echo "FAIL $name: output lacks '$want_text'"
        printf '%s\n' "$output" | sed 's/^/    /'
        failures=$((failures + 1))
    else
        echo "ok   $name"
    fi
}

apk() {
    env APKSIGNER="$tmp/apksigner" FAKE_OUT="$1" FAKE_EXIT="${2:-0}" bash "$CHECK" "$tmp/app.apk"
}
keystore() {
    env KEYTOOL="$tmp/keytool" ANDROID_SIGNING_STORE_PASSWORD=pw FAKE_OUT="$1" FAKE_EXIT="${2:-0}" \
        bash "$CHECK" --keystore "$tmp/release.jks" release_alias
}

run_case "the pin in the script is the v1.16.0 certificate" 0 "" \
    grep -qx "EXPECTED_CERT_SHA256=\"$PIN\"" "$CHECK"

run_case "apk: release key passes" 0 "[signer] OK" \
    apk "Signer #1 certificate DN: CN=LearnByzantineMusic\nSigner #1 certificate SHA-256 digest: $PIN\n"
run_case "apk: another key with the same DN fails" 1 "$OTHER" \
    apk "Signer #1 certificate DN: CN=LearnByzantineMusic\nSigner #1 certificate SHA-256 digest: $OTHER\n"
run_case "apk: two different certificates fail" 1 "2 διαφορετικά" \
    apk "Signer #1 certificate SHA-256 digest: $PIN\nSigner #2 certificate SHA-256 digest: $OTHER\n"
run_case "apk: no digest line fails" 1 "Δεν βρήκα SHA-256" \
    apk "Verifies\n"

# Newer apksigner (the GitHub runner's, 2026-09-26) prints one block per scheme instead of «Signer #1».
# The first case is the runner's own output for the v1.17.1 APK, which the old parser rejected.
run_case "apk: per-scheme format (runner, v1.17.1) passes" 0 "[signer] OK" \
    apk "V2 Signer: certificate DN: CN=LearnByzantineMusic, O=LearnByzantineMusic, OU=Android, L=Athens, ST=Attica, C=GR\nV2 Signer: certificate SHA-256 digest: $PIN\nV2 Signer: certificate SHA-1 digest: 826c1a7ab4ad07d4209408a2c4c1cff761361237\nV2 Signer: certificate MD5 digest: 41dd8e437b5f70f17446bd778e08b851\n"
run_case "apk: per-scheme format with another key fails" 1 "$OTHER" \
    apk "V2 Signer: certificate SHA-256 digest: $OTHER\n"
run_case "apk: the same certificate in two schemes passes" 0 "[signer] OK" \
    apk "V2 Signer: certificate SHA-256 digest: $PIN\nV3 Signer: certificate SHA-256 digest: $PIN\n"
run_case "apk: v2 release key but v3 another key fails" 1 "2 διαφορετικά" \
    apk "V2 Signer: certificate SHA-256 digest: $PIN\nV3 Signer: certificate SHA-256 digest: $OTHER\n"
run_case "apk: a digest that is not SHA-256 fails" 1 "Δεν διάβασα" \
    apk "Signer #1 certificate SHA-256 digest: abc\n"
run_case "apk: apksigner verify failing fails" 1 "απέτυχε" \
    apk "DOES NOT VERIFY\n" 1
run_case "apk: missing file cannot pass" 2 "Δεν βρέθηκε το APK" \
    env APKSIGNER="$tmp/apksigner" bash "$CHECK" "$tmp/missing.apk"
run_case "apk: missing apksigner cannot pass" 2 "Δεν βρέθηκε apksigner" \
    env APKSIGNER="$tmp/not-executable" bash "$CHECK" "$tmp/app.apk"

run_case "keystore: release key passes (colons, upper case)" 0 "[signer] OK" \
    keystore "Alias name: release_alias\nCertificate fingerprints:\n\t SHA1: 00:11\n\t SHA256: $PIN_COLONS\n"
run_case "keystore: another key fails" 1 "$OTHER" \
    keystore "Alias name: release_alias\n\t SHA256: $OTHER\n"
run_case "keystore: keytool failing cannot pass" 2 "δεν διάβασε" \
    keystore "" 1
run_case "keystore: missing password cannot pass" 2 "ANDROID_SIGNING_STORE_PASSWORD" \
    env KEYTOOL="$tmp/keytool" ANDROID_SIGNING_STORE_PASSWORD= bash "$CHECK" --keystore "$tmp/release.jks" a
run_case "keystore: missing keystore cannot pass" 2 "Δεν βρέθηκε το keystore" \
    env KEYTOOL="$tmp/keytool" ANDROID_SIGNING_STORE_PASSWORD=pw bash "$CHECK" --keystore "$tmp/missing.jks" a

run_case "no arguments cannot pass" 2 "Usage" bash "$CHECK"
run_case "--keystore without an alias cannot pass" 2 "Usage" bash "$CHECK" --keystore "$tmp/release.jks"

if ((failures > 0)); then
    echo "$failures case(s) failed"
    exit 1
fi
echo "all cases passed"
