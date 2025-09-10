#!/bin/bash

# The name for the final combined file
output_file="delme-combined-code-only.txt"
# New log file for files that were skipped
skipped_log_file="delme-skipped-files.log"

# Create or clear the output files before starting
> "$output_file"
> "$skipped_log_file"

# --- Argument arrays for the find command ---
# This is safer than building a single string for eval.
declare -a ignore_args
declare -a include_args

# --- Populate Ignore Arguments ---
# 1. Add our own output files to the ignore list first.
# The first pattern doesn't need a preceding "-o".
ignore_args+=('-name' "$output_file")
ignore_args+=('-o' '-name' "$skipped_log_file")

# 2. Add standard directories to ignore.
ignore_dirs=("./build" "./.gradle" "./.idea")
for dir in "${ignore_dirs[@]}"; do
    ignore_args+=('-o' '-path' "$dir/*")
done

# 3. Read .gitignore and add its patterns to the ignore list.
if [ -f ".gitignore" ]; then
    # Use grep to filter out comments and blank lines, then read each pattern.
    while IFS= read -r pattern; do
        # Trim leading/trailing whitespace
        pattern=$(echo "$pattern" | xargs)
        if [ -z "$pattern" ]; then continue; fi

        # If gitignore pattern ends with a slash, treat it as a directory path.
        if [[ "$pattern" == */ ]]; then
            pattern="${pattern%/}" # remove trailing slash
            ignore_args+=('-o' '-path' "./$pattern/*")
        else
            # Otherwise, treat it as a file name pattern.
            ignore_args+=('-o' '-name' "$pattern")
        fi
    done < <(grep -vE '^\s*#|^\s*$' .gitignore)
fi

# --- Populate Include Arguments ---
# This is the comprehensive list of file types we want to include.
include_args=(
    '-name' '*.java'
    '-o' '-name' '*.kt'
    '-o' '-name' '*.xml'
    '-o' '-name' 'AndroidManifest.xml'
    '-o' '-name' '*.gradle'
    '-o' '-name' '*.gradle.kts'
    '-o' '-name' 'gradle.properties'
    '-o' '-name' 'settings.gradle'
    '-o' '-name' '*.pro'
    '-o' '-name' '*.c'
    '-o' '-name' '*.cpp'
    '-o' '-name' '*.h'
    '-o' '-name' '*.glsl'
)

# --- Execute the find Command ---
# Construct the full command using the arrays.
# This is safe and robust, avoiding 'eval'.
find . -type f \
    '(' "${ignore_args[@]}" ')' -prune \
-o \
    '(' \
        '(' "${include_args[@]}" ')' -print0 \
        -o -fprintf "$skipped_log_file" "%p\n" \
    ')' \
| while IFS= read -r -d $'\0' file; do
    
    # Check if the file (from the include list) is text-based before appending it.
    if [[ "$(file -b --mime-type "$file")" == text/* ]]; then
        # Print a header with the path to the file
        echo "==================== FILE: $file ====================" >> "$output_file"
        
        # Append the content of the file
        cat "$file" >> "$output_file"
        
        # Add a newline for better separation between files
        echo "" >> "$output_file"
    else
        # This file matched our include patterns but was detected as binary.
        # Log this specific case for clarity.
        echo "INFO: Skipped binary file from include list: $file" >> "$skipped_log_file"
    fi
done

echo "All relevant Android project code files have been combined into $output_file"
echo "Files that were encountered but not included have been logged to $skipped_log_file"