# LearnByzantineMusic

## Overview
Το `LearnByzantineMusic` είναι Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής με ενότητες θεωρίας.
Η λειτουργία `Συνθέτης` έχει καταργηθεί και έχει αντικατασταθεί από σελίδα `8 Ήχοι` με οπτική απόδοση κλιμάκων και διαστημάτων.
Προστέθηκε σελίδα `Ρυθμίσεις` με discrete slider για μέγεθος γραμμάτων (`20/40/60/80/100`) που εφαρμόζει global font scaling σε όλες τις οθόνες.
Πλέον υποστηρίζεται και αυτοματοποιημένη διαδικασία release στο GitHub με tag-based publish, user-friendly release notes και ένα custom release asset (`apk-release.apk`).

## Business flow
- Ο χρήστης ανοίγει την αρχική οθόνη και επιλέγει θεωρητική ενότητα.
- Στο κάτω μέρος της αρχικής οθόνης εμφανίζεται footer με μορφή `poweredby JohnChourp v.<release_version>`.
- Το footer με το `poweredby JohnChourp v.<release_version>` είναι σταθερό στο κάτω μέρος της αρχικής σελίδας (εκτός scroll περιοχής).
- Από την αρχική οθόνη ο χρήστης μπορεί να ανοίξει τη σελίδα `Ρυθμίσεις`.
- Στη σελίδα `Ρυθμίσεις` ο χρήστης αλλάζει το μέγεθος γραμμάτων με slider που κουμπώνει μόνο στις τιμές `20/40/60/80/100` (προεπιλογή `60`).
- Η αλλαγή αποθηκεύεται άμεσα σε local preferences (`app_font_step`) και εφαρμόζεται global σε όλες τις activities.
- Πατώντας `8 Ήχοι`, ανοίγει η οθόνη επιλογής ήχου.
- Προεπιλεγμένος είναι ο `Α’ Ήχος`.
- Με αλλαγή ήχου από το selector, ανανεώνονται γένος, φθόγγοι ανόδου, διαστήματα (μόρια) και το διάγραμμα «σκάλα».
- Το ύψος κάθε οπτικού διαστήματος παραμένει αναλογικό στα μόρια.
- Με press-and-hold πάνω στο όνομα φθόγγου στο διάγραμμα, αναπαράγεται συνεχής τόνος στη συχνότητα του συγκεκριμένου φθόγγου για τον επιλεγμένο ήχο.
- Με απελευθέρωση (`UP`) ή έξοδο του δαχτύλου εκτός label (`EXIT`), ο τόνος σταματά άμεσα.
- Για νέα έκδοση app, ο maintainer τρέχει `scripts/release-and-tag.sh` (ή το skill wrapper), γίνεται bump έκδοσης, build release artifacts, ενιαίο commit με όλες τις αλλαγές του working tree, και tag push.
- Το release script δημιουργεί αυτόματα συνοπτική, user-friendly περιγραφή αλλαγών από previous tag σε νέο tag (`RELEASE_NOTES.md`) με πλήρη λίστα commits, χωρίς να επαναλαμβάνει τον τίτλο του release.
- Το release script δημοσιεύει άμεσα GitHub Release με μόνο custom asset το `apk-release.apk` (για εύκολο mobile install download) και χρησιμοποιεί τα generated notes ως release description.
- Τα `Source code (zip)` και `Source code (tar.gz)` εμφανίζονται αυτόματα από το GitHub σε κάθε tag release.
- Το release script και το GitHub Action δημοσιεύουν μόνο signed APK· αν λείπουν signing credentials, το release μπλοκάρεται πριν το upload.
- Πριν από release γίνεται αυτόματος έλεγχος ότι δεν υπάρχουν committed secrets/keystore αρχεία στο repository.
- Σε κάθε push/pull request τρέχει αυτόματα ο έλεγχος `Security Guard` για ανίχνευση committed secrets.
- Με push tag `vX.Y.Z`, το GitHub Actions workflow παραμένει ως επιπλέον fallback για release packaging.

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

### Input (ρύθμιση μεγέθους γραμμάτων)
```json
{
  "settings_page": true,
  "font_size_step": 80
}
```

### Output (global font scale)
```json
{
  "stored_key": "app_font_step",
  "stored_value": 80,
  "font_scale": 1.1,
  "scope": "all_activities"
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
  "release_notes": "build-artifacts/release/v1.0.3/RELEASE_NOTES.md",
  "assets": ["apk-release.apk", "source-code-zip", "source-code-tar-gz"]
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
- `SettingsActivity`
- `EightModesActivity`
- `BaseActivity`
- `AppFontScale`
- `layout_eight_modes.xml`
- `layout_settings.xml`
- `ScaleDiagramView`
- `PhthongTonePlayer`

- Ρυθμίσεις εφαρμογής:
- SharedPreferences file: `learn_byzantine_music_settings`
- Key: `app_font_step`
- Allowed values: `20 | 40 | 60 | 80 | 100`
- Default value: `60`

- Release automation scripts:
- `scripts/bump-version.sh`
- `scripts/release-and-tag.sh`
- `scripts/check-no-secrets.sh`
- `scripts/setup-release-signing.sh`
- `.codex/AGENTS.md` (project-specific Codex safety instructions)

- GitHub Actions:
- `.github/workflows/android-release.yml` (trigger σε tags `v*.*.*`)
- `.github/workflows/security-guard.yml` (trigger σε κάθε push/pull request)

- Υποχρεωτικό release signing για installable GitHub APK:
- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`
- Για local release script απαιτούνται επίσης env vars:
- `ANDROID_SIGNING_STORE_FILE`
- `ANDROID_SIGNING_STORE_PASSWORD`
- `ANDROID_SIGNING_KEY_ALIAS`
- `ANDROID_SIGNING_KEY_PASSWORD`

- Required GitHub permissions:
- `contents: write` για δημιουργία release και upload assets.

- Κανόνας ασφάλειας:
- Δεν γίνεται commit σε `.env*`, `*.jks`, `*.keystore`, `*.p12`, `*.pfx`, `*.pem`, `key.properties`.
- Ευαίσθητες τιμές περνάνε μόνο από GitHub Secrets και runtime env vars.

- Δεν απαιτούνται:
- login/auth provider
- push/SNS υποδομές
- backend resources

## Examples
### Happy path (UI)
- Ο χρήστης ανοίγει την αρχική οθόνη και βλέπει στο κάτω μέρος `poweredby JohnChourp v.1.0.3`.
- Πατά `Ρυθμίσεις`, μετακινεί το slider στο `80` και η εφαρμογή εμφανίζει άμεσα μεγαλύτερα γράμματα.
- Έπειτα ανοίγει `8 Ήχοι`, επιλέγει `Πλάγιος του Β’` και βλέπει άμεσα ενημερωμένη κλίμακα/διαστήματα με σωστή οπτική αναλογία.
- Κρατά πατημένο τον φθόγγο `Νη` και ακούει συνεχή τόνο, ο οποίος σταματά μόλις αφήσει το δάχτυλο.

### Happy path (release)
```bash
./scripts/setup-release-signing.sh --set-github-secrets
source "$HOME/.android/learnbyzantine/release-signing.env"
./scripts/release-and-tag.sh --bump patch
```
- Αναμενόμενο: νέο commit έκδοσης, νέο tag `vX.Y.Z`, push στο GitHub, direct publish GitHub Release και upload μόνο `apk-release.apk`.
- Το commit του release δημιουργείται αυτόματα ως ένα ενιαίο commit που περιλαμβάνει όλες τις διαθέσιμες αλλαγές (tracked/untracked, εκτός ignored) με σύντομο summary στο commit message.
- Το GitHub Release description περιλαμβάνει συνοπτική εικόνα (commits/files/contributors), περιοχές που επηρεάστηκαν και πλήρη λίστα αλλαγών από το προηγούμενο release, χωρίς διπλό τίτλο.

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
  "result": "release_aborted_before_publish",
  "action": "ορισμός ANDROID_KEYSTORE_* secrets στο GitHub και ANDROID_SIGNING_* vars στο local release environment"
}
```

### Failure example (secrets guard)
```json
{
  "error": "tracked_secret_detected",
  "message": "Βρέθηκε committed secret/keystore αρχείο. Μεταφορά σε GitHub Secrets και αφαίρεση από git history/index."
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

### Δεν αλλάζει το μέγεθος γραμμάτων. Τι να ελέγξω;
- Άνοιξε `Ρυθμίσεις` και επιβεβαίωσε ότι η τιμή άλλαξε σε ένα από τα επιτρεπτά βήματα (`20/40/60/80/100`).
- Έλεγξε ότι δεν δοκιμάζεις την ίδια τιμή με πριν (αν μείνει ίδια, δεν αλλάζει scale).
- Αν υπάρχει παλαιά τιμή εκτός επιτρεπτών βημάτων, το app την κανονικοποιεί αυτόματα στο κοντινότερο επιτρεπτό βήμα.

### Έγινε delete/cleanup και έμειναν “ορφανά” δεδομένα. Τι κάνουμε;
- Δεν υπάρχουν cloud resources για cleanup.
- Για local build artifacts, χρησιμοποίησε διαγραφή φακέλων `app/build/` και `build-artifacts/release/` αν χρειάζεται reset.

### Γιατί δεν δημοσιεύεται release όταν κάνω push;
- Το script κάνει direct publish με `gh`; έλεγξε πρώτα ότι υπάρχει ενεργό `gh auth login`.
- Αν χρησιμοποιείς `--skip-gh-release`, τότε η δημοσίευση εξαρτάται από το workflow tag trigger `v*.*.*`.
- Έλεγξε ότι έγινε push και του tag (`git push origin vX.Y.Z`), όχι μόνο branch.

### Γιατί εμφανίζει "App not installed as package appears to be invalid";
- Συνήθως το APK είναι unsigned (ή αλλιώς αλλοιωμένο κατά το download).
- Από εδώ και πέρα το pipeline αποτυγχάνει όταν λείπουν signing secrets και δεν ανεβάζει unsigned `apk-release.apk`.
- Έλεγξε στο GitHub Release ότι κατεβάζεις το artifact `apk-release.apk` του τελευταίου επιτυχημένου release.

### Το `SHA256SUMS.txt` έχει secrets και πρέπει να αφαιρείται;
- Όχι, ένα αρχείο SHA256 checksums περιέχει μόνο hashes αρχείων και όχι κωδικούς/keys.
- Παρ' όλα αυτά, το release policy του project πλέον ανεβάζει μόνο `apk-release.apk` ως custom asset για απλούστερη δημόσια διανομή.

### Πώς κρύβω ευαίσθητα στοιχεία ώστε να μη φαίνονται στον κώδικα;
- Βάλε τα σε `Settings -> Secrets and variables -> Actions` στο GitHub repository.
- Για signing χρησιμοποίησε τα `ANDROID_KEYSTORE_BASE64`, `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- Για αυτοματοποιημένη αρχική ρύθμιση keystore+secrets χρησιμοποίησε `./scripts/setup-release-signing.sh --set-github-secrets`.
- Το `scripts/check-no-secrets.sh` και το CI μπλοκάρουν release όταν εντοπίσουν committed secret files/keys.

### Πού βρίσκω τα signing keys και μπορώ να τα βάλω στο build.gradle.kts;
- Τα signing keys δεν υπάρχουν έτοιμα στο project, δημιουργούνται μία φορά με `keytool`.
- Δεν τα βάζουμε ποτέ στο `app/build.gradle.kts` επειδή το repository είναι public.
- Το project διαβάζει signing δεδομένα μόνο από environment variables και GitHub Secrets.

### Υπάρχει μόνιμος έλεγχος ασφάλειας για αυτό το public repository;
- Ναι, σε κάθε push/PR εκτελείται το workflow `Security Guard`.
- Επιπλέον, πριν από κάθε release εκτελείται ξανά ο ίδιος έλεγχος και αν αποτύχει, το release μπλοκάρεται.
- Για μέγιστη προστασία ενεργοποίησε στο GitHub branch protection με required status check το `Security Guard / repository-secrets-guard`.

### Πώς επηρεάζονται άλλα components;
- `app/build.gradle.kts`: προστέθηκε conditional release signing από environment variables.
- `scripts/bump-version.sh`: χειρίζεται `versionName/versionCode` bump.
- `scripts/release-and-tag.sh`: χτίζει release artifacts, απαιτεί υποχρεωτικά signing env vars, μπλοκάρει unsigned APK outputs, κάνει commit/tag/push, κάνει stage+commit όλες τις αλλαγές του working tree σε ένα release commit με σύντομο summary, παράγει user-friendly `RELEASE_NOTES.md` (previous tag → νέο tag) χωρίς διπλό τίτλο, και δημιουργεί/ενημερώνει direct GitHub Release μόνο με custom asset `apk-release.apk`.
- `scripts/check-no-secrets.sh`: αποτρέπει commit/release όταν υπάρχουν tracked μυστικά ή υπογεγραμμένα κλειδιά μέσα στο repository.
- `scripts/setup-release-signing.sh`: δημιουργεί release keystore εκτός repository, γράφει local env file signing και ενημερώνει προαιρετικά αυτόματα τα GitHub Actions secrets.
- `.github/workflows/security-guard.yml`: τρέχει secrets guard σε κάθε push/PR.
- `.github/workflows/android-release.yml`: τρέχει secrets guard, απαιτεί υποχρεωτικά signing secrets, μπλοκάρει unsigned APK outputs και δημοσιεύει μόνο το `apk-release.apk` ως custom asset.
- `MainActivity` και `layout_main_activity.xml`: προστέθηκε footer `poweredby JohnChourp v.<version>` με τιμή από `BuildConfig.VERSION_NAME`.
- `SettingsActivity`, `layout_settings.xml`, `AppFontScale` και `BaseActivity`: διαχειρίζονται την αποθήκευση/εφαρμογή global font scaling για όλη την εφαρμογή.
- `EightModesActivity`, `ScaleDiagramView` και `PhthongTonePlayer`: διαχειρίζονται touch labels και αναπαραγωγή συχνοτήτων με αναφορά `Νη = 220Hz`.

### Παραδείγματα
**Happy path**
```json
{
  "command": "./scripts/release-and-tag.sh --bump patch",
  "tag": "v1.0.3",
  "release_assets": ["apk-release.apk", "Source code (zip)", "Source code (tar.gz)"]
}
```

**Failure example**
```json
{
  "error": "gh_auth_missing",
  "message": "Δεν υπάρχει ενεργό gh auth session. Τρέξε gh auth login ή χρησιμοποίησε --skip-gh-release."
}
```
