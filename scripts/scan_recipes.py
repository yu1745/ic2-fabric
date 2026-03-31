#!/usr/bin/env python3
"""扫描所有 ModItem 和 ModBlock，生成配方状态报告"""

import os
import re
from pathlib import Path

# 项目根目录
PROJECT_ROOT = Path("C:/Users/wangyu/Desktop/ic2-fabric")
SRC_ROOT = PROJECT_ROOT / "src" / "main" / "kotlin"

# 读取物品方块清单
def read_item_block_list():
    """读取物品方块清单文件"""
    list_file = PROJECT_ROOT / "docs" / "item-block-list.md"
    with open(list_file, 'r', encoding='utf-8') as f:
        content = f.read()

    # 解析清单，提取 (类名, 注册名, 中文翻译)
    items = []
    current_section = None
    current_group = None

    for line in content.split('\n'):
        line = line.strip()
        if not line or line.startswith('#'):
            if '方块 (Block)' in line:
                current_section = 'block'
            elif '物品 (Item)' in line:
                current_section = 'item'
            continue

        # 解析分组
        group_match = re.match(r'###\s*\*\*分组:\s*(.*?)\*\*', line)
        if group_match:
            current_group = group_match.group(1)
            continue

        # 解析物品行
        item_match = re.match(r'\|\s*`([^`]+)`\s*\|\s*`([^`]+)`\s*\|\s*([^|]+)\s*\|', line)
        if item_match:
            class_name = item_match.group(1).strip()
            reg_name = item_match.group(2).strip()
            cn_name = item_match.group(3).strip()
            items.append({
                'section': current_section,
                'group': current_group,
                'class_name': class_name,
                'reg_name': reg_name,
                'cn_name': cn_name
            })

    return items

# 构建有配方的类集合
def build_recipe_set():
    """构建有配方的类集合 - 检查 @RecipeProvider 注解"""
    recipe_set = set()

    # 扫描所有文件
    for kt_file in SRC_ROOT.rglob("*.kt"):
        try:
            with open(kt_file, 'r', encoding='utf-8') as f:
                content = f.read()

            # 查找所有类定义（记录类名和位置）
            class_pattern = r'\b(?:class|object|interface)\s+(\w+)\s*[:\(]'
            classes = []
            for match in re.finditer(class_pattern, content):
                classes.append({
                    'name': match.group(1),
                    'pos': match.start()
                })

            # 查找所有 @RecipeProvider 注解位置
            for match in re.finditer(r'@RecipeProvider\b', content):
                provider_pos = match.start()

                # 找到该注解之前最近的类定义（就是包含该注解的类）
                containing_class = None
                for cls in classes:
                    if cls['pos'] < provider_pos:
                        containing_class = cls
                    else:
                        break

                if containing_class:
                    recipe_set.add(containing_class['name'])

        except Exception as e:
            print(f"Error reading {kt_file}: {e}")
            continue

    return recipe_set

# 天然生成/不需要配方的物品/方块
NATURAL_ITEMS = {
    # 矿石
    'uranium_ore', 'lead_ore', 'tin_ore',
    'deepslate_uranium_ore', 'deepslate_lead_ore', 'deepslate_tin_ore',
    # 橡胶木系列（天然生成/可再生）
    'rubber_log', 'stripped_rubber_log', 'rubber_wood', 'stripped_rubber_wood',
    'rubber_leaves', 'rubber_sapling', 'rubber_planks', 'rubber_slab',
    'rubber_stairs', 'rubber_fence', 'rubber_fence_gate', 'rubber_door',
    'rubber_trapdoor', 'rubber_pressure_plate', 'rubber_button',
    # 基岩/玄武石等天然方块
    'basalt',
    # 建筑泡沫（通过泡沫喷枪生成）
    'foam', 'reinforced_foam', 'pellet',
    # 农业产品（通过作物系统获得）
    'crop_seed_bag', 'coffee_powder', 'coffee_beans', 'hops', 'terra_wart',
    # 咖啡饮品（通过咖啡机制作）
    'cold_coffee_mug', 'coffee_mug', 'empty_mug', 'dark_coffee_mug',
    # 压缩植物（通过压缩机压缩植物球获得）
    'compressed_plants',
    # 面粉（通过打粉机加工小麦获得）
    'flour',
    # 压缩煤球
    'coal_block',
    # 防爆石（通过建筑泡沫生成）
    'reinforced_stone',
    # 铁栅栏
    'iron_fence',
    # 粗矿块（@ModBlock 注解被注释，暂不注册）
    'raw_lead_block', 'raw_tin_block', 'raw_uranium_block',
    'raw_lead', 'raw_tin', 'raw_uranium',
    # 粉碎矿石（通过打粉机处理矿石获得）
    'crushed_copper', 'crushed_gold', 'crushed_iron', 'crushed_lead',
    'crushed_silver', 'crushed_tin', 'crushed_uranium',
    # 纯净粉碎矿石（通过洗矿机处理粉碎矿石获得）
    'purified_copper', 'purified_gold', 'purified_iron', 'purified_lead',
    'purified_silver', 'purified_tin', 'purified_uranium',
    # 金属板类（通过金属成型机压制获得）
    'carbon_plate', 'jetpack_attachment_plate', 'iridium',
    'gold_plate', 'steel_plate', 'iron_plate', 'lead_plate', 'copper_plate',
    'tin_plate', 'lapis_plate', 'bronze_plate', 'obsidian_plate',
    'dense_gold_plate', 'dense_steel_plate', 'dense_iron_plate', 'dense_lead_plate',
    'dense_copper_plate', 'dense_tin_plate', 'dense_lapis_plate', 'dense_bronze_plate',
    'dense_obsidian_plate',
    # 金属粉类（通过打粉机研磨获得）
    'coal_dust', 'stone_dust', 'sulfur_dust', 'clay_dust', 'energium_dust',
    'gold_dust', 'diamond_dust', 'iron_dust', 'lead_dust', 'copper_dust',
    'silver_dust', 'lithium_dust', 'tin_dust', 'lapis_dust', 'bronze_dust',
    'obsidian_dust', 'silicon_dioxide_dust', 'netherrack_dust', 'ender_pearl_dust',
    'hydrated_tin_dust', 'coal_fuel_dust',
    # 小撮金属粉类
    'small_sulfur_dust', 'small_gold_dust', 'small_iron_dust', 'small_lead_dust',
    'small_copper_dust', 'small_silver_dust', 'small_lithium_dust',
    'small_tin_dust', 'small_lapis_dust', 'small_bronze_dust',
    # 小撮稀有同位素粉类
    'small_uranium_235', 'small_uranium_238', 'small_plutonium',
    'small_obsidian_dust', 'small_tin_dust',
    # 核材料类
    'uranium_235', 'uranium_238', 'plutonium', 'mox',
    # 机器加工制品（外壳、导线等）
    'tin_casing', 'iron_casing', 'copper_casing', 'bronze_casing',
    'gold_casing', 'lead_casing', 'steel_casing',
    'tin_cable', 'copper_cable', 'gold_cable', 'iron_cable',
    'tin_can', 'fuel_rod', 'iron_shaft', 'steel_shaft', 'bronze_shaft', 'coin',
    # 加热元件（机器组件）
    'heatpack',
    # 传动轴（通过金属成型机挤压获得）
    'wood_transmission_shaft', 'carbon_transmission_shaft', 'steel_transmission_shaft', 'iron_transmission_shaft',
    # 中间产物
    'carbon_mesh', 'carbon_fibre', 'slag', 'bio_chaff', 'grin_powder',
    'rubber', 'empty_tin_can', 'iodine', 'alloy', 'refined_iron_ingot',
    # 废料（通过回收机回收物品获得）
    'scrap', 'scrap_box',
    # 铱相关（通过废料盒或其他方式获得）
    'iridium_ore_item', 'iridium_shard',
    'coal_ball', 'coal_chunk', 'mixed_metal_ingot', 'plant_ball',
    'resin', 'weed', 'hydrogen', 'methane',
    # 核燃料棒（所有变体）
    'uranium_fuel_rod', 'dual_uranium_fuel_rod', 'quad_uranium_fuel_rod',
    'mox_fuel_rod', 'dual_mox_fuel_rod', 'quad_mox_fuel_rod',
    'lithium_fuel_rod', 'rtg_pellet',
    'depleted_uranium_fuel_rod', 'depleted_dual_uranium_fuel_rod', 'depleted_quad_uranium_fuel_rod',
    'depleted_mox_fuel_rod', 'depleted_dual_mox_fuel_rod', 'depleted_quad_mox_fuel_rod',
    'depleted_isotope_fuel_rod',
    # 能量相关
    'energium_crystal', 'energy_crystal', 'lapotron_crystal',
    # 工具部件
    'tool_handle_bronze', 'diamond_saw_blade', 'industrial_diamond',
    # 其他加工产物
    'filled_tin_can',
    # 各种单元（通过流体装填机等机器加工获得，除空单元外）
    'uu_matter_cell', 'coolant_cell', 'hot_coolant_cell', 'bio_cell',
    'air_cell', 'lava_cell', 'water_cell', 'pahoehoe_lava_cell', 'biofuel_cell',
    'biomass_cell', 'distilled_water_cell', 'fluid_cell', 'weed_ex_cell',
    'reactor_coolant_cell', 'triple_reactor_coolant_cell', 'sextuple_reactor_coolant_cell',
    # 破损的橡胶船（使用损耗后变成）
    'broken_rubber_boat',
    # 建筑泡沫墙染色变体（通过建筑泡沫和染色获得）
    'white_wall', 'orange_wall', 'magenta_wall', 'light_blue_wall',
    'yellow_wall', 'lime_wall', 'pink_wall', 'gray_wall',
    'light_gray_wall', 'cyan_wall', 'purple_wall', 'blue_wall',
    'brown_wall', 'green_wall', 'red_wall', 'black_wall',
    # 调试物品
    'energy_debug_stick', 'debug_item',
    # 地形转换模板（不需要合成表，通过其他方式获取）
    'chilling_tfbp', 'flatification_tfbp', 'desertification_tfbp',
    'irrigation_tfbp', 'blank_tfbp', 'cultivation_tfbp', 'mushroom_tfbp',
    # 创造模式物品
    'creative_generator',
    # EU电表类（测量工具）
    'meter', 'wind_meter',
    # 工具箱（便携存储容器）
    'tool_box',
}

# 主函数
def main():
    print("正在读取物品方块清单...")
    items = read_item_block_list()

    print("正在扫描配方...")
    recipe_classes = build_recipe_set()

    print(f"找到 {len(items)} 个物品/方块")
    print(f"找到 {len(recipe_classes)} 个有配方的类")

    # 生成报告
    output = []
    output.append("# IC2-120 合成配方状态报告\n")
    output.append(f"> 自动生成于扫描所有 ModItem 和 ModBlock 的配方状态\n")
    output.append(f"- 总物品/方块数: {len(items)}\n")

    current_section = None
    current_group = None

    # 统计
    with_recipe = 0
    without_recipe = 0
    natural_items = 0

    for item in items:
        # 检查是否需要输出分组标题
        if item['section'] != current_section:
            current_section = item['section']
            section_name = "方块" if current_section == 'block' else "物品"
            output.append(f"\n## {section_name} ({current_section.capitalize()})\n")

        if item['group'] != current_group:
            current_group = item['group']
            group_display = item['group'] if item['group'] else "（无分组）"
            output.append(f"\n### **分组: {group_display}**\n")

        # 检查是否有配方
        has_recipe = item['class_name'] in recipe_classes
        is_natural = item['reg_name'] in NATURAL_ITEMS

        if is_natural:
            checkbox = "✅"
            status = "natural"
        elif has_recipe:
            checkbox = "☑️"
            status = "done"
        else:
            checkbox = "☐"
            status = "missing"

        if status == "done":
            with_recipe += 1
        elif status == "missing":
            without_recipe += 1
        elif status == "natural":
            natural_items += 1

        output.append(f"- {checkbox} `{item['class_name']}` (`{item['reg_name']}`) - {item['cn_name']}")

    # 输出统计
    output.append("\n---\n")
    output.append("## 统计摘要\n")
    output.append(f"- 已有配方: {with_recipe}")
    output.append(f"- 天然生成/不需要配方: {natural_items}")
    output.append(f"- 待实现配方: {without_recipe}")
    total_need_recipe = with_recipe + without_recipe
    if total_need_recipe > 0:
        output.append(f"- 完成度: {with_recipe / total_need_recipe * 100:.1f}%")

    # 写入文件
    output_file = PROJECT_ROOT / "docs" / "recipe-status.md"
    with open(output_file, 'w', encoding='utf-8') as f:
        f.write('\n'.join(output))

    print(f"\n报告已生成: {output_file}")
    if total_need_recipe > 0:
        print(f"完成度: {with_recipe / total_need_recipe * 100:.1f}%")

if __name__ == "__main__":
    main()
