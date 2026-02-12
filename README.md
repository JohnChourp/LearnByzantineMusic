# LearnByzantineMusic

## Overview
Το `LearnByzantineMusic` είναι Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής, με ενότητες θεωρίας και παραδειγμάτων.  
Στην τρέχουσα έκδοση προστέθηκε και ο `Συνθέτης Μελωδίας`, ώστε ο χρήστης να γράφει στίχο και να τοποθετεί φθογγόσημα/σύμβολα σε ακριβείς θέσεις πάνω στο κείμενο.

## Business flow
- Ο χρήστης ανοίγει την εφαρμογή από την αρχική οθόνη.
- Επιλέγει θεωρητική ενότητα (ονόματα φθόγγων, χαρακτήρες ποσότητος/χρόνου/ποιότητος, μαρτυρίες κ.λπ.) ή ανοίγει τον `Συνθέτη`.
- Στον `Συνθέτη`:
- Ξεκινά με 1 γραμμή και προσθέτει επιπλέον μόνο με `+ Γραμμή`.
- Γράφει κείμενο σε γραμμές.
- Επιλέγει θέση κέρσορα ανά γραμμή.
- Εισάγει φθογγόσημα από την παλέτα συμβόλων.
- Μετακινεί κάθε σύμβολο ελεύθερα (drag) και με μικρομετατόπιση (±1dp, ±4dp).
- Χρησιμοποιεί undo/redo για αλλαγές σε στίχο και σύμβολα.
- Αποθηκεύει έργο τοπικά ως JSON ή το ξανανοίγει αργότερα.
- Εξάγει την ορατή σελίδα ως PNG.
- Ανοίγει τη σελίδα `Εικόνες Συνθέτη` για λίστα όλων των PNG exports.
- Από τη σελίδα εικόνων μπορεί να ανοίξει απευθείας τον φάκελο exports στον default file manager.
- Αν δεν υποστηρίζεται direct folder-open στη συσκευή, γίνεται fallback: αντιγραφή path στο clipboard + ενημερωτικό toast.

Κύριες αμετάβλητες αρχές:
- Το project αποθηκεύεται με schema version `1`.
- Η αποθήκευση είναι τοπική (χωρίς cloud/account).
- Δεν υπάρχει backend API ή χρέωση χρήστη.

## Inputs & outputs
### Composer project JSON (input/output)
```json
{
  "version": 1,
  "title": "Αναστάσιμον Δείγμα",
  "updatedAt": 1739388000000,
  "lines": [
    {
      "id": "line-1",
      "lyrics": "Αναστάσεως ημέρα",
      "symbols": [
        {
          "id": "sym-1",
          "symbolKey": "a1",
          "symbolText": "a",
          "symbolFontId": "mk_byzantine",
          "charIndex": 0,
          "dxDp": 0.0,
          "dyDp": -24.0,
          "scale": 1.0
        }
      ]
    }
  ]
}
```

### Σύμβολα παλέτας (asset input)
```json
{
  "key": "g1",
  "label": "G1",
  "text": "a",
  "fontId": "mk_xronos",
  "category": "time",
  "defaultDyDp": -46.0
}
```

### PNG export (output)
- Παραγόμενο αρχείο: `composition_YYYYMMDD_HHMMSS.png`
- Θέση: `externalFilesDir("exports")` (ή fallback στο `filesDir`)
- Διαχείριση από UI: σελίδα `Εικόνες Συνθέτη` με λίστα PNG και action `Άνοιγμα φακέλου εικόνων`.

## Configuration
- Android build:
- `compileSdk = 34`
- `minSdk = 24`
- `targetSdk = 34`

- Πόροι που απαιτούνται για Composer:
- `app/src/main/assets/editor_symbols_local_v1.json` (generated local mapping)
- `app/src/main/assets/local_fonts/mk_byzantine.ttf`
- `app/src/main/assets/local_fonts/mk_ison.ttf`
- `app/src/main/assets/local_fonts/mk_fthores.ttf`
- `app/src/main/assets/local_fonts/mk_loipa.ttf`
- `app/src/main/assets/local_fonts/mk_xronos.ttf`
- Τα παραπάνω παράγονται τοπικά με:
- `scripts/prepare-local-mk-assets.sh`
- Τα proprietary assets είναι local-only και δεν γίνονται commit στο git.
- Αν λείπουν local assets, ο Composer εμφανίζει οδηγία για το script και κάνει controlled fallback χωρίς crash.

- Τοπικά δεδομένα:
- JSON projects: `<app filesDir>/composer_projects/*.json`
- PNG exports: `<app external files>/exports/*.png`

- Δικαιώματα/υποδομές:
- Δεν απαιτείται backend, API key ή AWS resource.
- Δεν απαιτείται online authentication.

## Examples
### Happy path
- Ο χρήστης ανοίγει Συνθέτη (1 αρχική γραμμή), προσθέτει 2 επιπλέον γραμμές με `+ Γραμμή`, βάζει 12 σύμβολα, κάνει αποθήκευση και εξαγωγή PNG.
- Αναμενόμενο αποτέλεσμα:
- Δημιουργείται `.json` αρχείο στον φάκελο `composer_projects`.
- Δημιουργείται `.png` αρχείο στον φάκελο `exports`.

### Failure example
- Περίπτωση: αλλοιωμένο JSON αρχείο κατά τη φόρτωση.
- Αναμενόμενο αποτέλεσμα:
- Εμφάνιση μηνύματος αποτυχίας φόρτωσης.
- Η τρέχουσα σύνθεση παραμένει αμετάβλητη.

### Edge-case example
- Περίπτωση: πολύ μεγάλος στίχος και σύμβολο στο τελευταίο `charIndex`.
- Αναμενόμενο αποτέλεσμα:
- Το σύμβολο παραμένει αγκυρωμένο στο τέλος της γραμμής.
- Επιτρέπεται περαιτέρω μετατόπιση με drag/nudge.

## Συχνές ερωτήσεις (FAQ)
### Γιατί αποτυγχάνει το login/auth;
- Η εφαρμογή αυτή δεν χρησιμοποιεί login/auth ροή ή token-based πρόσβαση.
- Αν προστεθεί μελλοντικά auth layer, πρέπει να τεκμηριωθούν issuer/audience και οι απαιτούμενες claims.

### Δεν έρχονται notifications. Τι να ελέγξω;
- Η εφαρμογή δεν περιλαμβάνει push/SNS μηχανισμό στην παρούσα αρχιτεκτονική.
- Αν χρειαστεί ειδοποίηση για αποτυχίες export/save, προτείνεται τοπικό in-app notification channel.

### Το dispatch/routes δεν ενημερώνεται σωστά. Τι να ελέγξω;
- Δεν υπάρχει dispatch ή route engine. Ο Composer είναι τοπικός editor χωρίς EventBridge/SQS ροές.

### Έγινε delete/cleanup και έμειναν “ορφανά” δεδομένα. Τι κάνουμε;
- Τα δεδομένα είναι τοπικά αρχεία JSON/PNG.
- Ελέγξτε τον φάκελο `composer_projects` για παλιά `.json` και τον φάκελο `exports` για παλιά `.png`.
- Η αποκατάσταση γίνεται με επαναφόρτωση έγκυρου JSON backup.

### Το κουμπί “Άνοιγμα φακέλου εικόνων” δεν ανοίγει file manager. Τι κάνω;
- Σε κάποιους file managers/ROMs δεν υποστηρίζεται άμεσο άνοιγμα `Android/data/...` φακέλων.
- Η εφαρμογή κάνει fallback και αντιγράφει αυτόματα το πλήρες path στο clipboard.
- Επικολλήστε το path στον file manager για άμεση πλοήγηση στον σωστό φάκελο exports.

### Ο Συνθέτης κλείνει μόλις πατήσω το κουμπί. Τι να ελέγξω;
- Ελέγξτε αν υπάρχουν τα local assets:
- `app/src/main/assets/editor_symbols_local_v1.json`
- `app/src/main/assets/local_fonts/*.ttf`
- Τρέξτε `scripts/prepare-local-mk-assets.sh` και ξανακάντε install.
- Ελέγξτε `adb logcat` για πιθανά errors σε φόρτωση local fonts/symbols.
- Η τρέχουσα υλοποίηση κάνει fallback ώστε να μη γίνεται crash.

### Πώς επηρεάζονται άλλα components;
- Η νέα λειτουργία επηρεάζει τα εξής app components:
- `MainActivity` (νέο entry button για Συνθέτη)
- `ComposerActivity` + editor modules (line editing, symbol placement, undo/redo)
- `AndroidManifest` (νέα Activity δήλωση)
- Δεν επηρεάζονται backend υπηρεσίες, δικτυακά endpoints ή εξωτερικά συστήματα.

### Παραδείγματα
**Happy path**
```json
{
  "title": "Χερουβικό Δείγμα",
  "lines": 3,
  "symbolsInserted": 10,
  "saved": true,
  "pngExported": true
}
```

**Failure example**
```json
{
  "error": "project_load_failed",
  "message": "Αποτυχία φόρτωσης αρχείου."
}
```
