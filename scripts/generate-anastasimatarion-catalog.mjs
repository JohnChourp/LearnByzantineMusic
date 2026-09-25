#!/usr/bin/env node
/**
 * Builds app/src/main/assets/anastasimatarion_v1.json — the hymn catalog behind the
 * «Αναστασιματάριο» page — from the Greek liturgical texts of the Octoechos Sunday services.
 *
 * The source is NOT named here and is NOT written into the asset. This repository is public, and
 * the liturgical texts follow the same confidential policy as the εορτολόγιο dataset (ClickUp
 * `869dbkkwf`, decided 2026-09-22). Pass the base URL at generation time:
 *
 *   ANASTASIMATARION_SOURCE_URL=<base-url> node scripts/generate-anastasimatarion-catalog.mjs
 *
 * The value is supplied by the operator at generation time and is deliberately stored in NEITHER
 * repository — not here, and not in the brain. Without it the script refuses to run rather than
 * silently emitting an empty catalog. The catalog is regenerated rarely; carrying the source in a
 * file would defeat the point of removing it from the asset.
 *
 * Only the INCIPIT (opening words) of each hymn is stored, never the full text: the page lists
 * the hymns so recordings can be kept per hymn; the chanter sings from the book.
 *
 * Per mode (Tone1Sun … Tone8Sun) the catalog keeps what the Anastasimatarion sets to music:
 *   Great Vespers — Κύριε ἐκέκραξα (Psalm 140; not part of the Octoechos text, added as a fixed
 *                   first item), Στιχηρά Αναστάσιμα, Ανατολικά, Δογματικό, Απόστιχα with this
 *                   mode's Θεοτοκίο, Απολυτίκιο with its Θεοτοκίο.
 *   Orthros       — Καθίσματα (with their Θεοτοκία), Υπακοή, Αναβαθμοί (one entry per
 *                   antiphon), Προκείμενο, the Ειρμοί of the Resurrection canon, Κοντάκιο,
 *                   Αίνοι (Αναστάσιμα and Ανατολικά).
 *   Liturgy       — Μακαρισμοί (one entry).
 * Skipped on purpose: Small Vespers, the Midnight Office, repeated Apolytikia, the Theotokia of
 * the other seven modes that every page lists, the troparia of the canons, the Οίκος.
 *
 * ## Hymn codes are permanent, and locked (ClickUp `869f5x2a9`)
 *
 * Each hymn has a two-digit code, unique in its mode. On the user's device the code is a
 * PERMANENT key in two places:
 *   - the hymn's recordings folder "<code> <incipit>" (HymnFolders), found again by its
 *     "<code> " prefix;
 *   - the hymn's analysis settings, stored under "hymn:<mode>:<code>" (AnalysisSettingsStore).
 * A code that moves re-attaches the user's recordings and settings to a DIFFERENT hymn, silently.
 * The codes used to be a display-order counter, so one hymn added early in a mode would have
 * moved every code after it.
 *
 * So codes come from the committed lock scripts/anastasimatarion-codes.lock.json (not shipped in
 * the APK). It maps each hymn's identity — service, group, incipit and occurrence (the n-th hymn
 * sharing all three; always 1 today, because selectHymns drops a repeated incipit inside a group)
 * — to its code. Βαρύς 02 and 42 share the incipit «Δεῦτε ἀγαλλιασώμεθα τῷ Κυρίῳ»; their groups
 * tell them apart. On every run:
 *   - a hymn in the lock keeps its code, wherever it is now displayed;
 *   - a new hymn gets the next free code (highest code ever given + 1, retired codes included):
 *     at the END, however early it is displayed;
 *   - a locked hymn the source no longer yields STOPS the run. Resolve it in the lock, under the
 *     mode, and run again:
 *       "rename": [{"code": "05", "service": "…", "group": "…", "incipit": "<the corrected text>"}]
 *           the same code now names that hymn: an incipit fix, or a deliberate reassignment;
 *       "retire": [{"code": "05"}]
 *           the hymn left the catalog; its code moves to "retired" and is never given again.
 *     The run applies them, records them under "renamed" / "retired", and removes the
 *     instructions. Any other change to an incipit — even one accent — is refused the same way;
 *   - nothing is written unless every mode succeeds.
 * AnastasimatarionCodeLockTest (JVM, run by CI) fails whenever the shipped asset and the lock
 * disagree. CI runs no Node, so that test — not this script — is the gate.
 * scripts/tests/test_anastasimatarion_codes.mjs tests the locking rules (node --test).
 *
 * The lock was created once from the asset as it shipped, so no code moved when it was introduced:
 *   node scripts/generate-anastasimatarion-catalog.mjs --init-lock-from-asset
 * That command refuses to overwrite an existing lock: the lock is the record of every code ever
 * given, and an asset cannot tell which codes were retired.
 *
 * Usage (Node 18+, no dependencies):
 *   node scripts/generate-anastasimatarion-catalog.mjs [--cache-dir DIR] [--out FILE] [--lock FILE]
 * --cache-dir reads/writes ToneNSun.html there instead of downloading every time.
 */
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

const SOURCE_URL = process.env.ANASTASIMATARION_SOURCE_URL ?? '';
const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');

// The app's eight modes (music/Mode.kt), in the same order: each key is Mode.key and the N of
// ToneNSun.html is Mode.number. AnastasimatarionGeneratorModesTest checks this list on every build.
const MODES = [
  { key: 'first', page: 'Tone1Sun.html', label: 'Α' },
  { key: 'second', page: 'Tone2Sun.html', label: 'Β' },
  { key: 'third', page: 'Tone3Sun.html', label: 'Γ' },
  { key: 'fourth', page: 'Tone4Sun.html', label: 'Δ' },
  { key: 'plagal_first', page: 'Tone5Sun.html', label: 'ΠΛ Α' },
  { key: 'plagal_second', page: 'Tone6Sun.html', label: 'ΠΛ Β' },
  { key: 'varys', page: 'Tone7Sun.html', label: 'ΒΑΡΥΣ' },
  { key: 'plagal_fourth', page: 'Tone8Sun.html', label: 'ΠΛ Δ' },
];

// Display order of the groups inside each service.
const SERVICES = [
  { key: 'vespers', groups: ['kekragarion', 'stichera_anastasima', 'anatolika', 'dogmatikon', 'aposticha', 'apolytikion'] },
  { key: 'orthros', groups: ['kathismata', 'ypakoe', 'anavathmoi', 'prokeimenon', 'heirmoi', 'kontakion', 'ainoi_anastasima', 'ainoi_anatolika'] },
  { key: 'liturgy', groups: ['makarismoi'] },
];

const KEKRAGARION_INCIPIT = 'Κύριε ἐκέκραξα πρὸς σέ, εἰσάκουσόν μου';

function parseArgs(argv) {
  const args = {
    cacheDir: null,
    out: path.join(ROOT, 'app/src/main/assets/anastasimatarion_v1.json'),
    lock: path.join(ROOT, 'scripts/anastasimatarion-codes.lock.json'),
    initLock: false,
  };
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === '--cache-dir') args.cacheDir = argv[++i];
    else if (argv[i] === '--out') args.out = argv[++i];
    else if (argv[i] === '--lock') args.lock = argv[++i];
    else if (argv[i] === '--init-lock-from-asset') args.initLock = true;
    else throw new Error(`Unknown argument: ${argv[i]}`);
  }
  return args;
}

/** Uppercase, accent-free form used only for matching headings. */
export function norm(text) {
  return text.normalize('NFD').replace(/\p{M}/gu, '').toUpperCase()
    .replace(/[΄'’`´ʹ.,·;:\-–—]/g, ' ').replace(/\s+/g, ' ').trim();
}

/**
 * Page HTML → [{ red, text }] lines plus { sep: true } paragraph ends. Red marks the rubrics
 * (headings, mode markers, notes); `<br/>` breaks a line inside a hymn, `</p>` ends a hymn.
 */
export function toLines(html) {
  const text = html
    .replace(/<script[\s\S]*?<\/script>/gi, ' ')
    .replace(/<a [^>]*>[\s\S]*?<\/a>/gi, ' ')
    .replace(/<font color="?#ff0000"?>/gi, '⟦R⟧')
    .replace(/<br\s*\/?>/gi, '\n')
    .replace(/<\/p>/gi, '\n⟦P⟧\n')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&');
  return text.split(/\r?\n/)
    .map((raw) => (raw.trim() === '⟦P⟧'
      ? { sep: true, red: false, text: '' }
      : { red: raw.includes('⟦R⟧'), text: raw.replace(/⟦[RP]⟧/g, '').replace(/\s+/g, ' ').trim() }))
    .filter((line) => line.sep || line.text.length > 0);
}

// Rubrics that are section headings (as opposed to the name of a model melody).
const HEADING_WORDS = /^(ΣΤΙΧΗΡΑ|ΚΑΙ ΣΤΙΧΗΡΑ|ΑΠΟΣΤΙΧΑ|ΘΕΟΤΟΚΙΟΝ|ΚΑΝΩΝ|ΩΔΗ|ΑΠΟΛΥΤΙΚΙΟΝ|ΚΑΘΙΣΜΑ|(Η )?ΥΠΑΚΟΗ|(ΟΙ )?ΑΝΑΒΑΘΜΟΙ|ΑΝΤΙΦΩΝΟΝ|ΠΡΟΚΕΙΜΕΝΟΝ|ΚΟΝΤΑΚΙΟΝ|(Ο )?ΟΙΚΟΣ|ΑΙΝΟΙ|ΕΤΕΡΑ|ΔΟΓΜΑΤΙΚΟΝ|ΕΙΣ ΤΟΝ ΣΤΙΧΟΝ|ΕΞΑΠΟΣΤΕΙΛΑΡΙΟΝ|ΤΡΙΑΔΙΚΟΝ|ΕΥΛΟΓΗΤΑΡΙΑ|(ΟΙ )?ΜΑΚΑΡΙΣΜΟΙ)/;

const SERVICE_MARKERS = [
  [/^ΕΝ ΤΩ ΜΙΚΡΩ ΕΣΠΕΡΙΝΩ/, 'small_vespers'],
  [/^ΕΝ ΤΩ ΜΕΓΑΛΩ ΕΣΠΕΡΙΝΩ/, 'vespers'],
  [/^ΕΝ ΤΩ ΜΕΣΟΝΥΚΤΙΚΩ/, 'midnight'],
  [/^ΕΝ ΤΩ ΟΡΘΡΩ/, 'orthros'],
  [/^ΘΕΙΑ ΛΕΙΤΟΥΡΓΙΑ/, 'liturgy'],
];

/**
 * Lines → hymn blocks { service, heading, notes[], text[] }. A red «Ἦχος …» starts a hymn,
 * red lines right after it are notes (model melody, «Ὁ Εἱρμὸς»), any other red line is a
 * heading. «Δόξα…» / «Καὶ νῦν…» split a block into separate hymns; verses (Στίχ.) are dropped.
 */
export function toBlocks(lines) {
  const blocks = [];
  let service = null;
  let heading = null;
  let current = null;
  const flush = () => {
    if (current && current.text.length) blocks.push(current);
    current = null;
  };
  const start = (notes = []) => { current = { service, heading, notes, afterEchos: false, text: [] }; };
  let lastWasHeading = false;
  let paragraphEnded = false;
  for (const line of lines) {
    if (line.sep) { paragraphEnded = true; continue; }
    const n = norm(line.text);
    if (paragraphEnded && !line.red && current && current.text.length) {
      // A new paragraph starts a new hymn under the same rubrics — unless it opens in lowercase,
      // which is the tail of the previous hymn («… δόξα τῇ συγκαταβάσει σου»).
      const first = line.text.charAt(0);
      if (first === first.toUpperCase()) {
        const { afterEchos } = current;
        flush();
        start();
        current.afterEchos = afterEchos;
        current.continued = true;
      }
    }
    paragraphEnded = false;
    if (/^ΟΙ ΜΑΚΑΡΙΣΜΟΙ/.test(n)) {
      // The Beatitudes open the Liturgy even where the page has no «ΘΕΙΑ ΛΕΙΤΟΥΡΓΙΑ» rubric.
      flush(); service = 'liturgy'; heading = null; lastWasHeading = false;
      const rest = line.text.split(' ').slice(2).join(' ');
      if (rest) { start(); current.text.push(rest); }
      continue;
    }
    const marker = SERVICE_MARKERS.find(([re]) => re.test(n));
    if (marker) { flush(); service = marker[1]; heading = null; lastWasHeading = false; continue; }
    if (/^ΣΤΙΧ /.test(n)) { flush(); continue; }                     // psalm verse, red or black
    if (/^ΨΑΛΜΟΣ /.test(n) && line.red) { flush(); continue; }       // psalm title inside the Praises
    // Cues are the word followed by punctuation («Δόξα…», «Καὶ νῦν. Σταυροθεοτοκίον»); a refrain
    // inside a hymn («δόξα τῇ ἀναστάσει σου·») continues with words.
    const cue = /^(δοξα|και νυν)\s*[.…]/i.test(line.text.normalize('NFD').replace(/\p{M}/gu, '')) && n.length < 40;
    if (cue && /^ΔΟΞΑ( |$)/.test(n)) { flush(); start(['doxa']); continue; }
    if (cue && /^ΚΑΙ ΝΥΝ( |$)/.test(n)) { flush(); start(['kai_nyn']); continue; }
    if (line.red) {
      if (current && current.text.length === 0 && current.continued) current = null; // a rubric, not a hymn, follows
      const pending = current && current.text.length === 0;
      if (/^ΗΧΟΣ /.test(n)) {
        if (!pending) { flush(); start(); }
        current.afterEchos = true;
        lastWasHeading = false;
        continue;
      }
      if (pending && current.afterEchos) { current.notes.push(line.text); continue; } // model melody, «Ὁ Εἱρμὸς»
      if (lastWasHeading && !pending && !HEADING_WORDS.test(n)) {
        // A second rubric right under a heading («Κοντάκιον» → «Ἐπεφάνης σήμερον») names the model melody.
        start([line.text]);
        current.afterEchos = true;
        lastWasHeading = false;
        continue;
      }
      heading = line.text;
      lastWasHeading = true;
      if (pending) { current.heading = heading; continue; }         // heading right after «Δόξα…/Καὶ νῦν…»
      flush();
      continue;
    }
    lastWasHeading = false;
    if (!current) start();
    current.text.push(line.text);
  }
  flush();
  return blocks;
}

/** Opening words of a hymn: at least three words, cut at the first clause break, ≤ 60 chars. */
export function incipitOf(textLines) {
  const joined = textLines.join(' ')
    .normalize('NFC')
    .replace(/µ/g, 'μ')                 // some pages type the micro sign for μ
    .replace(/^[-–]\s*/, '').replace(/[«»"“”]/g, '').replace(/\s+/g, ' ').trim();
  const words = joined.split(' ');
  let out = '';
  for (const word of words) {
    const next = out ? `${out} ${word}` : word;
    if (next.length > 60 && out) break;
    out = next;
    const count = out.split(' ').length;
    if (count >= 3 && /[,··;;.!]$/u.test(word)) break;
  }
  return out.replace(/[\s,··;;.!:]+$/u, '');
}

function isThisModeTheotokion(headingNorm, modeLabel) {
  const m = headingNorm.match(/^ΘΕΟΤΟΚΙΟΝ ΤΟΥ Η?ΗΧΟΥ (.+)$/);
  return m ? norm(m[1]) === modeLabel : null;
}

const SINGLE = new Set(['dogmatikon', 'ypakoe', 'prokeimenon', 'kontakion', 'makarismoi']);
const ODES = ['α΄', 'γ΄', 'δ΄', 'ε΄', 'ς΄', 'ζ΄', 'η΄', 'θ΄'];
// norm() of an ode number («ᾨδὴ ς'» → «ΩΔΗ Σ») → its display label.
const ODE_LABELS = { Α: 'α΄', Β: 'β΄', Γ: 'γ΄', Δ: 'δ΄', Ε: 'ε΄', Σ: 'ς΄', ΣΤ: 'ς΄', Ζ: 'ζ΄', Η: 'η΄', Θ: 'θ΄' };
const LONG_LINE = 90;

/**
 * The hymns inside one block. A block opened by a red «Ἦχος» is one hymn (poetic line breaks).
 * Pages that give the mode in the heading instead write one hymn per paragraph: there every long
 * line is its own hymn and a run of short lines is one hymn.
 */
export function hymnsOf(block) {
  if (block.afterEchos) return [block.text];
  // Paragraph pages: every line is a long paragraph, one hymn each. Anything else is one hymn.
  if (block.text.length > 1 && block.text.every((line) => line.length >= LONG_LINE)) return block.text.map((line) => [line]);
  return [block.text];
}

/** Hymn blocks of one page → { groupKey: [ { incipit, note? } ] } following the rules above. */
export function selectHymns(blocks, mode) {
  const groups = Object.fromEntries(SERVICES.flatMap((s) => s.groups).map((g) => [g, []]));
  groups.kekragarion.push({ incipit: KEKRAGARION_INCIPIT, note: 'Ψαλμός 140' });
  const seen = new Set();
  const addText = (group, rawText, extra = {}) => {
    if (SINGLE.has(group) && groups[group].length) return;
    // A short first line ending in a full stop names the model melody («Τὸν τάφον σου Σωτήρ.»).
    const text = rawText.length > 2 && rawText[0].length < 30 && /\.$/.test(rawText[0]) ? rawText.slice(1) : rawText;
    const incipit = incipitOf(text);
    const key = `${group}|${norm(incipit)}`;
    if (!incipit || seen.has(key)) return;
    seen.add(key);
    groups[group].push({ incipit, ...extra });
  };
  const add = (group, block, extra = {}) => {
    for (const text of hymnsOf(block)) addText(group, text, extra);
  };
  const isCue = (block) => block.notes.includes('doxa') || block.notes.includes('kai_nyn');
  let vespersPhase = 'start';
  let apostichaTheotokionTaken = false;
  let apolytikionTheotokionTaken = false;
  let canon = null;
  let anavathmoiHeading = null;
  let orthrosSection = null;
  let needAntiphon = false;
  const liturgyLines = [];

  for (const block of blocks) {
    const h = norm(block.heading || '');
    if (block.service === 'vespers') {
      if (/^ΣΤΙΧΗΡΑ ΑΝΑΣΤΑΣΙΜΑ/.test(h)) { vespersPhase = 'stichera'; add('stichera_anastasima', block); continue; }
      if (/^ΕΤΕΡΑ ΣΤΙΧΗΡΑ ΑΝΑΤΟΛΙΚΑ/.test(h)) { vespersPhase = 'anatolika'; add('anatolika', block); continue; }
      if (/^(ΘΕΟΤΟΚΙΟΝ|ΔΟΓΜΑΤΙΚΟΝ)/.test(h) && vespersPhase === 'anatolika') { vespersPhase = 'dogmatikon'; add('dogmatikon', block); continue; }
      if (/^ΑΠΟΣΤΙΧΑ/.test(h)) { vespersPhase = 'aposticha'; add('aposticha', block); continue; }
      if (/^ΑΠΟΛΥΤΙΚΙΟΝ/.test(h)) {
        vespersPhase = 'apolytikion';
        if (!groups.apolytikion.length) add('apolytikion', block);
        continue;
      }
      // Every page lists the Theotokia of all eight modes; keep only this mode's.
      const own = isThisModeTheotokion(h, mode.label);
      if (own === true && vespersPhase === 'aposticha' && !apostichaTheotokionTaken) {
        apostichaTheotokionTaken = true; add('aposticha', block, { note: 'Θεοτοκίο' }); continue;
      }
      if (own === true && vespersPhase === 'apolytikion' && !apolytikionTheotokionTaken) {
        apolytikionTheotokionTaken = true; add('apolytikion', block, { note: 'Θεοτοκίο' }); continue;
      }
      continue;
    }
    if (block.service === 'orthros') {
      if (/ΚΑΝΩΝ ΑΝΑΣΤΑΣΙΜΟΣ/.test(h)) canon = 'anastasimos';
      else if (/ΚΑΝΩΝ /.test(h)) canon = 'other';
      if (/^ΚΑΘΙΣΜΑΤΑ ΑΝΑΣΤΑΣΙΜΑ/.test(h)) {
        orthrosSection = 'kathismata';
        add('kathismata', block, block.notes.includes('kai_nyn') ? { note: 'Θεοτοκίο' } : {}); continue;
      }
      // Some pages give a sessional hymn's Theotokion its own «Θεοτοκίον» heading.
      if (h === 'ΘΕΟΤΟΚΙΟΝ' && orthrosSection === 'kathismata') { add('kathismata', block, { note: 'Θεοτοκίο' }); continue; }
      if (!/^ΘΕΟΤΟΚΙΟΝ/.test(h)) orthrosSection = null;
      if (/^(Η )?ΥΠΑΚΟΗ/.test(h)) { add('ypakoe', block); continue; }
      if (/^ΟΙ ΑΝΑΒΑΘΜΟΙ|^ΑΝΤΙΦΩΝΟΝ/.test(h)) {
        // One entry per antiphon: the first troparion after each antiphon start — a new heading or
        // an «Ἀντίφωνον Α΄» label line. Paragraph breaks and the «Δόξα…» Triadikon start nothing.
        if (block.heading !== anavathmoiHeading) { anavathmoiHeading = block.heading; needAntiphon = true; }
        if (isCue(block)) continue;
        for (const line of block.text) {
          if (/^ΑΝΤΙΦΩΝΟΝ/.test(norm(line))) { needAntiphon = true; continue; }
          if (needAntiphon) { addText('anavathmoi', [line.replace(/^[-–]\s*/, '')]); needAntiphon = false; }
        }
        continue;
      }
      if (/^ΠΡΟΚΕΙΜΕΝΟΝ/.test(h)) { add('prokeimenon', block); continue; }
      if (canon === 'anastasimos' && block.notes.some((note) => /ΕΙΡΜΟΣ/.test(norm(note)))) {
        const odeLetter = (h.match(/^ΩΔΗ ([Α-Ω]+)/) || [])[1];
        const label = ODE_LABELS[odeLetter] || ODES[groups.heirmoi.length];
        addText('heirmoi', block.text, { note: `Ωδή ${label}` });
        continue;
      }
      if (/^ΚΟΝΤΑΚΙΟΝ/.test(h)) { add('kontakion', block); continue; }
      // The Praises; their «Δόξα…» (Eothinon) and «Καὶ νῦν…» (Ὑπερευλογημένη) are not per mode.
      if (!isCue(block) && /^ΣΤΙΧΗΡΑ ΑΝΑΣΤΑΣΙΜΑ/.test(h)) { add('ainoi_anastasima', block); continue; }
      if (!isCue(block) && /^ΕΤΕΡΑ ΣΤΙΧΗΡΑ ΑΝΑΤΟΛΙΚΑ/.test(h)) { add('ainoi_anatolika', block); continue; }
      continue;
    }
    if (block.service === 'liturgy' && !isCue(block)) liturgyLines.push(...block.text);
  }
  // Beatitudes: verses («Μακάριοι…», «Χαίρετε…») alternate with troparia; keep the first troparion.
  const isVerse = (t) => /^(ΜΑΚΑΡΙΟΙ|ΧΑΙΡΕΤΕ) /.test(norm(t));
  const lines = liturgyLines.filter((t) => !/^ΟΙ ΜΑΚΑΡΙΣΜΟΙ$/.test(norm(t)));
  const firstVerse = lines.findIndex(isVerse);
  if (firstVerse >= 0) {
    const troparion = [];
    for (let i = firstVerse + 1; i < lines.length && !isVerse(lines[i]); i += 1) troparion.push(lines[i]);
    if (troparion.length) addText('makarismoi', troparion);
  }
  return groups;
}

// ---- The code lock (see "Hymn codes are permanent" in the header) --------------------------------

const CODE = /^\d{2}$/;
const LOCK_ABOUT = 'The code of every Anastasimatarion hymn, locked: recordings folders and analysis settings '
  + 'are keyed by it. Written by scripts/generate-anastasimatarion-catalog.mjs and checked by '
  + 'AnastasimatarionCodeLockTest. Read the header of that script before editing.';

/** What a hymn is, never where it is displayed. Lock entries without `occurrence` are the first. */
export function identityKey({ service, group, incipit, occurrence = 1 }) {
  return `${service}/${group}/${incipit}#${occurrence}`;
}

/** Numbers hymns that share service, group and incipit: 1 for the first, 2 for the second, … */
export function withOccurrences(hymns) {
  const seen = new Map();
  return hymns.map((hymn) => {
    const base = `${hymn.service}/${hymn.group}/${hymn.incipit}`;
    const occurrence = (seen.get(base) ?? 0) + 1;
    seen.set(base, occurrence);
    return { ...hymn, occurrence };
  });
}

/** A lock entry: the code and the identity, `occurrence` only when it is not the first. */
function lockEntry({ code, service, group, incipit, occurrence = 1 }) {
  return { code, service, group, incipit, ...(occurrence > 1 ? { occurrence } : {}) };
}

function withoutCode({ code, ...identity }) {
  return identity;
}

function describe(hymn) {
  const occurrence = hymn.occurrence ?? 1;
  return `«${hymn.incipit}» (${hymn.service}/${hymn.group}${occurrence > 1 ? `, occurrence ${occurrence}` : ''})`;
}

const byCode = (a, b) => a.code.localeCompare(b.code);

/**
 * One mode's codes, from its lock entry. Pure: `hymns` are the mode's hymns in display order, each
 * { service, group, incipit }. Returns each hymn's code (same order), the lock entry to write back
 * and what changed. Throws — having decided nothing — when the lock cannot be honoured.
 *
 * A code is only ever looked up by identity, so the display order cannot move one; the only way a
 * code comes to name another hymn is an explicit "rename" in the lock.
 */
export function assignCodes(modeKey, hymns, entry = {}) {
  const problems = [];
  const locked = new Map(); // code → lock entry
  const codeOf = new Map(); // identity → code
  for (const hymn of entry.hymns ?? []) {
    if (!CODE.test(hymn.code)) problems.push(`${modeKey}: locked code "${hymn.code}" is not two digits`);
    if (locked.has(hymn.code)) problems.push(`${modeKey} ${hymn.code}: locked twice`);
    const id = identityKey(hymn);
    if (codeOf.has(id)) problems.push(`${modeKey}: ${describe(hymn)} is locked twice, as ${codeOf.get(id)} and ${hymn.code}`);
    locked.set(hymn.code, lockEntry(hymn));
    codeOf.set(id, hymn.code);
  }
  const retired = [...(entry.retired ?? [])];
  const retiredCodes = new Set();
  for (const hymn of retired) {
    if (!CODE.test(hymn.code)) problems.push(`${modeKey}: retired code "${hymn.code}" is not two digits`);
    if (retiredCodes.has(hymn.code)) problems.push(`${modeKey} ${hymn.code}: retired twice`);
    if (locked.has(hymn.code)) problems.push(`${modeKey} ${hymn.code}: both locked and retired — a retired code is never used again`);
    retiredCodes.add(hymn.code);
  }
  const renamed = [...(entry.renamed ?? [])];
  const report = { added: [], renamed: [], retired: [] };

  for (const rename of entry.rename ?? []) {
    const before = locked.get(rename.code);
    if (!before) { problems.push(`${modeKey} rename ${rename.code}: not a locked code`); continue; }
    if (!rename.service || !rename.group || !rename.incipit) {
      problems.push(`${modeKey} rename ${rename.code}: needs "service", "group" and "incipit"`);
      continue;
    }
    const after = lockEntry(rename);
    if (identityKey(after) === identityKey(before)) continue; // already applied
    const holder = codeOf.get(identityKey(after));
    if (holder) {
      problems.push(`${modeKey} rename ${rename.code}: ${describe(after)} already has code ${holder}`);
      continue;
    }
    codeOf.delete(identityKey(before));
    codeOf.set(identityKey(after), rename.code);
    locked.set(rename.code, after);
    renamed.push({ code: rename.code, from: withoutCode(before), to: withoutCode(after) });
    report.renamed.push(rename.code);
  }

  const toRetire = new Set();
  for (const retire of entry.retire ?? []) {
    if (!locked.has(retire.code)) { problems.push(`${modeKey} retire ${retire.code}: not a locked code`); continue; }
    toRetire.add(retire.code);
  }

  const current = withOccurrences(hymns);
  const produced = new Set(current.map(identityKey));
  for (const code of toRetire) {
    if (produced.has(identityKey(locked.get(code)))) {
      problems.push(`${modeKey} retire ${code}: ${describe(locked.get(code))} is still in the source`);
    }
  }
  const unlocked = current.filter((hymn) => !codeOf.has(identityKey(hymn)));
  for (const hymn of locked.values()) {
    if (toRetire.has(hymn.code) || produced.has(identityKey(hymn))) continue;
    problems.push(
      `${modeKey} ${hymn.code}: the locked hymn ${describe(hymn)} is no longer in the source. `
      + `Resolve it in the lock, under modes.${modeKey}, and run again:\n`
      + `  its text was corrected → "rename": [{"code": "${hymn.code}", "service": "${hymn.service}", `
      + `"group": "${hymn.group}", "incipit": "<the new text>"}]\n`
      + `  it left the catalog    → "retire": [{"code": "${hymn.code}"}] (the code is never given again)`
      + (unlocked.length ? `\n  this mode's hymns not in the lock: ${unlocked.map(describe).join(', ')}` : ''),
    );
  }
  if (problems.length) throw new Error(problems.join('\n'));

  // Locked hymns keep their code; a new hymn takes the next free one, at the end.
  let last = Math.max(0, ...[...locked.keys(), ...retiredCodes].map(Number));
  const codes = current.map((hymn) => {
    const id = identityKey(hymn);
    if (codeOf.has(id)) return codeOf.get(id);
    last += 1;
    const code = String(last).padStart(2, '0');
    locked.set(code, lockEntry({ ...hymn, code }));
    codeOf.set(id, code);
    report.added.push(code);
    return code;
  });
  if (last > 99) throw new Error(`${modeKey}: more than 99 codes — folder names and the lock assume two digits`);
  if (new Set(codes).size !== codes.length) throw new Error(`${modeKey}: a code was given to two hymns`);

  for (const code of toRetire) {
    retired.push(locked.get(code));
    locked.delete(code);
    report.retired.push(code);
  }
  return {
    codes,
    entry: { hymns: [...locked.values()].sort(byCode), renamed, retired: retired.sort(byCode) },
    report,
  };
}

/** The lock that keeps an existing asset's codes exactly as they are. */
export function lockFromAsset(asset) {
  const modes = {};
  for (const mode of asset.modes) {
    const hymns = withOccurrences(mode.services.flatMap((service) => service.groups.flatMap((group) => group.hymns
      .map((hymn) => ({ code: hymn.code, service: service.key, group: group.key, incipit: hymn.incipit })))));
    const codes = hymns.map((hymn) => hymn.code);
    const bad = codes.filter((code, i) => !CODE.test(code) || codes.indexOf(code) !== i);
    if (bad.length) throw new Error(`${mode.key}: codes not two-digit or not unique: ${bad.join(', ')}`);
    modes[mode.key] = { hymns: hymns.map(lockEntry).sort(byCode), renamed: [], retired: [] };
  }
  return { version: 1, about: LOCK_ABOUT, modes };
}

/** One lock entry per line, so a changed code or incipit is a one-line diff. */
export function formatLock(lock) {
  const inline = (value) => (value && typeof value === 'object' && !Array.isArray(value)
    ? `{${Object.entries(value).map(([k, v]) => `${JSON.stringify(k)}: ${inline(v)}`).join(', ')}}`
    : JSON.stringify(value));
  const list = (items, indent) => (items.length
    ? `[\n${items.map((item) => `${indent} ${inline(item)}`).join(',\n')}\n${indent}]`
    : '[]');
  const modes = Object.entries(lock.modes).map(([key, entry]) => [
    `  ${JSON.stringify(key)}: {`,
    `   "hymns": ${list(entry.hymns, '   ')},`,
    `   "renamed": ${list(entry.renamed, '   ')},`,
    `   "retired": ${list(entry.retired, '   ')}`,
    '  }',
  ].join('\n'));
  return `{\n "version": ${lock.version},\n "about": ${JSON.stringify(lock.about)},\n "modes": {\n${modes.join(',\n')}\n }\n}\n`;
}

export function readLock(file) {
  if (!fs.existsSync(file)) {
    throw new Error(`No code lock at ${file}. Create it once, from the shipped asset: --init-lock-from-asset`);
  }
  const lock = JSON.parse(fs.readFileSync(file, 'utf8'));
  if (lock.version !== 1 || !lock.modes || typeof lock.modes !== 'object') throw new Error(`${file}: not a version 1 code lock`);
  const unknown = Object.keys(lock.modes).filter((key) => !MODES.some((mode) => mode.key === key));
  if (unknown.length) throw new Error(`${file}: modes the catalog does not have: ${unknown.join(', ')}`);
  // A mode missing from the lock would restart its codes at 01: restore it from git instead.
  const missing = MODES.filter((mode) => !lock.modes[mode.key]).map((mode) => mode.key);
  if (missing.length) throw new Error(`${file}: no entry for ${missing.join(', ')} — restore it from git history`);
  return lock;
}

function initLockFromAsset(args) {
  if (fs.existsSync(args.lock)) {
    throw new Error(`${args.lock} already exists. It records every code ever given, retired ones included, `
      + 'and an asset cannot say which were retired: it is never rebuilt from an asset.');
  }
  const lock = lockFromAsset(JSON.parse(fs.readFileSync(args.out, 'utf8')));
  fs.writeFileSync(args.lock, formatLock(lock));
  for (const [key, entry] of Object.entries(lock.modes)) console.log(`${key.padEnd(14)} ${String(entry.hymns.length).padStart(2)} codes locked`);
  console.log(`wrote ${path.relative(ROOT, args.lock)} from ${path.relative(ROOT, args.out)}`);
}

async function loadPage(mode, cacheDir) {
  const cached = cacheDir ? path.join(cacheDir, mode.page) : null;
  if (cached && fs.existsSync(cached)) return fs.readFileSync(cached, 'utf8');
  if (!SOURCE_URL) {
    throw new Error(
      'ANASTASIMATARION_SOURCE_URL is not set, and no cached page was found for ' + mode.page +
      '. The source is deliberately not stored in this repository — see the file header.',
    );
  }
  const response = await fetch(SOURCE_URL.replace(/\/?$/, '/') + mode.page);
  if (!response.ok) throw new Error(`${mode.page}: HTTP ${response.status}`);
  const html = await response.text();
  if (cached) { fs.mkdirSync(cacheDir, { recursive: true }); fs.writeFileSync(cached, html); }
  return html;
}

async function main() {
  const args = parseArgs(process.argv.slice(2));
  if (args.initLock) { initLockFromAsset(args); return; }
  const lock = readLock(args.lock);
  // No `source` block: the asset must carry no source and no URL (see the file header).
  const catalog = {
    version: 1,
    modes: [],
  };
  const nextLock = { version: 1, about: LOCK_ABOUT, modes: {} };
  for (const mode of MODES) {
    const groups = selectHymns(toBlocks(toLines(await loadPage(mode, args.cacheDir))), mode);
    const hymns = SERVICES.flatMap((service) => service.groups.flatMap((g) => groups[g]
      .map((hymn) => ({ service: service.key, group: g, incipit: hymn.incipit }))));
    // Codes from the lock, never from the display order. Throws before anything is written.
    const { codes, entry, report } = assignCodes(mode.key, hymns, lock.modes[mode.key]);
    let next = 0;
    const services = SERVICES.map((service) => ({
      key: service.key,
      groups: service.groups.filter((g) => groups[g].length).map((g) => ({
        key: g,
        hymns: groups[g].map((hymn) => ({ code: codes[next++], ...hymn })),
      })),
    }));
    catalog.modes.push({ key: mode.key, services });
    nextLock.modes[mode.key] = entry;
    const summary = SERVICES.flatMap((s) => s.groups.map((g) => `${g}=${groups[g].length}`)).join(' ');
    const changes = Object.entries(report).filter(([, list]) => list.length).map(([kind, list]) => `${kind} ${list.join(',')}`);
    console.log(`${mode.key.padEnd(14)} ${String(codes.length).padStart(2)} hymns | ${summary}${changes.length ? ` | ${changes.join(' ')}` : ''}`);
  }
  fs.writeFileSync(args.out, `${JSON.stringify(catalog, null, 1)}\n`);
  fs.writeFileSync(args.lock, formatLock(nextLock));
  console.log(`wrote ${path.relative(ROOT, args.out)} and ${path.relative(ROOT, args.lock)}`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((error) => { console.error(error); process.exit(1); });
}
