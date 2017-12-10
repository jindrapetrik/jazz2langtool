# jazz2langtool
Jazz Jackrabbit 2 language files tool

This tool can export Jazz Jackrabbit 2 language files (J2S) into human-readable and editable text file.
After editing, it is possible to assemble file back.

## Usage:
Exporting J2S file:
```
java -jar jazz2langtool.jar export english.j2s english.txt
```
Importing text file back to J2S file:
```
java -jar jazz2langtool.jar import modified.txt english.j2s
```

## Requirements:
You need Java 8 or later installed.

## Information about J2S file format:
[Jazz 2 J2S File specification](J2S_specification.txt), [Jazz 2 J2S Character encoding](J2S_character_encoding.txt)
