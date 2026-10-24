"""把 ic2_120 仍引用的 ic2 模型/贴图一对一迁移到 ic2_120 命名空间。

从 ic2_120 的 blockstates 与 models 出发，递归收集引用到的 ic2 模型（parent、overrides、
blockstate 的 model），复制到 ic2_120 同路径并把其中的 ic2 引用改为 ic2_120；
这些模型用到的 ic2 贴图经 items.remaster_image 重制到 ic2_120 同路径。
原来多个模型共享的同一张贴图/父模型，迁移后仍共享同一份。

被 tintindex 染色的贴图用中性灰重制（冷钢偏蓝会和生物群系染色叠加变色）。
目标路径已存在则不覆盖（报告冲突），保证不会破坏 ic2_120 自有资产。

用法：python3 migrate.py <assets 目录> [--dry-run]
"""
import json
import re
import sys
from pathlib import Path

from PIL import Image

sys.path.insert(0, str(Path(__file__).parent))
import items  # noqa: E402

REF = re.compile(r'^ic2:(.+)$')
# 已改写为 ic2_120 但贴图文件缺失（重新生成时）同样需要从 ic2 重制
REF_ANY = re.compile(r'^ic2(?:_120)?:(.+)$')


def load(p):
    return json.loads(p.read_text(encoding='utf-8'))


def model_refs(d):
    out = []
    if isinstance(d.get('parent'), str):
        out.append(d['parent'])
    for o in d.get('overrides', []) or []:
        out.append(o.get('model'))
    return [r for r in out if r]


def texture_refs(d):
    return [v for v in (d.get('textures') or {}).values() if isinstance(v, str) and not v.startswith('#')]


def tinted_slots(d):
    slots = set()
    for e in d.get('elements', []) or []:
        for face in (e.get('faces') or {}).values():
            if 'tintindex' in face and face.get('texture', '').startswith('#'):
                slots.add(face['texture'][1:])
    return slots


def blockstate_models(d):
    out = []

    def walk(v):
        if isinstance(v, dict):
            if isinstance(v.get('model'), str):
                out.append(v['model'])
            for x in v.values():
                walk(x)
        elif isinstance(v, list):
            for x in v:
                walk(x)
    walk(d)
    return out


# 线缆保持原版 ic2 模型与贴图（用户要求，重制效果不理想）
SKIP = ('cable',)


def skipped(p):
    return any(k in p.as_posix() for k in SKIP)


def main(assets, dry):
    assets = Path(assets)
    src, dst = assets / 'ic2', assets / 'ic2_120'
    queue, models, textures, tinted, conflicts, missing = [], set(), set(), set(), [], []

    def want_model(ref):
        # ic2:… 或已被改写成 ic2_120:… 但模型文件缺失的引用，都需要从 ic2 复制
        m = REF_ANY.match(ref or '')
        if m and ref.startswith('ic2_120:') and (dst / 'models' / f'{m.group(1)}.json').exists():
            return
        if m and m.group(1) not in models:
            models.add(m.group(1))
            queue.append(m.group(1))

    for p in (dst / 'blockstates').glob('*.json'):
        if skipped(p):
            continue
        for r in blockstate_models(load(p)):
            want_model(r)
    for p in (dst / 'models').rglob('*.json'):
        if skipped(p):
            continue
        d = load(p)
        for r in model_refs(d):
            want_model(r)
        slots = tinted_slots(d)
        for k, t in (d.get('textures') or {}).items():
            m = REF_ANY.match(t) if isinstance(t, str) else None
            if m and (src / 'textures' / f'{m.group(1)}.png').exists():
                textures.add(m.group(1))
                if k in slots:
                    tinted.add(m.group(1))
    while queue:
        rel = queue.pop()
        sp = src / 'models' / f'{rel}.json'
        if not sp.exists():
            missing.append(f'model {rel}')
            continue
        d = load(sp)
        for r in model_refs(d):
            want_model(r)
        slots = tinted_slots(d)
        for k, t in (d.get('textures') or {}).items():
            m = REF.match(t) if isinstance(t, str) else None
            if m:
                textures.add(m.group(1))
                if k in slots:
                    tinted.add(m.group(1))

    print(f'模型 {len(models)}，贴图 {len(textures)}（其中染色 {len(tinted)}）')
    if dry:
        return
    # 1) 模型：复制并改引用
    for rel in sorted(models):
        sp, tp = src / 'models' / f'{rel}.json', dst / 'models' / f'{rel}.json'
        if not sp.exists():
            continue
        text = re.sub(r'"ic2:', '"ic2_120:', sp.read_text(encoding='utf-8'))
        if tp.exists():
            if tp.read_text(encoding='utf-8') != text:
                conflicts.append(f'model {rel}')
            continue
        tp.parent.mkdir(parents=True, exist_ok=True)
        tp.write_text(text, encoding='utf-8')
    # 2) 贴图：重制
    made = 0
    for rel in sorted(textures):
        sp, tp = src / 'textures' / f'{rel}.png', dst / 'textures' / f'{rel}.png'
        if not sp.exists():
            missing.append(f'texture {rel}')
            continue
        if tp.exists():
            continue
        try:
            img = items.remaster_image(Image.open(sp), rel, neutral=rel in tinted)
        except Exception as e:  # ic2 资源中个别文件损坏
            missing.append(f'texture {rel}（无法读取：{type(e).__name__}）')
            continue
        tp.parent.mkdir(parents=True, exist_ok=True)
        img.save(tp)
        meta = sp.with_suffix('.png.mcmeta')
        if meta.exists():
            tp.with_suffix('.png.mcmeta').write_text(meta.read_text())
        made += 1
    # 3) ic2_120 自身 JSON：引用改到 ic2_120（仅当目标已存在）
    changed = 0
    for p in list((dst / 'blockstates').glob('*.json')) + list((dst / 'models').rglob('*.json')):
        if skipped(p):
            continue
        s = p.read_text(encoding='utf-8')
        own = p.relative_to(dst / 'models').with_suffix('').as_posix() if 'models' in p.parts else None

        def sub(m):
            path = m.group(1)
            # ic2_120 已有同名包装模型（如 parent 指向 ic2 同路径）时，改写会变成自引用死循环
            if path == own:
                return m.group(0)
            ok = (dst / 'models' / f'{path}.json').exists() or (dst / 'textures' / f'{path}.png').exists()
            return f'"ic2_120:{path}"' if ok else m.group(0)
        t = re.sub(r'"ic2:([^"]+)"', sub, s)
        if t != s:
            p.write_text(t, encoding='utf-8')
            changed += 1
    print(f'重制贴图 {made}，改写 {changed} 个文件')
    for c in conflicts:
        print('冲突（未覆盖）', c)
    for m in missing:
        print('ic2 中不存在', m)


if __name__ == '__main__':
    main(sys.argv[1], '--dry-run' in sys.argv)
