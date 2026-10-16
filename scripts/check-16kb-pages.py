#!/usr/bin/env python3
"""Fails when a native library in an APK or AAB is not 16 KB page aligned.

Google Play rejects apps with native code that target Android 15+ unless every LOAD segment of
every .so is aligned to at least 16 KB. FFmpegKit (4 KB) was the reason it was removed
(ClickUp 869fhmxpt). Run this on every release build:

    python3 scripts/check-16kb-pages.py app/build/outputs/apk/release/*.apk

Pure standard library: reads the ELF program headers straight out of the archive.
"""
import struct
import sys
import zipfile

PAGE = 16 * 1024
PT_LOAD = 1


def load_alignments(data: bytes):
    if data[:4] != b"\x7fELF":
        raise ValueError("not an ELF file")
    is64 = data[4] == 2
    endian = "<" if data[5] == 1 else ">"
    if is64:
        phoff, = struct.unpack_from(endian + "Q", data, 0x20)
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 0x36)
    else:
        phoff, = struct.unpack_from(endian + "I", data, 0x1C)
        phentsize, phnum = struct.unpack_from(endian + "HH", data, 0x2A)
    for i in range(phnum):
        base = phoff + i * phentsize
        p_type, = struct.unpack_from(endian + "I", data, base)
        if p_type != PT_LOAD:
            continue
        if is64:
            p_align, = struct.unpack_from(endian + "Q", data, base + 0x30)
        else:
            p_align, = struct.unpack_from(endian + "I", data, base + 0x1C)
        yield p_align


def check(path: str) -> int:
    bad = 0
    libs = 0
    with zipfile.ZipFile(path) as archive:
        for name in sorted(archive.namelist()):
            if not name.endswith(".so"):
                continue
            libs += 1
            aligns = list(load_alignments(archive.read(name)))
            worst = min(aligns) if aligns else 0
            status = "ok " if worst >= PAGE else "BAD"
            if worst < PAGE:
                bad += 1
            print(f"{status} {worst:>6}  {name}")
    print(f"{path}: {libs} native libraries, {bad} not 16 KB aligned")
    return bad


def main(argv) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    return 1 if sum(check(p) for p in argv[1:]) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
