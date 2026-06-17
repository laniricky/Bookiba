import os

# Directories to search
dirs = [
    r"c:\DEV\Bookiba\feature",
    r"c:\DEV\Bookiba\app\src\main\java\co\booknook\app",
    r"c:\DEV\Bookiba\core\designsystem\src\main\java\co\booknook\core\designsystem\components"
]

# Replacements mapping
replacements = {
    "SoftWhite": "androidx.compose.material3.MaterialTheme.colorScheme.background",
    "Cream": "androidx.compose.material3.MaterialTheme.colorScheme.surface",
    "DarkBrown": "androidx.compose.material3.MaterialTheme.colorScheme.onBackground",
    "WarmBrown": "androidx.compose.material3.MaterialTheme.colorScheme.onSurface",
    "DeepCharcoal": "androidx.compose.material3.MaterialTheme.colorScheme.surface"
}

def process_file(filepath):
    # Exclude Color.kt and Theme.kt just in case, though they aren't in these dirs
    if filepath.endswith("Color.kt") or filepath.endswith("Theme.kt"):
        return
        
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    original_content = content

    for old_val, new_val in replacements.items():
        import re
        # Only replace exact word matches, and skip lines starting with import
        new_lines = []
        for line in content.split('\n'):
            if line.strip().startswith('import '):
                new_lines.append(line)
            else:
                new_lines.append(re.sub(r'\b' + old_val + r'\b', new_val, line))
        content = '\n'.join(new_lines)

    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Updated {filepath}")

for d in dirs:
    for root, _, files in os.walk(d):
        for file in files:
            if file.endswith(".kt"):
                process_file(os.path.join(root, file))

print("Done.")
