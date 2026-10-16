#!/usr/bin/env python3
"""Fails when a native library in an APK or AAB is not 16 KB page aligned.

Google Play rejects apps with native code that target Android 15+ unless every LOAD segment of
every .so is aligned to at least 16 KB, and every .so stored uncompressed in the archive starts at
a 16 KB offset (so it can be mapped straight from the APK). FFmpegKit (4 KB) was the reason it was removed
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


def data_offset(raw, info: zipfile.ZipInfo) -> int:
    """Where the entry's bytes start: after its local header, whose extra field may differ from the central one."""
    raw.seek(info.header_offset)
    local = raw.read(30)
    name_length, extra_length = struct.unpack_from("<HH", local, 26)
    return info.header_offset + 30 + name_length + extra_length


def check(path: str) -> int:
    bad = 0
    libs = 0
    with open(path, "rb") as raw, zipfile.ZipFile(raw) as archive:
        for info in sorted(archive.infolist(), key=lambda i: i.filename):
            name = info.filename
            if not name.endswith(".so"):
                continue
            libs += 1
            aligns = list(load_alignments(archive.read(name)))
            worst = min(aligns) if aligns else 0
            stored = info.compress_type == zipfile.ZIP_STORED
            offset = data_offset(raw, info) if stored else None
            ok = worst >= PAGE and (not stored or offset % PAGE == 0)
            if not ok:
                bad += 1
            where = f"zip offset {offset}" if stored else "compressed"
            print(f"{'ok ' if ok else 'BAD'} {worst:>6}  {where:<20} {name}")
    print(f"{path}: {libs} native libraries, {bad} not 16 KB aligned")
    return bad


def main(argv) -> int:
    if len(argv) < 2:
        print(__doc__)
        return 2
    return 1 if sum(check(p) for p in argv[1:]) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv))
