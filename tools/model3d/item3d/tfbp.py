"""地形转换模板家族：参照 ic2 textures/item/tfbp/*（灰色卡匣，黑色侧框，顶部彩色识别块，底部两枚红色触针）。"""
from core import Model, item, mat

BODY = mat('#c0c0c0', 'metal')
FRAME = mat('#3a3a3a')   # 原图纯黑侧框，按规范提到 #3a3a3a
INSET = mat('#8d8d8d')
PIN = mat('#ff0000')


def cartridge(code):
    m = Model()
    m.box(4.5, 3.0, 6.6, 11.5, 13.0, 9.4, BODY)                      # 卡匣主体
    for x in (3.6, 11.5):                                             # 黑色侧框
        m.box(x, 2.6, 6.2, x + 0.9, 13.4, 9.8, FRAME)
    m.box(4.5, 13.0, 6.2, 11.5, 13.8, 9.8, FRAME)                    # 顶框
    m.box(6.0, 5.0, 9.4, 10.0, 10.6, 9.8, INSET)                     # 正面标签凹槽
    m.box(6.6, 13.8, 7.0, 9.4, 15.0, 9.0, code)                      # 顶部识别块
    for x in (5.4, 9.4):                                              # 底部红色触针
        m.box(x, 1.0, 7.4, x + 1.2, 3.0, 8.6, PIN)
    return m


@item('blank_tfbp')
def blank_tfbp():
    return cartridge(mat('#3a3a3a'))


@item('chilling_tfbp')
def chilling_tfbp():
    return cartridge(mat('#ffffff'))


@item('cultivation_tfbp')
def cultivation_tfbp():
    return cartridge(mat('#00a040'))


@item('desertification_tfbp')
def desertification_tfbp():
    return cartridge(mat('#aa8015'))


@item('flatification_tfbp')
def flatification_tfbp():
    return cartridge(mat('#b4b4b4'))


@item('irrigation_tfbp')
def irrigation_tfbp():
    return cartridge(mat('#0075b2'))


@item('mushroom_tfbp')
def mushroom_tfbp():
    return cartridge(mat('#6a2ab0'))
