from PIL import Image
import os

NANO_SABER_DIR = "core/src/main/resources/assets/ic2/textures/item/tool/electric/nano_saber"
OUT_DIR = "more-weapons-addon/src/main/resources/assets/ic2_120_more_weapons_addon/textures/item/quantum_saber"

os.makedirs(OUT_DIR, exist_ok=True)

files = ["inactive.png"] + [f"active_{i}.png" for i in range(1, 11)]

for f in files:
    src = os.path.join(NANO_SABER_DIR, f)
    dst = os.path.join(OUT_DIR, f)
    img = Image.open(src).convert("RGBA")
    r, g, b, a = img.split()
    r = r.point(lambda p: 255 - p)
    g = g.point(lambda p: 255 - p)
    b = b.point(lambda p: 255 - p)
    inverted = Image.merge("RGBA", (r, g, b, a))
    inverted.save(dst)
    print(f"Inverted: {f}")

print("Done! All textures inverted.")
