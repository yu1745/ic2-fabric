#!/usr/bin/env python3
"""
扫描项目中使用 @ModBlock 和 @ModItem 注解的类，
按 group 分组后生成 [类名, zh_cn翻译] 列表并写入 docs/item-block-list.md
"""

import json
import re
from collections import defaultdict
from pathlib import Path

PROJECT_ROOT = Path(__file__).parent.parent
ZH_CN_PATH = PROJECT_ROOT / "src/main/resources/assets/ic2_120/lang/zh_cn.json"
KT_PATTERN = PROJECT_ROOT / "src"

# 加载 zh_cn 翻译
with open(ZH_CN_PATH, "r", encoding="utf-8") as f:
    zh_cn: dict = json.load(f)


def to_reg_name(class_name: str) -> str:
    """将类名转换为注册名（驼峰转下划线小写）"""
    s1 = re.sub("(.)([A-Z][a-z]+)", r"\1_\2", class_name)
    return re.sub("([a-z0-9])([A-Z])", r"\1_\2", s1).lower()


def parse_annotation_params(params_str: str) -> tuple[str | None, str]:
    """解析注解参数字符串，返回 (name, group)"""
    name_match = re.search(r'name\s*=\s*"([^"]*)"', params_str)
    group_match = re.search(r'group\s*=\s*"([^"]*)"', params_str)
    name = name_match.group(1) if name_match else None
    group = group_match.group(1) if group_match else ""
    return name, group


def extract_mod_annotations(content: str):
    """
    从 Kotlin 文件内容中提取 @ModBlock 和 @ModItem 注解及其 class 定义。
    返回 list of (type, class_name, reg_name, group, translation)
    """
    results = []
    # 移除块注释和行注释
    cleaned = re.sub(r"/\*.*?\*/", "", content, flags=re.DOTALL)
    cleaned = re.sub(r"//.*?$", "", cleaned, flags=re.MULTILINE)

    lines = cleaned.split("\n")
    i = 0
    while i < len(lines):
        line = lines[i]

        block_match = re.search(r"@ModBlock\s*\(([^)]*)\)", line)
        item_match = re.search(r"@ModItem\s*\(([^)]*)\)", line)

        if block_match or item_match:
            annot_type = "block" if block_match else "item"
            params_str = (block_match or item_match).group(1)
            name, group = parse_annotation_params(params_str)

            # 在后续行中寻找 class 定义
            for j in range(i, min(i + 15, len(lines))):
                class_match = re.search(r"^\s*class\s+(\w+)", lines[j])
                if class_match:
                    class_name = class_match.group(1)
                    if name is None:
                        name = to_reg_name(class_name)

                    key = f"{annot_type}.ic2_120.{name}"
                    translation = zh_cn.get(key, "（未找到翻译）")
                    results.append((annot_type, class_name, name, group, translation))
                    i = j
                    break
        i += 1

    return results


def main():
    all_results = []

    for kt_file in KT_PATTERN.rglob("*.kt"):
        try:
            content = kt_file.read_text(encoding="utf-8")
        except Exception:
            continue
        all_results.extend(extract_mod_annotations(content))

    # 按 type 分组，再按 group 分组
    blocks_by_group: dict = defaultdict(list)
    items_by_group: dict = defaultdict(list)

    for typ, cls, name, group, trans in all_results:
        if typ == "block":
            blocks_by_group[group].append((cls, name, trans))
        else:
            items_by_group[group].append((cls, name, trans))

    # 对每个 group 内部按翻译排序
    for g in blocks_by_group:
        blocks_by_group[g].sort(key=lambda x: x[2])
    for g in items_by_group:
        items_by_group[g].sort(key=lambda x: x[2])

    # group 排序：空字符串放最后
    def group_sort_key(k):
        return (k != "", k)

    block_groups = sorted(blocks_by_group.keys(), key=group_sort_key)
    item_groups = sorted(items_by_group.keys(), key=group_sort_key)

    total_blocks = sum(len(v) for v in blocks_by_group.values())
    total_items = sum(len(v) for v in items_by_group.values())

    lines = [
        "# IC2-120 物品与方块列表\n",
        "> 由脚本自动生成（扫描 @ModBlock / @ModItem 注解 + zh_cn.json）\n",
        f"- **方块总数**: {total_blocks}  |  **物品总数**: {total_items}",
        f"- **方块分组数**: {len(block_groups)}  |  **物品分组数**: {len(item_groups)}",
        "",
        "## 方块 (Block)\n",
    ]

    for group_key in block_groups:
        group_label = f"**分组: {group_key}**" if group_key else "**（无分组）**"
        lines.append(f"### {group_label}\n")
        lines.append("| 类名 | 注册名 | 中文翻译 |")
        lines.append("|------|--------|----------|")
        for cls, name, trans in blocks_by_group[group_key]:
            lines.append(f"| `{cls}` | `{name}` | {trans} |")
        lines.append("")

    lines.append("## 物品 (Item)\n")

    for group_key in item_groups:
        group_label = f"**分组: {group_key}**" if group_key else "**（无分组）**"
        lines.append(f"### {group_label}\n")
        lines.append("| 类名 | 注册名 | 中文翻译 |")
        lines.append("|------|--------|----------|")
        for cls, name, trans in items_by_group[group_key]:
            lines.append(f"| `{cls}` | `{name}` | {trans} |")
        lines.append("")

    output_path = PROJECT_ROOT / "docs/item-block-list.md"
    output_path.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"已生成: {output_path}")
    print(f"方块: {total_blocks} 个（{len(block_groups)} 分组）")
    print(f"物品: {total_items} 个（{len(item_groups)} 分组）")
    for g in block_groups:
        print(f"  Block group[{repr(g)}]: {len(blocks_by_group[g])} 个")
    for g in item_groups:
        print(f"  Item  group[{repr(g)}]: {len(items_by_group[g])} 个")


if __name__ == "__main__":
    main()
