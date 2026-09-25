/**
 * The hymn-code lock of scripts/generate-anastasimatarion-catalog.mjs (ClickUp 869f5x2a9).
 *
 * The generator cannot run without the confidential source, which is stored nowhere, so its
 * locking rules are tested here as the pure function they live in, `assignCodes`, against the
 * shipped asset and the committed lock. Run: node --test scripts/tests/
 *
 * CI runs no Node. AnastasimatarionCodeLockTest (JVM) is the gate on the committed files; this
 * file shows the generator would keep them valid.
 */
import { test } from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import {
  assignCodes, formatLock, identityKey, lockFromAsset, readLock, withOccurrences,
} from '../generate-anastasimatarion-catalog.mjs';

const ROOT = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const asset = JSON.parse(fs.readFileSync(path.join(ROOT, 'app/src/main/assets/anastasimatarion_v1.json'), 'utf8'));
const lockText = fs.readFileSync(path.join(ROOT, 'scripts/anastasimatarion-codes.lock.json'), 'utf8');
const lock = JSON.parse(lockText);

/** A mode's hymns in display order, each with the code the asset gives it. */
function displayed(modeKey) {
  const mode = asset.modes.find((m) => m.key === modeKey);
  return mode.services.flatMap((service) => service.groups.flatMap((group) => group.hymns
    .map((hymn) => ({ service: service.key, group: group.key, incipit: hymn.incipit, code: hymn.code }))));
}

/** What the generator hands to assignCodes: the hymns, without codes. */
const bare = (hymns) => hymns.map(({ code, ...hymn }) => hymn);

test('regenerating the shipped hymns moves no code in any mode', () => {
  assert.equal(asset.modes.length, 8);
  for (const mode of asset.modes) {
    const hymns = displayed(mode.key);
    const { codes, entry, report } = assignCodes(mode.key, bare(hymns), lock.modes[mode.key]);
    assert.deepEqual(codes, hymns.map((hymn) => hymn.code), mode.key);
    assert.deepEqual(entry, lock.modes[mode.key], mode.key);
    assert.deepEqual(report, { added: [], renamed: [], retired: [] }, mode.key);
  }
});

test('the committed lock is what the generator writes, and matches the shipped asset', () => {
  assert.equal(formatLock(lock), lockText);
  const fromAsset = lockFromAsset(asset);
  for (const key of Object.keys(lock.modes)) assert.deepEqual(fromAsset.modes[key].hymns, lock.modes[key].hymns, key);
});

test('a hymn displayed early gets the next free code, at the end, and no other code moves', () => {
  const hymns = bare(displayed('first'));
  const newcomer = { service: 'vespers', group: 'stichera_anastasima', incipit: 'Νέος ύμνος πρώτος στη σειρά' };
  hymns.splice(1, 0, newcomer);
  const { codes, entry, report } = assignCodes('first', hymns, lock.modes.first);
  assert.equal(codes[1], '46');
  assert.deepEqual(codes.filter((_, i) => i !== 1), displayed('first').map((hymn) => hymn.code));
  assert.deepEqual(report, { added: ['46'], renamed: [], retired: [] });
  assert.deepEqual(entry.hymns.at(-1), { code: '46', ...newcomer });
});

test('a code follows its hymn, not its position', () => {
  const swapped = displayed('first');
  [swapped[1], swapped[2]] = [swapped[2], swapped[1]];
  assert.deepEqual(assignCodes('first', bare(swapped), lock.modes.first).codes, swapped.map((hymn) => hymn.code));
});

test('a locked hymn the source no longer yields stops the run — even for one accent', () => {
  const hymns = bare(displayed('first'));
  hymns[1] = { ...hymns[1], incipit: 'Τὰς ἑσπερινὰς ἡμῶν εὐχὰς' };
  assert.throws(
    () => assignCodes('first', hymns, lock.modes.first),
    /first 02: the locked hymn «Τὰς ἑσπερινὰς ἡμῶν εὐχάς».*no longer in the source[\s\S]*"rename"[\s\S]*"retire"[\s\S]*not in the lock: «Τὰς ἑσπερινὰς ἡμῶν εὐχὰς»/,
  );
});

test('a rename keeps the code for the corrected text, records it, and is consumed', () => {
  const fixed = 'Τὰς ἑσπερινὰς ἡμῶν εὐχὰς';
  const hymns = bare(displayed('first'));
  hymns[1] = { ...hymns[1], incipit: fixed };
  const rename = [{ code: '02', service: 'vespers', group: 'stichera_anastasima', incipit: fixed }];
  const result = assignCodes('first', hymns, { ...lock.modes.first, rename });
  assert.equal(result.codes[1], '02');
  assert.deepEqual(result.report, { added: [], renamed: ['02'], retired: [] });
  assert.equal(result.entry.hymns.find((hymn) => hymn.code === '02').incipit, fixed);
  assert.deepEqual(result.entry.renamed, [{
    code: '02',
    from: { service: 'vespers', group: 'stichera_anastasima', incipit: 'Τὰς ἑσπερινὰς ἡμῶν εὐχάς' },
    to: { service: 'vespers', group: 'stichera_anastasima', incipit: fixed },
  }]);
  assert.equal('rename' in result.entry, false);
  // The written lock needs no instruction the next time.
  assert.deepEqual(assignCodes('first', hymns, result.entry).codes, result.codes);
});

test('a retired code is recorded for good and never given again, even to the same text', () => {
  const leaving = 'Κυκλώσατε λαοὶ Σιών';
  const hymns = bare(displayed('first')).filter((hymn) => hymn.incipit !== leaving);
  const retired = assignCodes('first', hymns, { ...lock.modes.first, retire: [{ code: '03' }] });
  assert.deepEqual(retired.report, { added: [], renamed: [], retired: ['03'] });
  assert.equal(retired.entry.hymns.some((hymn) => hymn.code === '03'), false);
  assert.deepEqual(retired.entry.retired.map((hymn) => hymn.code), ['03']);
  const back = [...hymns, { service: 'vespers', group: 'stichera_anastasima', incipit: leaving }];
  assert.equal(assignCodes('first', back, retired.entry).codes.at(-1), '46');
});

test('the highest code, once retired, is still never given again', () => {
  const hymns = bare(displayed('first'));
  const last = hymns.pop();
  const retired = assignCodes('first', hymns, { ...lock.modes.first, retire: [{ code: '45' }] });
  assert.equal(assignCodes('first', [...hymns, { ...last, incipit: 'Άλλος ύμνος' }], retired.entry).codes.at(-1), '46');
});

test('an instruction that does not fit is refused', () => {
  const hymns = bare(displayed('first'));
  assert.throws(() => assignCodes('first', hymns, { ...lock.modes.first, retire: [{ code: '03' }] }), /retire 03: .* is still in the source/);
  assert.throws(() => assignCodes('first', hymns, { ...lock.modes.first, retire: [{ code: '77' }] }), /retire 77: not a locked code/);
  const taken = { code: '02', service: 'vespers', group: 'stichera_anastasima', incipit: 'Κυκλώσατε λαοὶ Σιών' };
  assert.throws(() => assignCodes('first', hymns, { ...lock.modes.first, rename: [taken] }), /rename 02: .* already has code 03/);
});

test('the Βαρύς twins share an incipit and are still two hymns', () => {
  const hymns = displayed('varys');
  const twins = hymns.filter((hymn) => hymn.incipit === 'Δεῦτε ἀγαλλιασώμεθα τῷ Κυρίῳ');
  assert.deepEqual(twins.map((hymn) => hymn.code), ['02', '42']);
  assert.notEqual(identityKey(twins[0]), identityKey(twins[1]));
  assert.deepEqual(assignCodes('varys', bare(hymns), lock.modes.varys).codes, hymns.map((hymn) => hymn.code));
});

test('twins inside one group are told apart by occurrence', () => {
  const twin = { service: 'vespers', group: 'aposticha', incipit: 'Ίδιος ύμνος' };
  assert.deepEqual(withOccurrences([twin, twin, { ...twin, group: 'ainoi' }]).map((hymn) => hymn.occurrence), [1, 2, 1]);
  const { codes, entry } = assignCodes('m', [twin, twin], {});
  assert.deepEqual(codes, ['01', '02']);
  assert.deepEqual(entry.hymns[1], { code: '02', ...twin, occurrence: 2 });
});

test('a broken lock is refused before anything is assigned', () => {
  const a = { service: 's', group: 'g', incipit: 'A' };
  const b = { service: 's', group: 'g', incipit: 'B' };
  assert.throws(() => assignCodes('m', [a, b], { hymns: [{ code: '01', ...a }, { code: '01', ...b }] }), /m 01: locked twice/);
  assert.throws(() => assignCodes('m', [a], { hymns: [{ code: '01', ...a }], retired: [{ code: '01', ...b }] }), /both locked and retired/);
  assert.throws(() => assignCodes('m', [a], { hymns: [{ code: '1', ...a }] }), /not two digits/);
});

test('more than 99 codes is refused', () => {
  const many = Array.from({ length: 100 }, (_, i) => ({ service: 's', group: 'g', incipit: `Ύμνος ${i}` }));
  assert.throws(() => assignCodes('m', many, {}), /more than 99/);
});

test('a lock that lost a mode is refused, rather than restarting that mode at 01', () => {
  const dir = fs.mkdtempSync(path.join(os.tmpdir(), 'hymn-lock-'));
  try {
    const file = path.join(dir, 'lock.json');
    const { varys, ...others } = lock.modes;
    fs.writeFileSync(file, JSON.stringify({ ...lock, modes: others }));
    assert.throws(() => readLock(file), /no entry for varys/);
    fs.writeFileSync(file, lockText);
    assert.deepEqual(readLock(file), lock);
  } finally {
    fs.rmSync(dir, { recursive: true, force: true });
  }
});
