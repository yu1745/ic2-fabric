"""导出全部（或指定）3D 物品。

用法：python3 tools/model3d/item3d/build.py <assets 目录> [物品名...]
各家族模块放在本目录（*.py，除 core/build/preview），导入即注册。
"""
import importlib
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
import core  # noqa: E402

for p in sorted(HERE.glob('*.py')):
    if p.stem not in ('core', 'build', 'preview'):
        importlib.import_module(p.stem)

if __name__ == '__main__':
    n = core.export(sys.argv[1], sys.argv[2:] or None)
    print(f'3D 物品 {n} 个')
