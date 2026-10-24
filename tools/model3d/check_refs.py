"""校验 ic2_120 资源中所有 ic2_120: 模型/贴图引用都能解析到文件（迁移后的回归检查）。

用法：python3 check_refs.py <assets/ic2_120 目录>
"""
import glob
import json
import os
import sys

A = sys.argv[1].rstrip('/') + '/'
bad = []


def exists(path, key):
    # model/parent 只能解析到模型；其余（textures 下的槽位）只能解析到贴图
    if key in ('model', 'parent'):
        return os.path.exists(A + f'models/{path}.json') or path.endswith('.obj') and os.path.exists(A + path)
    return os.path.exists(A + f'textures/{path}.png') or os.path.exists(A + path)


def walk(v, where, key=None):
    if isinstance(v, dict):
        for k, x in v.items():
            walk(x, where, k)
    elif isinstance(v, list):
        for x in v:
            walk(x, where, key)
    elif isinstance(v, str) and key == 'parent' and where.startswith('models/') and \
            v.split(':', 1)[-1] == where[len('models/'):-len('.json')]:
        bad.append((where, v + '（自引用）'))
    elif isinstance(v, str) and v.startswith('ic2_120:') and key not in ('loader',):
        if not exists(v.split(':', 1)[1], key):
            bad.append((where, v))


for p in glob.glob(A + 'blockstates/*.json') + glob.glob(A + 'models/**/*.json', recursive=True):
    walk(json.load(open(p, encoding='utf-8')), os.path.relpath(p, A))
print(f'无法解析的引用 {len(bad)} 个')
for b in bad:
    print(*b)
sys.exit(1 if bad else 0)
