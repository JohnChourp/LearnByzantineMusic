# LearnByzantineMusic Project Instructions

Οι παρακάτω οδηγίες ισχύουν μόνο για το repository `LearnByzantineMusic`.

## Mandatory Security Checks (always)

1. Πριν από οποιαδήποτε αλλαγή, έλεγξε για πιθανή έκθεση ευαίσθητων δεδομένων.
2. Μετά από κάθε αλλαγή που προορίζεται για commit/release, εκτέλεσε:
   - `./scripts/check-no-secrets.sh`
3. Μην τοποθετείς ποτέ secrets μέσα σε source files ή tracked configs.
4. Όλα τα ευαίσθητα δεδομένα πρέπει να έρχονται από:
   - GitHub Actions Secrets
   - Runtime environment variables
5. Για Android release signing χρησιμοποίησε αποκλειστικά:
   - `ANDROID_KEYSTORE_BASE64`
   - `ANDROID_KEYSTORE_PASSWORD`
   - `ANDROID_KEY_ALIAS`
   - `ANDROID_KEY_PASSWORD`
6. Αν ο έλεγχος secrets αποτύχει, σταμάτα το release/commit flow μέχρι να διορθωθεί.
