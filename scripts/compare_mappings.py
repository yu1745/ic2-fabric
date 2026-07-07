import os
import re
import sys
from collections import Counter

GRADLE_CACHE = os.path.expanduser(
    "~/.gradle/caches/fabric-loom/1.20.1"
)

YARN_TINY = os.path.join(
    GRADLE_CACHE,
    "net.fabricmc.yarn.1_20_1.1.20.1+build.10-v2",
    "mappings.tiny",
)
SRG_TINY = os.path.join(
    GRADLE_CACHE,
    "loom.mappings.1_20_1.layered+hash.40359-v2",
    "mappings-srg.tiny",
)

KT_SEARCH_PATHS = ["core", "advanced-solar-addon", "advanced-weapons-addon", "buildcraft-addon"]

IMPORT_RE = re.compile(r"^\s*import\s+(net\.minecraft\.[\w.*]+)", re.MULTILINE)


def parse_tiny_header(path):
    with open(path, "r", encoding="utf-8") as f:
        first = f.readline().rstrip("\n")
    parts = first.split("\t")
    # tiny v2: parts[0]='tiny', parts[1]=major, parts[2]=minor, parts[3:]=columns
    return parts[3:]


def parse_tiny_classes(path):
    columns = parse_tiny_header(path)
    classes = {}
    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            stripped = line.rstrip("\n")
            if not stripped or stripped.startswith("tiny\t"):
                continue
            parts = stripped.split("\t")
            if parts[0] != "c":
                continue
            values = parts[1:]
            if len(values) < len(columns):
                continue
            row = dict(zip(columns, values))
            intermediary = row.get("intermediary", "")
            if intermediary:
                classes[intermediary] = row
    return classes


def build_yarn_to_mojang():
    yarn_classes = parse_tiny_classes(YARN_TINY)
    srg_classes = parse_tiny_classes(SRG_TINY)

    mapping = {}
    for intermediary, yarn_row in yarn_classes.items():
        yarn_name = yarn_row.get("named", "")
        if not yarn_name or yarn_name == intermediary:
            continue
        srg_row = srg_classes.get(intermediary)
        if not srg_row:
            continue
        mojang_name = srg_row.get("srg", "")
        if not mojang_name:
            continue
        mapping[yarn_name] = mojang_name
    return mapping


def scan_kt_imports():
    imports = Counter()
    root = os.getcwd()
    for search_path in KT_SEARCH_PATHS:
        full = os.path.join(root, search_path)
        if not os.path.isdir(full):
            continue
        for dirpath, _dirs, files in os.walk(full):
            if os.sep + "build" + os.sep in dirpath + os.sep:
                continue
            for fname in files:
                if not fname.endswith(".kt"):
                    continue
                fpath = os.path.join(dirpath, fname)
                try:
                    with open(fpath, "r", encoding="utf-8") as f:
                        content = f.read()
                except Exception:
                    continue
                for m in IMPORT_RE.finditer(content):
                    imports[m.group(1)] += 1
    return imports


def main():
    print("Loading mappings...")
    yarn_to_mojang = build_yarn_to_mojang()
    print(f"  {len(yarn_to_mojang)} classes with both Yarn and Mojang names")

    print("\nScanning .kt files for net.minecraft imports...")
    imports = scan_kt_imports()
    print(f"  {sum(imports.values())} total import statements")
    print(f"  {len(imports)} unique imported paths")

    identical = []
    different = []
    not_found = []

    for imp_path, count in imports.most_common():
        fqcn = imp_path
        is_wildcard = fqcn.endswith(".*")
        if is_wildcard:
            fqcn = fqcn[:-2]
        fqcn_slash = fqcn.replace(".", "/")

        if fqcn_slash in yarn_to_mojang:
            mojang_slash = yarn_to_mojang[fqcn_slash]
            if mojang_slash == fqcn_slash:
                identical.append((imp_path, count))
            else:
                mojang_dot = mojang_slash.replace("/", ".")
                different.append((imp_path, mojang_dot, count))
        else:
            not_found.append((imp_path, count))

    print(f"\n{'='*60}")
    print(f"RESULTS")
    print(f"{'='*60}")
    print(f"  Identical (no change needed):     {len(identical):4d} unique imports")
    print(f"  Different (would need remapping):  {len(different):4d} unique imports")
    print(f"  Not found in mapping:              {len(not_found):4d} unique imports")

    if different:
        print(f"\n{'='*60}")
        print(f"IMPORTS THAT DIFFER (Yarn -> Mojang), showing all {len(different)}:")
        print(f"{'='*60}")
        for yarn_path, mojang_path, count in sorted(different, key=lambda x: x[0]):
            print(f"  {yarn_path}")
            print(f"    -> {mojang_path}  (used {count}x)")

    if not_found:
        print(f"\n{'='*60}")
        print(f"NOT FOUND IN MAPPING ({len(not_found)} unique), first 30:")
        print(f"{'='*60}")
        for imp_path, count in not_found[:30]:
            print(f"  {imp_path}  ({count}x)")
        if len(not_found) > 30:
            print(f"  ... and {len(not_found) - 30} more")


if __name__ == "__main__":
    main()
