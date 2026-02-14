## Completed
- Κυκλοφόρησε Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής με πλοήγηση σε θεωρητικές ενότητες.
- Καταργήθηκε πλήρως η λειτουργία `Συνθέτης` από πλοήγηση, manifest, κώδικα, layouts και assets.
- Προστέθηκε η σελίδα `8 Ήχοι` με selector, γένος, κλίμακα ανόδου και διαστήματα σε μόρια.
- Προστέθηκε αναλογική οπτική αποτύπωση διαστημάτων τύπου «σκάλα» στην ενότητα `8 Ήχοι`.
- Ρυθμίστηκε αυτοματοποιημένο Android release pipeline με tag trigger (`vX.Y.Z`) σε GitHub Actions.
- Προστέθηκε release packaging με artifacts `APK`, `AAB`, checksums και zip bundle ανά έκδοση.
- Προστέθηκε local release automation με scripts για version bump, build, commit, tag και push.
- Προστέθηκε reusable Codex skill για one-command release update του app.
- Προστέθηκε footer στην αρχική σελίδα με μορφή `poweredby JohnChourp v.<version>` που τραβάει την τιμή από το τρέχον release version του app.

## In Progress
- [ ] No in-progress items identified in the current codebase.

## Next
- [ ] Ενσωμάτωση changelog template ανά release για πιο στοχευμένες release notes.
- [ ] Προσθήκη αυτόματου smoke test (launch sanity) πριν το publish του release.
- [ ] Προσθήκη προστασίας ώστε το release script να μπλοκάρει όταν ο remote branch δεν είναι up-to-date.
- [ ] Προσθήκη επιλογής για prerelease tags (π.χ. `v1.1.0-rc.1`) όταν απαιτούνται δοκιμαστικές διανομές.
