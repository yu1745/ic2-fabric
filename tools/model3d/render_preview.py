"""Blender 无头预览：按 Minecraft 固定面亮度渲染导出的 OBJ。

blender -b -P render_preview.py -- <assets/ic2_120 目录> <输出目录> <模型名>...
复用 ~/Motive 的 motive_bl 着色辅助（只读引用）。
"""
import sys, os
sys.path.insert(0, os.path.expanduser('~/Motive/dev-assets/recovered/compressor-scripts'))
import motive_bl as mb

args = sys.argv[sys.argv.index('--') + 1:]
assets, out, names = args[0], args[1], args[2:]
mdir = os.path.join(assets, 'models/block/machine3d')
tdir = os.path.join(assets, 'textures/block/machine3d')
os.makedirs(out, exist_ok=True)
views = {
    'front': (0.62, 0.55, -0.62),   # 正面（北）+ 东侧
    'back': (-0.62, 0.55, 0.62),    # 背面（南）+ 西侧
}
for name in names:
    for view, d in views.items():
        mb.reset_scene()
        mb._image_cache.clear()
        mb.import_repo_obj(os.path.join(mdir, name + '.obj'), tdir, name, offset=(-0.5, 0, -0.5))
        cam = mb.add_ortho_camera('cam', d, target_mc=(0, 0.5, 0), scale=1.75)
        mb.render(cam, os.path.join(out, f'{name}_{view}.png'), res=768)
