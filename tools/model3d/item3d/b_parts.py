"""子代理 B 的共享部件函数（不注册任何物品）。

只提供几何部件：圆顶堆、近似球体、碎块簇、锭、板、外壳槽、八棱柱轴等；
整件模型由 crafting / resource / dust / misc_items 中的逐物品函数组合。
"""
import math

from core import mat  # noqa: F401  供调用方统一导入

PH = math.pi / 8


def clamp(v):
    return max(0.0, min(16.0, v))


def disc_y(m, cx, cz, y0, y1, rad, tex, sides=8):
    """竖直八棱柱（平面朝坐标轴）。"""
    m.prism('y', y0, y1, (cx, cz), rad, sides, tex, PH if sides == 8 else 0.0)


def disc_z(m, cx, cy, z0, z1, rad, tex, sides=8):
    """朝南的八棱柱圆盘（硬币、圆片、正面圆形部件）。"""
    m.prism('z', z0, z1, (cx, cy), rad, sides, tex, PH if sides == 8 else 0.0)


def rod_x(m, cy, cz, x0, x1, rad, tex, sides=8):
    m.prism('x', x0, x1, (cy, cz), rad, sides, tex, PH if sides == 8 else 0.0)


def mound(m, cx, cz, y0, height, rad, tex, layers=5, power=1.6):
    """圆顶堆（粉末堆）：自下而上的八棱柱层，半径按 (1-t)^(1/power) 收口。
    tex 可为单材质或按层的列表（自下而上）。返回每层 (y0, y1, r) 以便贴颗粒。"""
    out = []
    h = height / layers
    for i in range(layers):
        t = i / layers
        rr = rad * (1 - t ** power) ** 0.5
        rr = max(rr, rad * 0.18)
        t_ = tex[i] if isinstance(tex, (list, tuple)) else tex
        disc_y(m, cx, cz, y0 + i * h, y0 + (i + 1) * h, rr, t_)
        out.append((y0 + i * h, y0 + (i + 1) * h, rr))
    return out


def sphere(m, cx, cy, cz, rad, tex, layers=5, axis='y'):
    """近似球体：沿 axis 堆叠的八棱柱，半径取圆截面。tex 可为单材质或列表（沿轴自小到大）。"""
    h = 2 * rad / layers
    for i in range(layers):
        a0 = -rad + i * h
        mid = a0 + h / 2
        rr = math.sqrt(max(rad * rad - mid * mid, 0)) * 1.04
        rr = max(rr, rad * 0.3)
        t_ = tex[i] if isinstance(tex, (list, tuple)) else tex
        if axis == 'y':
            m.prism('y', cy + a0, cy + a0 + h, (cx, cz), rr, 8, t_, PH)
        elif axis == 'z':
            m.prism('z', cz + a0, cz + a0 + h, (cx, cy), rr, 8, t_, PH)
        else:
            m.prism('x', cx + a0, cx + a0 + h, (cy, cz), rr, 8, t_, PH)


def chunk(m, x, y, z, sx, sy, sz, tex, top=None):
    """单个碎块：主体方块 + 顶面略小的倒角层（顶面另一材质时更显立体）。"""
    m.box(clamp(x), clamp(y), clamp(z), clamp(x + sx), clamp(y + sy), clamp(z + sz), tex)
    if top:
        ins = min(sx, sz) * 0.2
        m.box(clamp(x + ins), clamp(y + sy), clamp(z + ins), clamp(x + sx - ins), clamp(y + sy + 0.5), clamp(z + sz - ins), top)


def ingot(m, body, top, edge, x0=1.5, x1=14.5, z0=4.0, z1=12.0, y0=4.5, h=6.5):
    """锭：逐级收口的梯形块（底边 edge 暗色、两级主体 body、顶面 top 高光），平放、长轴沿 x。
    返回顶面 (x0, y, z0, x1, z1)。"""
    m.box(x0, y0, z0, x1, y0 + 1.0, z1, edge)                                         # 底边
    m.box(x0 + 0.5, y0 + 1.0, z0 + 0.5, x1 - 0.5, y0 + 3.5, z1 - 0.5, body)            # 下段
    m.box(x0 + 1.25, y0 + 3.5, z0 + 1.25, x1 - 1.25, y0 + h - 1.0, z1 - 1.25, body)    # 上段收口
    m.box(x0 + 2.0, y0 + h - 1.0, z0 + 2.0, x1 - 2.0, y0 + h, z1 - 2.0, top)           # 顶面
    return (x0 + 2.0, y0 + h, z0 + 2.0, x1 - 2.0, z1 - 2.0)


def plate(m, face, rim, back, x0=3.0, y0=3.0, x1=13.0, y1=13.0, z0=7.0, t=1.5):
    """普通板：立放朝南的薄板，周边 rim 斜边（略缩）、正面 face 凸起一层。"""
    m.box(x0, y0, z0, x1, y1, z0 + t, {'*': rim, 'n': back})
    m.box(x0 + 0.75, y0 + 0.75, z0 + t, x1 - 0.75, y1 - 0.75, z0 + t + 0.5, face)
    return z0 + t + 0.5


def dense_plate(m, face, mid, rim, x0=2.5, y0=2.5, x1=13.5, y1=13.5, z0=6.0):
    """致密板：三层叠压（底层最大、颜色最暗），总厚约 3.5，比普通板明显厚重。"""
    m.box(x0, y0, z0, x1, y1, z0 + 1.25, rim)
    m.box(x0 + 0.5, y0 + 0.5, z0 + 1.25, x1 - 0.5, y1 - 0.5, z0 + 2.5, mid)
    m.box(x0 + 1.0, y0 + 1.0, z0 + 2.5, x1 - 1.0, y1 - 1.0, z0 + 3.5, face)
    return z0 + 3.5


def casing(m, light, mid, dark, x0=3.0, x1=13.0, y0=2.0, y1=14.0, z0=6.5):
    """外壳：瓦楞状金属薄壳——背板两侧向后折边，正面四道竖向凸棱（亮色）+ 两道横向压槽（暗色）。"""
    t = 1.5
    m.box(x0, y0, z0, x1, y1, z0 + t, mid)                                     # 壳板
    m.box(x0 - 0.75, y0, z0 - 2.0, x0, y1, z0 + t, {'*': mid, 's': light})       # 左折边
    m.box(x1, y0, z0 - 2.0, x1 + 0.75, y1, z0 + t, {'*': mid, 's': light})       # 右折边
    for i in range(4):                                                         # 竖向凸棱
        xa = x0 + 0.75 + i * 2.4
        m.box(xa, y0 + 0.5, z0 + t, xa + 1.5, y1 - 0.5, z0 + t + 0.75, light)
    for yy in (y0 + 3.6, y1 - 4.4):                                            # 横向压槽
        m.box(x0, yy, z0 + t, x1, yy + 0.8, z0 + t + 0.9, dark)
