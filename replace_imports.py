import re
import os
import sys

def replace_in_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # 1. 删除 import ic2_120.content.ModBlockEntities
    content = re.sub(r'import ic2_120\.content\.ModBlockEntities\n', '', content)
    
    # 2. 删除 import ic2_120.content.screen.ModScreenHandlers
    content = re.sub(r'import ic2_120\.content\.screen\.ModScreenHandlers\n', '', content)
    
    # 3. 添加 import ic2_120.registry.type (如果没有的话)
    if 'import ic2_120.registry.type' not in content and 'ic2_120.registry' in content:
        content = re.sub(r'(import ic2_120\.registry\.[^\n]+\n)', r'\1import ic2_120.registry.type\n', content)
    
    # 4. 替换 ModBlockEntities.getType(XBlockEntity::class) 为 XBlockEntity::class.type()
    content = re.sub(r'ModBlockEntities\.getType\((\w+BlockEntity)::class\)', r'\1::class.type()', content)
    
    # 5. 替换 ModScreenHandlers.getType(XScreenHandler::class) 为 XScreenHandler::class.type()
    content = re.sub(r'ModScreenHandlers\.getType\((\w+ScreenHandler)::class\)', r'\1::class.type()', content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated: {filepath}")
        return True
    return False

def main():
    root = "C:/Users/wangyu/Desktop/ic2_1.20/src"
    count = 0
    for dirpath, dirnames, filenames in os.walk(root):
        for filename in filenames:
            if filename.endswith('.kt'):
                filepath = os.path.join(dirpath, filename)
                if replace_in_file(filepath):
                    count += 1
    print(f"Total files updated: {count}")

if __name__ == "__main__":
    main()
