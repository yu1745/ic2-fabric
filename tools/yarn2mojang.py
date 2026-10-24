#!/usr/bin/env python3
"""把仓库代码从 yarn 名批量替换为 mojang 名。
用法: python3 yarn2mojang.py <tsv> <srcroot1> [<srcroot2>...]
只处理 net.minecraft.* 类; 只替换 import 与 FQCN 全路径, 简名仅在唯一映射时替换。
"""
import sys, re, os, collections

tsv, roots = sys.argv[1], sys.argv[2:]
table = {}
for line in open(tsv):
    if line.startswith('#'): continue
    y, m, kind = line.split('\t')[:3]
    if kind == 'class':
        table[y] = m

used = set()
files = []
for root in roots:
    for dp, _, fns in os.walk(root):
        for fn in fns:
            if fn.endswith(('.kt', '.java')):
                p = os.path.join(dp, fn)
                files.append(p)
                for line in open(p, encoding='utf-8', errors='replace'):
                    for mm in re.finditer(r'\b(net\.minecraft(?:\.\w+)+)\b', line):
                        used.add(mm.group(1))
print('files:', len(files), 'used net.minecraft FQCNs:', len(used))

mapped = {y: m for y, m in table.items() if y in used}
print('mapped:', len(mapped), 'unmapped:', len(used - set(table)))
for y in sorted(used - set(table))[:10]:
    print('  unmapped e.g.', y)

# simple-name ambiguity check
by_simple = collections.defaultdict(set)
for y, m in mapped.items():
    by_simple[y.rsplit('.', 1)[1]].add((y, m))
simple_repl = {}
for s, pairs in by_simple.items():
    moj = {m for _, m in pairs}
    if len(pairs) == 1:
        simple_repl[s] = next(iter(moj)).rsplit('.', 1)[1]

# own-class collision check: our declarations with same simple name as a rename target
own_names = set()
decl = re.compile(r'^(?:class|object|interface|enum class)\s+(\w+)', re.M)
for p in files:
    try: own_names |= set(decl.findall(open(p, encoding='utf-8', errors='replace').read()))
    except: pass
renames = {s: m for s, m in simple_repl.items() if s != m}
collide = {s for s in renames if s in own_names}
print('simple renames:', len(renames), 'colliding with own classes (skipped):', sorted(collide))
for s in collide: del renames[s]

fq_repl = sorted(mapped.items(), key=lambda kv: -len(kv[0]))

def convert(text):
    for y, m in fq_repl:
        text = re.sub(r'\b%s\b' % re.escape(y), m, text)
    if renames:
        pat = re.compile(r'\b(%s)\b' % '|'.join(map(re.escape, sorted(renames, key=len, reverse=True))))
        text = pat.sub(lambda m: renames[m.group(1)], text)
    return text

changed = 0
for p in files:
    src = open(p, encoding='utf-8', errors='replace').read()
    out = convert(src)
    if out != src:
        open(p, 'w', encoding='utf-8').write(out)
        changed += 1
print('changed files:', changed)
