#!/usr/bin/env bash
# Κάθε release υπογράφεται με ΤΟ ΙΔΙΟ πιστοποιητικό — αλλιώς καμία εγκατεστημένη εφαρμογή δεν ενημερώνεται.
#
# Το Android δέχεται ενημέρωση μόνο όταν το πιστοποιητικό υπογραφής ταιριάζει με το εγκατεστημένο.
# Αλλιώς ο χρήστης βλέπει «Η εφαρμογή δεν εγκαταστάθηκε» και πρέπει να απεγκαταστήσει πρώτα, που
# σβήνει σημειώσεις, πρόοδο και ρυθμίσεις. Ούτε το Google Auto Backup τα σώζει: ένα backup από
# εφαρμογή με άλλο πιστοποιητικό απορρίπτεται («Restore manifest signatures do not match») και το
# Android καθαρίζει τα δεδομένα της εφαρμογής (μετρημένο σε κινητό, 2026-09-26).
#
# Το v1.17.0 βγήκε υπογεγραμμένο με το τοπικό upload key ενός μηχανήματος (89cfbf95…b70939) αντί για
# το κλειδί των releases (f14d6379…4537de) που υπέγραψε το v1.16.0 και τα releases του GitHub Actions
# πριν από αυτό. Και τα δύο κλειδιά έχουν το ίδιο DN, γιατί το setup-release-signing.sh φτιάχνει νέο
# κλειδί με σταθερό DN σε κάθε μηχάνημα χωρίς keystore — άρα το «apksigner → CN=LearnByzantineMusic»
# δεν απέδειχνε τίποτα. Εδώ συγκρίνεται το SHA-256 του πιστοποιητικού, το μόνο που συγκρίνει το Android.
#
# Χρήση:
#   check-release-signer.sh <apk>
#       exit 0 μόνο αν ΚΑΘΕ πιστοποιητικό που τυπώνει το apksigner είναι το καρφωμένο (και υπάρχει ένα).
#       Το apksigner ≤ 36 τυπώνει «Signer #1 certificate …», τα νεότερα μία γραμμή ανά scheme
#       («V2 Signer: certificate …») — το v1.17.1 κόλλησε εδώ στο GitHub Actions· διαβάζονται και οι δύο.
#   check-release-signer.sh --keystore <keystore> <alias>
#       exit 0 μόνο αν το κλειδί <alias> του keystore είναι το καρφωμένο πιστοποιητικό·
#       ο κωδικός διαβάζεται από το ANDROID_SIGNING_STORE_PASSWORD. Τρέχει πριν από bump/build.
# Exit 1 σε λάθος πιστοποιητικό, 2 όταν ο έλεγχος δεν μπορεί να τρέξει — ποτέ «πέρασε» από προεπιλογή.
# Τα APKSIGNER / KEYTOOL αντικαθιστούν τα εργαλεία (για το scripts/tests/test_check_release_signer.sh).
#
# Η αλλαγή του pin ΔΕΝ διορθώνει ένα mismatch: αφήνει ορφανή κάθε εγκατεστημένη εφαρμογή. Νέο κλειδί
# σημαίνει APK Signature Scheme v3 rotation (lineage υπογεγραμμένο από το παλιό κλειδί), όχι νέο pin.
set -euo pipefail

EXPECTED_CERT_SHA256="f14d6379ddc512cc1b05de57553144c9ae373ef7e09edeb8256b4191bb4537de"

usage() {
    echo "Usage: check-release-signer.sh <apk> | --keystore <keystore> <alias>" >&2
}

normalize() {
    tr -d ':[:space:]' | tr '[:upper:]' '[:lower:]'
}

compare_to_pin() {
    local what="$1" raw="$2" actual
    actual="$(printf '%s' "$raw" | normalize)"
    if [[ ! "$actual" =~ ^[0-9a-f]{64}$ ]]; then
        echo "ERROR: Δεν διάβασα SHA-256 πιστοποιητικού για $what (πήρα: '$raw')." >&2
        return 1
    fi
    if [[ "$actual" != "$EXPECTED_CERT_SHA256" ]]; then
        echo "ERROR: Λάθος κλειδί υπογραφής για $what." >&2
        echo "ERROR:   αναμενόμενο (κλειδί των releases) = $EXPECTED_CERT_SHA256" >&2
        echo "ERROR:   βρέθηκε                            = $actual" >&2
        echo "ERROR: Με αυτό το κλειδί καμία εγκατεστημένη εφαρμογή δεν ενημερώνεται. Το release ΔΕΝ προχωράει." >&2
        echo "ERROR: Αν αυτό το μηχάνημα δεν έχει το κλειδί των releases: κάνε bump + commit + tag και push το tag —" >&2
        echo "ERROR: το workflow android-release.yml χτίζει και υπογράφει με το κλειδί των GitHub secrets." >&2
        return 1
    fi
    echo "[signer] OK: $what υπογράφεται με το κλειδί των releases ($EXPECTED_CERT_SHA256)."
}

find_apksigner() {
    if [[ -n "${APKSIGNER:-}" ]]; then
        [[ -x "$APKSIGNER" ]] && { echo "$APKSIGNER"; return 0; }
        return 1
    fi
    if command -v apksigner >/dev/null 2>&1; then
        command -v apksigner
        return 0
    fi
    local sdk candidate
    for sdk in "${ANDROID_HOME:-}" "${ANDROID_SDK_ROOT:-}" "$HOME/Library/Android/sdk" "$HOME/Android/Sdk"; do
        [[ -n "$sdk" && -d "$sdk/build-tools" ]] || continue
        candidate="$(ls -d "$sdk"/build-tools/*/apksigner "$sdk"/build-tools/*/apksigner.bat 2>/dev/null | sort -V | tail -n1 || true)"
        if [[ -n "$candidate" ]]; then
            echo "$candidate"
            return 0
        fi
    done
    return 1
}

check_apk() {
    local apk="$1" signer output digests distinct count
    if [[ ! -f "$apk" ]]; then
        echo "ERROR: Δεν βρέθηκε το APK: $apk" >&2
        return 2
    fi
    if ! signer="$(find_apksigner)"; then
        echo "ERROR: Δεν βρέθηκε apksigner (όρισε APKSIGNER ή ANDROID_HOME). Χωρίς τον έλεγχο το release δεν προχωράει." >&2
        return 2
    fi
    if ! output="$("$signer" verify --print-certs "$apk" 2>&1)"; then
        printf '%s\n' "$output" >&2
        echo "ERROR: Το apksigner verify απέτυχε για $apk." >&2
        return 1
    fi
    digests="$(printf '%s\n' "$output" | grep -E 'Signer.* certificate SHA-256 digest:' | sed -E 's/.* certificate SHA-256 digest:[[:space:]]*//' || true)"
    if [[ -z "$digests" ]]; then
        printf '%s\n' "$output" >&2
        echo "ERROR: Δεν βρήκα SHA-256 πιστοποιητικού στην έξοδο του apksigner για $apk." >&2
        return 1
    fi
    distinct="$(printf '%s\n' "$digests" | tr '[:upper:]' '[:lower:]' | tr -d ': \t\r' | sort -u)"
    count="$(printf '%s\n' "$distinct" | wc -l | tr -d ' ')"
    if [[ "$count" != "1" ]]; then
        printf '%s\n' "$output" >&2
        echo "ERROR: Το $apk έχει $count διαφορετικά πιστοποιητικά υπογραφής." >&2
        return 1
    fi
    compare_to_pin "το APK $(basename "$apk")" "$distinct"
}

check_keystore() {
    local keystore="$1" alias="$2" keytool output digest
    if [[ ! -f "$keystore" ]]; then
        echo "ERROR: Δεν βρέθηκε το keystore: $keystore" >&2
        return 2
    fi
    if [[ -z "${ANDROID_SIGNING_STORE_PASSWORD:-}" ]]; then
        echo "ERROR: Λείπει το ANDROID_SIGNING_STORE_PASSWORD για τον έλεγχο του keystore." >&2
        return 2
    fi
    keytool="${KEYTOOL:-keytool}"
    if ! command -v "$keytool" >/dev/null 2>&1; then
        echo "ERROR: Δεν βρέθηκε το keytool. Χωρίς τον έλεγχο το release δεν προχωράει." >&2
        return 2
    fi
    if ! output="$("$keytool" -list -v -keystore "$keystore" -alias "$alias" -storepass:env ANDROID_SIGNING_STORE_PASSWORD 2>&1)"; then
        echo "ERROR: Το keytool δεν διάβασε το κλειδί '$alias' από το $keystore." >&2
        return 2
    fi
    digest="$(printf '%s\n' "$output" | sed -n 's/^[[:space:]]*SHA256:[[:space:]]*//p' | head -n1)"
    compare_to_pin "το κλειδί '$alias' του $(basename "$keystore")" "$digest"
}

case "${1:-}" in
    --keystore)
        if (($# != 3)); then
            usage
            exit 2
        fi
        check_keystore "$2" "$3"
        ;;
    ""|-h|--help)
        usage
        exit 2
        ;;
    *)
        if (($# != 1)); then
            usage
            exit 2
        fi
        check_apk "$1"
        ;;
esac
