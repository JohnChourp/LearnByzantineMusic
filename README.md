# LearnByzantineMusic

## Overview
Το `LearnByzantineMusic` είναι Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής με ενότητες θεωρίας.
Η λειτουργία `Συνθέτης` έχει καταργηθεί και έχει αντικατασταθεί από σελίδα `8 Ήχοι` με οπτική απόδοση κλιμάκων και διαστημάτων.
Πλέον υποστηρίζεται και αυτοματοποιημένη διαδικασία release στο GitHub με tag-based publish και packaged artifacts (`APK`, `AAB`, checksums, zip).

## Business flow
- Ο χρήστης ανοίγει την αρχική οθόνη και επιλέγει θεωρητική ενότητα.
- Στο κάτω μέρος της αρχικής οθόνης εμφανίζεται footer με μορφή `poweredby JohnChourp v.<release_version>`.
- Πατώντας `8 Ήχοι`, ανοίγει η οθόνη επιλογής ήχου.
- Προεπιλεγμένος είναι ο `Α’ Ήχος`.
- Με αλλαγή ήχου από το selector, ανανεώνονται γένος, φθόγγοι ανόδου, διαστήματα (μόρια) και το διάγραμμα «σκάλα».
- Το ύψος κάθε οπτικού διαστήματος παραμένει αναλογικό στα μόρια.
- Με press-and-hold πάνω στο όνομα φθόγγου στο διάγραμμα, αναπαράγεται συνεχής τόνος στη συχνότητα του συγκεκριμένου φθόγγου για τον επιλεγμένο ήχο.
- Με απελευθέρωση (`UP`) ή έξοδο του δαχτύλου εκτός label (`EXIT`), ο τόνος σταματά άμεσα.
- Για νέα έκδοση app, ο maintainer τρέχει `scripts/release-and-tag.sh` (ή το skill wrapper), γίνεται bump έκδοσης, build release artifacts, commit + tag push.
- Με push tag `vX.Y.Z`, το GitHub Actions workflow δημοσιεύει αυτόματα GitHub Release με release packages.

Κύριες αμετάβλητες αρχές:
- Η σελίδα `8 Ήχοι` είναι εκπαιδευτική προβολή με τοπικό interaction ακρόασης φθόγγων (χωρίς αποθήκευση κατάστασης).
- Δεν υπάρχει backend API/cloud.
- Κάθε release πρέπει να έχει μοναδικό `versionCode` και semantic `versionName`.

## Inputs & outputs
### Input (UI επιλογή ήχου)
```json
{
  "selected_mode": "Α’ Ήχος"
}
```

### Output (δεδομένα προβολής)
```json
{
  "mode": "Α’ Ήχος",
  "genus": "Διατονικό",
  "ascending_phthongs": ["Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω", "Νη΄"],
  "ascending_moria": [12, 10, 8, 12, 12, 10, 8]
}
```

### Input (touch φθόγγου στο διάγραμμα)
```json
{
  "mode": "Α’ Ήχος",
  "touch_action": "DOWN",
  "phthong_label": "Πα",
  "index_top_to_bottom": 6
}
```

### Output (ήχος φθόγγου)
```json
{
  "base_frequency_hz": 220.0,
  "moria_from_low_ni": 12,
  "formula": "f = 220 * 2^(moria/72)",
  "frequency_hz": 246.94,
  "playback": "continuous_while_pressed"
}
```

### Input (release automation)
```json
{
  "command": "./scripts/release-and-tag.sh --bump patch"
}
```

### Output (release automation)
```json
{
  "version_name": "1.0.3",
  "version_code": 3,
  "tag": "v1.0.3",
  "github_release": "published",
  "assets": ["apk", "aab", "zip", "sha256"]
}
```

## Configuration
- Android build:
- `compileSdk = 34`
- `minSdk = 24`
- `targetSdk = 34`
- `Compose Compiler Extension = 1.5.14`
- `Kotlin Gradle Plugin = 1.9.24`
- `AGP = 8.5.2`

- Κύρια components:
- `MainActivity`
- `EightModesActivity`
- `layout_eight_modes.xml`
- `ScaleDiagramView`
- `PhthongTonePlayer`

- Release automation scripts:
- `scripts/bump-version.sh`
- `scripts/release-and-tag.sh`

- GitHub Actions:
- `.github/workflows/android-release.yml` (trigger σε tags `v*.*.*`)

- Προαιρετικό release signing μέσω GitHub Secrets:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

- Required GitHub permissions:
- `contents: write` για δημιουργία release και upload assets.

- Δεν απαιτούνται:
- login/auth provider
- push/SNS υποδομές
- backend resources

## Examples
### Happy path (UI)
- Ο χρήστης ανοίγει την αρχική οθόνη και βλέπει στο κάτω μέρος `poweredby JohnChourp v.1.0.3`.
- Έπειτα ανοίγει `8 Ήχοι`, επιλέγει `Πλάγιος του Β’` και βλέπει άμεσα ενημερωμένη κλίμακα/διαστήματα με σωστή οπτική αναλογία.
- Κρατά πατημένο τον φθόγγο `Νη` και ακούει συνεχή τόνο, ο οποίος σταματά μόλις αφήσει το δάχτυλο.

### Happy path (release)
```bash
./scripts/release-and-tag.sh --bump patch
```
- Αναμενόμενο: νέο commit έκδοσης, νέο tag `vX.Y.Z`, push στο GitHub, και αυτόματο Release με `APK/AAB/ZIP/SHA256`.

### Failure example
```json
{
  "error": "tag_already_exists",
  "message": "Το tag v1.0.3 υπάρχει ήδη. Δώσε νέο version bump ή explicit version."
}
```

### Edge-case example
```json
{
  "error": "missing_signing_secrets",
  "result": "release_build_unsigned",
  "action": "ορισμός ANDROID_KEYSTORE_* secrets για production publish"
}
```

## Συχνές ερωτήσεις (FAQ)
### Γιατί αποτυγχάνει το login/auth;
- Η εφαρμογή δεν χρησιμοποιεί login/auth ροή.
- Δεν απαιτούνται token/session ή identity provider.

### Δεν έρχονται notifications. Τι να ελέγξω;
- Δεν υπάρχει μηχανισμός push/SNS στο app.
- Δεν υπάρχουν backend notification routes.

### Το dispatch/routes δεν ενημερώνεται σωστά. Τι να ελέγξω;
- Δεν υπάρχει dispatch/routes ροή στην εφαρμογή.
- Η λογική είναι αποκλειστικά τοπική προβολή περιεχομένου.

### Δεν ακούγεται ο τόνος στους φθόγγους. Τι να ελέγξω;
- Επιβεβαίωσε ότι γίνεται press-and-hold πάνω στο ίδιο το label του φθόγγου (όχι στο κενό του διαγράμματος).
- Έλεγξε ένταση media του device και ότι δεν είναι σε muted/silent mode.
- Αν άλλαξες ήχο από selector την ώρα που έπαιζε τόνος, ξαναπάτησε hold σε label για νέο playback.

### Έγινε delete/cleanup και έμειναν “ορφανά” δεδομένα. Τι κάνουμε;
- Δεν υπάρχουν cloud resources για cleanup.
- Για local build artifacts, χρησιμοποίησε διαγραφή φακέλων `app/build/` και `build-artifacts/release/` αν χρειάζεται reset.

### Γιατί δεν δημοσιεύεται release όταν κάνω push;
- Το workflow ενεργοποιείται μόνο σε push tag μορφής `v*.*.*`.
- Έλεγξε ότι έγινε push και του tag (`git push origin vX.Y.Z`), όχι μόνο branch.
- Έλεγξε στο GitHub Actions αν το job `Android Tag Release` ολοκληρώθηκε επιτυχώς.

### Πώς επηρεάζονται άλλα components;
- `app/build.gradle.kts`: προστέθηκε conditional release signing από environment variables.
- `scripts/bump-version.sh`: χειρίζεται `versionName/versionCode` bump.
- `scripts/release-and-tag.sh`: χτίζει release artifacts, κάνει commit/tag/push.
- `.github/workflows/android-release.yml`: δημιουργεί GitHub Release και release packages.
- `MainActivity` και `layout_main_activity.xml`: προστέθηκε footer `poweredby JohnChourp v.<version>` με τιμή από `BuildConfig.VERSION_NAME`.
- `EightModesActivity`, `ScaleDiagramView` και `PhthongTonePlayer`: διαχειρίζονται touch labels και αναπαραγωγή συχνοτήτων με αναφορά `Νη = 220Hz`.

### Παραδείγματα
**Happy path**
```json
{
  "command": "./scripts/release-and-tag.sh --bump patch",
  "tag": "v1.0.3",
  "release_assets": ["app-release.apk", "app-release.aab", "LearnByzantineMusic-v1.0.3-packages.zip"]
}
```

**Failure example**
```json
{
  "error": "dirty_worktree",
  "message": "Υπάρχουν μη αποθηκευμένες αλλαγές. Κάνε commit/stash πρώτα."
}
```
