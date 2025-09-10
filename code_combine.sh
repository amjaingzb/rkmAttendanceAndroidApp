#!/bin/bash

# The name for the final combined file
output_file="delme-combined-code-only.txt"
# New log file for files that were skipped
skipped_log_file="delme-skipped-files.log"

# Create or clear the output files before starting
> "$output_file"
> "$skipped_log_file"

# 1. Define files and directories to explicitly ignore.
ignore_dirs=("./build" "./.gradle" "./.idea")
ignore_patterns="-name $output_file -o -name $skipped_log_file"

# Add standard directories to the find command's exclusion list
for dir in "${ignore_dirs[@]}"; do
    ignore_patterns+=" -o -path $dir/*"
done

# 2. Read .gitignore and add its patterns to the ignore list
if [ -f ".gitignore" ]; then
    gitignore_patterns=$(awk '!/^#|^$/ {
        pattern = $0
        if (substr(pattern, length(pattern)) == "/") {
            sub(/\/$/, "", pattern);
            printf "-o -path \"./%s/*\" ", pattern
        } else {
            printf "-o -name \"%s\" ", pattern
        }
    }' .gitignore)
    ignore_patterns+=" $gitignore_patterns"
fi

# 3. Define the file types we WANT to include for code and build logic.
# This is the comprehensive list.
include_patterns="\
    -name \"*.java\" \
-o -name \"*.kt\" \
-o -name \"*.xml\" \
-o -name \"AndroidManifest.xml\" \
-o -name \"*.gradle\" \
-o -name \"*.gradle.kts\" \
-o -name \"gradle.properties\" \
-o -name \"settings.gradle\" \
-o -name \"*.pro\" \
-o -name \"*.c\" \
-o -name \"*.cpp\" \
-o -name \"*.h\" \
-o -name \"*.glsl\""

# 4. Construct and run the find command with logging for skipped files.
# The logic is: prune ignored paths, then for everything else, either print it for inclusion
# OR print it to the skipped log file.
eval find . -type f \
    \( $ignore_patterns \) -prune \
-o \( \
    \( $include_patterns \) -print0 \
    -o -fprintf \"$skipped_log_file\" \"%p\\n\" \
\) | while IFS= read -r -d $'\0' file; do
    
    # 5. Check if the file (from the include list) is text-based before appending it.
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