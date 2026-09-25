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
 * Each hymn gets a two-digit code in display order ("01", "02", …). The app names a hymn's
 * recordings folder "<code> <incipit>" and finds it again by the code prefix, so codes must stay
 * stable: regenerate only to fix text, and check the codes did not move (the script prints a
 * per-mode summary; compare it with git diff before committing).
 *
 * Usage (Node 18+, no dependencies):
 *   node scripts/generate-anastasimatarion-catalog.mjs [--cache-dir DIR] [--out FILE]
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
  const args = { cacheDir: null, out: path.join(ROOT, 'app/src/main/assets/anastasimatarion_v1.json') };
  for (let i = 0; i < argv.length; i += 1) {
    if (argv[i] === '--cache-dir') args.cacheDir = argv[++i];
    else if (argv[i] === '--out') args.out = argv[++i];
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
  // No `source` block: the asset must carry no source and no URL (see the file header).
  const catalog = {
    version: 1,
    modes: [],
  };
  for (const mode of MODES) {
    const groups = selectHymns(toBlocks(toLines(await loadPage(mode, args.cacheDir))), mode);
    let code = 0;
    const services = SERVICES.map((service) => ({
      key: service.key,
      groups: service.groups.filter((g) => groups[g].length).map((g) => ({
        key: g,
        hymns: groups[g].map((hymn) => ({ code: String(++code).padStart(2, '0'), ...hymn })),
      })),
    }));
    catalog.modes.push({ key: mode.key, services });
    const summary = SERVICES.flatMap((s) => s.groups.map((g) => `${g}=${groups[g].length}`)).join(' ');
    console.log(`${mode.key.padEnd(14)} ${String(code).padStart(2)} hymns | ${summary}`);
  }
  fs.writeFileSync(args.out, `${JSON.stringify(catalog, null, 1)}\n`);
  console.log(`wrote ${path.relative(ROOT, args.out)}`);
}

if (process.argv[1] && fileURLToPath(import.meta.url) === path.resolve(process.argv[1])) {
  main().catch((error) => { console.error(error); process.exit(1); });
}
