## Completed
- Κυκλοφόρησε Android εφαρμογή εκμάθησης Βυζαντινής Μουσικής με πλοήγηση σε θεωρητικές ενότητες.
- Καταργήθηκε πλήρως η λειτουργία `Συνθέτης` από πλοήγηση, manifest, κώδικα, layouts και assets.
- Προστέθηκε η σελίδα `8 Ήχοι` με selector, γένος, κλίμακα ανόδου και διαστήματα σε μόρια.
- Προστέθηκε αναλογική οπτική αποτύπωση διαστημάτων τύπου «σκάλα» στην ενότητα `8 Ήχοι`.
- Προστέθηκε tap-and-hold αλληλεπίδραση στους φθόγγους της σελίδας `8 Ήχοι` με αναπαραγωγή συχνοτήτων βάσει μορίων (`Νη = 220Hz`, `f = 220 * 2^(m/72)`).
- Ρυθμίστηκε αυτοματοποιημένο Android release pipeline με tag trigger (`vX.Y.Z`) σε GitHub Actions.
- Προστέθηκε direct GitHub Release publish στο `scripts/release-and-tag.sh` με upload assets και σταθερό alias `apk-release.apk` για εύκολο mobile download/install.
- Προστέθηκε signing guard σε release script και GitHub Actions ώστε να αποτυγχάνει το release όταν λείπουν keystore credentials ή όταν παράγεται μόνο unsigned APK.
- Προστέθηκε dedicated secrets guard script (`scripts/check-no-secrets.sh`) και εκτέλεση του σε local release + GitHub Actions για αποτροπή committed secret files/keys.
- Προστέθηκε ξεχωριστό workflow `Security Guard` που εκτελείται σε κάθε push/PR για συνεχή έλεγχο repository secrets σε public περιβάλλον.
- Προστέθηκε αξιόπιστη ρύθμιση CodeQL για Java/Kotlin με manual build mode, setup JDK 17 και compile μέσω `:app:compileDebugKotlin`.
- Προστέθηκε project-specific αρχείο `.codex/AGENTS.md` με υποχρεωτικούς κανόνες security-check για κάθε μελλοντική εργασία στο repository.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `commons-io` σε `2.14.0` για mitigation του `XmlStreamReader` DoS advisory.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `protobuf-java` σε `3.25.5` για mitigation DoS advisory (nested unknown-fields parsing).
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `jdom2` σε `2.0.6.1` για mitigation XXE advisory.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `netty-codec` σε `4.1.129.Final` για mitigation DoS advisory τύπου zip-bomb στους Netty decompressing decoders.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `netty-codec-http` σε `4.1.129.Final` για mitigation CRLF injection advisory στο `HttpRequestEncoder`.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `netty-codec-http2` σε `4.1.129.Final` για mitigation HTTP/2 DoS advisories.
- Προστέθηκε buildscript classpath hardening με explicit forced transitive αναβάθμιση `netty-handler` σε `4.1.129.Final` για mitigation SslHandler/SniHandler advisories.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `jose4j` σε `0.9.6` για mitigation DoS advisory σε compressed JWE content.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `commons-compress` σε `1.26.0` για mitigation OOM advisory σε unpacking Pack200 αρχείων.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `commons-lang3` σε `3.18.0` για mitigation του ClassUtils uncontrolled recursion advisory.
- Προστέθηκε buildscript classpath hardening με forced transitive αναβάθμιση `bcpkix-jdk18on`/`bcprov-jdk18on`/`bcutil-jdk18on` σε `1.79` για mitigation Excessive Allocation και Ed25519 infinite-loop advisories.
- Προστέθηκε script `scripts/setup-release-signing.sh` για ασφαλή αρχική δημιουργία keystore, local signing env setup και προαιρετική αυτόματη ενημέρωση GitHub Actions signing secrets.
- Απλοποιήθηκαν τα GitHub Release custom assets ώστε να ανεβαίνει μόνο το `apk-release.apk` (με τα source archives να παρέχονται αυτόματα από το GitHub).
- Προστέθηκε αυτόματη δημιουργία user-friendly release notes στο `scripts/release-and-tag.sh` με σύνοψη και πλήρη λίστα αλλαγών από previous tag στο νέο release.
- Προστέθηκε release packaging ανά έκδοση με signed APK alias `apk-release.apk` και generated release notes.
- Προστέθηκε local release automation με scripts για version bump, build, commit, tag και push.
- Προστέθηκε reusable Codex skill για one-command release update του app.
- Προστέθηκε footer στην αρχική σελίδα με μορφή `poweredby JohnChourp v.<version>` που τραβάει την τιμή από το τρέχον release version του app.
- Προστέθηκε νέα σελίδα `Ρυθμίσεις` με discrete slider (`20/40/60/80/100`) και άμεση αποθήκευση προτίμησης μεγέθους γραμμάτων.
- Εφαρμόστηκε global font scaling σε όλες τις activities μέσω `BaseActivity` + `AppFontScale` με μόνιμη αποθήκευση key `app_font_step`.
- Το `poweredby` στην αρχική σελίδα μεταφέρθηκε σε σταθερό footer στο κάτω μέρος του screen.
- Βελτιώθηκε το release script ώστε να κάνει ενιαίο release commit με όλες τις αλλαγές του working tree και σύντομο auto-summary στο commit message.
- Βελτιώθηκε η παραγωγή release notes ώστε να μη διπλασιάζεται ο τίτλος του GitHub Release.
- Προστέθηκε anti-duplicate fallback guard στο tag workflow ώστε να γίνεται skip publish όταν υπάρχει ήδη custom asset `apk-release.apk` στο release του ίδιου tag.

## In Progress
- [ ] No in-progress items identified in the current codebase.

## Next
- [ ] Προσθήκη αυτόματου smoke test (launch sanity) πριν το publish του release.
- [ ] Προσθήκη προστασίας ώστε το release script να μπλοκάρει όταν ο remote branch δεν είναι up-to-date.
- [ ] Προσθήκη επιλογής για prerelease tags (π.χ. `v1.1.0-rc.1`) όταν απαιτούνται δοκιμαστικές διανομές.
