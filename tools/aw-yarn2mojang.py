#!/usr/bin/env python3
"""accesswidener yarn -> mojang。用法: python3 aw-yarn2mojang.py <tsv> <awfile...>"""
import sys, re
tsv, files = sys.argv[1], sys.argv[2:]
cls, mem = {}, {}
for line in open(tsv):
    if line.startswith('#'): continue
    y, m, kind, desc = (line.split('\t') + [''])[:4]
    if kind == 'class':
        cls[y] = m
    else:
        mem[(y.rsplit('.', 1)[0], y.rsplit('.', 1)[1], re.sub(r'L([^;]+);', r'\1', desc))] = m.rsplit('.', 1)[1]

def conv_desc(d):
    d = re.sub(r'L([^;]+);', lambda mm: 'L%s;' % cls.get(mm.group(1), mm.group(1)), d)
    return d

def canon(d):
    d = re.sub(r'L([^;]+);', r'\1', d)
    return re.sub(r'[A-Za-z_$][\w$]*(?:[./][\w$]+)+', lambda m: m.group(0).split('/')[-1].split('.')[-1], d)

for f in files:
    out = []
    for line in open(f):
        line = line.rstrip('\n')
        if line.startswith('#') or not line.strip() or line.startswith('accessWidener'):
            out.append(line); continue
        parts = line.split()
        # e.g. accessible method <class> <name> <desc>
        if len(parts) >= 4 and parts[1] in ('method', 'field'):
            c = cls.get(parts[2].replace('/', '.'), parts[2].replace('/', '.'))
            name, desc = parts[3], parts[4] if len(parts) > 4 else ''
            if parts[1] == 'method' and name != '<init>':
                name = mem.get((parts[2].replace('/', '.'), name, canon(desc)), name)
            elif parts[1] == 'field':
                name = mem.get((parts[2].replace('/', '.'), name, canon(desc)), name)
            parts[2] = c.replace('.', '/')
            parts[3] = name
            if len(parts) > 4: parts[4] = conv_desc(desc)
            out.append('\t'.join(['accessible' if p == 'accessible' else ('mutable' if p == 'mutable' else p) for p in parts]) if False else ' '.join(parts))
        else:
            out.append(line)
    open(f, 'w').write('\n'.join(out) + '\n')
    print('converted', f)
