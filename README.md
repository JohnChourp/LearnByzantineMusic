# LearnByzantineMusic

## Overview
Το `LearnByzantineMusic` είναι Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής με ενότητες θεωρίας.
Η λειτουργία `Συνθέτης` έχει καταργηθεί πλήρως και αντικαταστάθηκε από νέα σελίδα `8 Ήχοι`, όπου ο χρήστης βλέπει κλίμακες, φθόγγους και διαστήματα (σε μόρια) για άνοδο και κάθοδο, μαζί με οπτικά διαγράμματα τύπου «σκάλα».

## Business flow
- Ο χρήστης ανοίγει την αρχική οθόνη και επιλέγει ενότητα.
- Πατώντας `8 Ήχοι`, ανοίγει η νέα οθόνη επιλογής ήχου.
- Προεπιλεγμένος είναι ο `Α’ Ήχος`.
- Ο χρήστης αλλάζει ήχο από το selector (Spinner) και η οθόνη ανανεώνει:
- το όνομα/γένος του ήχου,
- τη σειρά φθόγγων στην άνοδο και κάθοδο,
- τα διαστήματα ανά βήμα (από φθόγγο σε φθόγγο) σε μόρια.
- το διάγραμμα ανά κατεύθυνση (άνοδος/κάθοδος) με τους φθόγγους και τα διαστήματα.

Κύριες αμετάβλητες αρχές:
- Η σελίδα `8 Ήχοι` είναι μόνο εκπαιδευτική προβολή (read-only).
- Δεν υπάρχουν αποθηκεύσεις έργων, εξαγωγές ή επεξεργασία συμβόλων.
- Δεν υπάρχει backend API ή cloud εξάρτηση.

## Inputs & outputs
### Input (επιλογή ήχου από UI)
```json
{
  "selected_mode": "Α’ Ήχος"
}
```

### Output (δεδομένα που προβάλλονται στην οθόνη)
```json
{
  "mode": "Α’ Ήχος",
  "genus": "Διατονικό",
  "ascending_phthongs": ["Νη", "Πα", "Βου", "Γα", "Δι", "Κε", "Ζω", "Νη΄"],
  "ascending_moria": [12, 10, 8, 12, 12, 10, 8],
  "descending_phthongs": ["Νη΄", "Ζω", "Κε", "Δι", "Γα", "Βου", "Πα", "Νη"],
  "descending_moria": [8, 10, 12, 12, 8, 10, 12]
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
- `MainActivity` (entry / πλοήγηση)
- `EightModesActivity` (προβολή 8 ήχων)
- `layout_eight_modes.xml` (UI selector + κλίμακες)
- `ScaleDiagramView` (custom σχεδίαση διαγραμμάτων φθόγγων/μορίων)

- Δεν απαιτούνται:
- login/auth provider
- push/SNS υποδομές
- backend resources

## Examples
### Happy path
- Ο χρήστης ανοίγει `8 Ήχοι`, αφήνει την προεπιλογή `Α’ Ήχος` και βλέπει άμεσα άνοδο/κάθοδο με φθόγγους και μόρια.
- Έπειτα επιλέγει `Πλάγιος του Β’` και η κλίμακα/διαστήματα ανανεώνονται επιτόπου.

### Failure example
```json
{
  "error": "mode_not_found",
  "message": "Μη έγκυρη επιλογή ήχου. Έγινε fallback στον Α’ Ήχο."
}
```

### Edge-case example
- Περίπτωση: στιγμιαίο `onNothingSelected` από το Spinner.
- Αναμενόμενο: η οθόνη κάνει fallback στο index `0` και αποδίδει ξανά τον `Α’ Ήχο` χωρίς crash.

## Συχνές ερωτήσεις (FAQ)
### Γιατί αποτυγχάνει το login/auth;
- Η εφαρμογή δεν χρησιμοποιεί login/auth ροή.
- Δεν απαιτούνται token, session ή εξωτερικός identity provider.

### Δεν έρχονται notifications. Τι να ελέγξω;
- Η τρέχουσα αρχιτεκτονική δεν περιλαμβάνει push notifications/SNS.
- Δεν υπάρχει μηχανισμός αποστολής ειδοποιήσεων από backend.

### Το dispatch/routes δεν ενημερώνεται σωστά. Τι να ελέγξω;
- Δεν υπάρχει dispatch/routes ροή στο app.
- Η λειτουργία είναι τοπική προβολή εκπαιδευτικού περιεχομένου.

### Έγινε delete/cleanup και έμειναν “ορφανά” δεδομένα. Τι κάνουμε;
- Μετά την κατάργηση του Συνθέτη δεν παράγονται JSON/PDF projects.
- Αν υπάρχουν παλιά αρχεία export από προηγούμενες εκδόσεις, μπορούν να διαγραφούν με ασφάλεια ως legacy δεδομένα.

### Πώς επηρεάζονται άλλα components;
- Επηρεάστηκαν τα εξής:
- `MainActivity` (αντικατάσταση κουμπιού Συνθέτη με `8 Ήχοι`),
- `AndroidManifest.xml` (αφαίρεση activities Συνθέτη, προσθήκη `EightModesActivity`),
- `app/build.gradle.kts` (αφαίρεση dependency `androidx.documentfile`),
- resources/layouts/assets του παλιού editor (κατάργηση).
- Οι υπόλοιπες θεωρητικές ενότητες παραμένουν λειτουργικά ανεξάρτητες.

### Παραδείγματα
**Happy path**
```json
{
  "selected_mode": "Πλάγιος του Δ’",
  "rendered": true,
  "ascending_diagram_segments": 7,
  "descending_diagram_segments": 7
}
```

**Failure example**
```json
{
  "error": "mode_not_found",
  "fallback": "Α’ Ήχος"
}
```
