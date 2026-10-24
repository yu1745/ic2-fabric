#!/usr/bin/env python3
"""生成 yarn↔mojang 映射转换表（按 obfuscated 名 + 描述符 join）。

用法: python3 gen-yarn-mojang-table.py <yarn_mappings.jar> <mojang_client.txt> <out.tsv>

- yarn mappings.jar 内的 mappings.tiny (v2): official(obf) / intermediary / named(yarn)，descriptor 为 obf 空间
- mojang client.txt: lined proguard 格式 `mojang -> obf:`，descriptor 为 mojang 空间（经类表翻译回 obf 再 join）
- 重载方法 proguard 共享 obf 名，必须按 (obf类, obf成员名, obf描述符) join
"""
import sys, zipfile, re
from collections import defaultdict

DESC_TYPE = re.compile(r'L([^;]+);')

def canonical_desc(desc):
    # 统一两侧描述符: L..; -> 名, 包名剥掉, 只留最后段
    desc = re.sub(r'L([^;]+);', r'\1', desc)
    return re.sub(r'[A-Za-z_$][\w$]*(?:[./][\w$]+)+',
                  lambda m: m.group(0).split('/')[-1].split('.')[-1], desc)

def parse_yarn_tiny(jar):
    classes = {}                      # obf -> yarn
    members = defaultdict(dict)       # (obfClass, obfMember, obfDesc) -> (yarnName, kind)
    with zipfile.ZipFile(jar) as z:
        data = z.read('mappings/mappings.tiny').decode()
    cur = None
    for line in data.splitlines():
        if not line or line.startswith('#'):
            continue
        parts = line.strip().split('\t')
        if parts[0] == 'c':
            if len(parts) < 4 or ' ' in parts[1]:
                continue  # javadoc 注释行
            cur = parts[1]
            classes[cur] = parts[3]
        elif parts[0] in ('m', 'f'):
            if len(parts) < 5:
                continue
            desc, off, named = parts[1], parts[2], parts[4]
            kind = 'method' if parts[0] == 'm' else 'field'
            members[(cur, off, canonical_desc(desc))] = (named, kind)
    return classes, members

def parse_mojang(path, cls_to_obf):
    lines = [l for l in open(path) if l.strip() and not l.lstrip().startswith('#')]
    # 第一遍：建全量 mojang类名 -> obf 类表
    for line in lines:
        if not line.startswith(' ') and not line.startswith('\t'):
            m = re.match(r'^(\S+) -> (\S+):$', line.rstrip('\n'))
            if m:
                cls_to_obf[m.group(1)] = m.group(2)
    DOTTED = re.compile(r'[A-Za-z_$][\w$]*(?:\.[\w$]+)+')  # proguard 描述符里的点号类名
    def to_obf(desc):
        return DOTTED.sub(lambda m: cls_to_obf.get(m.group(0), m.group(0)), desc)
    members = defaultdict(dict)       # (obfClass, obfName, obfDesc) -> {mojangName}
    cur = None
    for line in lines:
        line = line.rstrip('\n')
        if not line.startswith(' ') and not line.startswith('\t'):
            m = re.match(r'^(\S+) -> (\S+):$', line)
            if m:
                cur = m.group(2)      # obf
            continue
        body = re.sub(r'^(?:\s*\d+:\d+:)+', '', line.strip())
        m = re.match(r'^(\S[^ ]*) ([^ (]+)\((.*)\) -> (\S+)$', body)
        if m:
            ret, name, params, obf = m.groups()
            desc = to_obf('(%s)%s' % (params, ret))
            members[(cur, obf, canonical_desc(desc))][name] = True
            continue
        m = re.match(r'^(\S+) (\S+) -> (\S+)$', body)
        if m:
            members[(cur, m.group(3), canonical_desc(to_obf(m.group(1))))][m.group(2)] = True
    return members

def main():
    jar, txt, out = sys.argv[1], sys.argv[2], sys.argv[3]
    yc, ymem = parse_yarn_tiny(jar)
    cls_to_obf = {v: k for k, v in yc.items()}   # mojang类名 -> obf（用 yarn 表兜底，mojang 行继续补全）
    mmem = parse_mojang(txt, cls_to_obf)
    moj_by_obf = {}
    for moj, obf in cls_to_obf.items():
        moj_by_obf[obf] = moj
    rows, ambiguous = 0, 0
    with open(out, 'w') as f:
        f.write('# yarn\tmojang\tkind\tdesc(obf)\n')
        for obf, yname in sorted(yc.items(), key=lambda kv: kv[1]):
            mname = moj_by_obf.get(obf)
            if mname:
                f.write(f'{yname}\t{mname}\tclass\t\n'.replace('/', '.'))
                rows += 1
        for (cls, off, desc), (yname, kind) in sorted(ymem.items()):
            ycls, mcls = yc.get(cls), moj_by_obf.get(cls)
            names = mmem.get((cls, off, desc))
            if not (ycls and mcls and names) or len(names) != 1:
                ambiguous += bool(names and len(names) > 1)
                continue
            f.write(f'{ycls}.{yname}\t{mcls}.{next(iter(names))}\t{kind}\t{desc}\n'.replace('/', '.'))
            rows += 1
    print(f'wrote {rows} rows to {out} (skipped-ambiguous: {ambiguous})')

main()
