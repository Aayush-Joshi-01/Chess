# Build and Run Guide

## Prerequisites

- **Java 17 or later** (for record types and pattern matching)
  - Download: https://adoptium.net/
  - Verify: `java --version`
- **Google Guava 18.0** — already included at `Lib/guava-18.0.jar`

---

## Option A — IntelliJ IDEA (Recommended)

1. Open IntelliJ IDEA → **File → Open** → select the `D:\Chess` folder
2. Go to **File → Project Structure → Libraries** → click `+` → **Java** → select `Lib/guava-18.0.jar`
3. Go to **File → Project Structure → Modules → Sources** → mark `src` as Sources Root
4. Set **Project SDK** to Java 17+
5. Run `com.chess.engine.Chessv2` (right-click → Run)

### Optional: Add Piece Art
Place 12 PNG files (60×60px or larger) in `src/com/chess/gui/art/`:
```
white_king.png   white_queen.png   white_rook.png
white_bishop.png white_knight.png  white_pawn.png
black_king.png   black_queen.png   black_rook.png
black_bishop.png black_knight.png  black_pawn.png
```
The game runs without them (falls back to Unicode symbols).

---

## Option B — Command Line (javac + jar)

### Step 1: Compile
```powershell
# From D:\Chess
$sources = Get-ChildItem -Recurse -Filter "*.java" -Path "src" | ForEach-Object { $_.FullName }
New-Item -ItemType Directory -Force -Path "out"
javac -cp "Lib\guava-18.0.jar" -d out $sources
```

### Step 2: Copy Resources
```powershell
# Copy piece art (if you have it)
$artDest = "out\com\chess\gui\art"
New-Item -ItemType Directory -Force -Path $artDest
Copy-Item "src\com\chess\gui\art\*.png" -Destination $artDest
```

### Step 3: Create Runnable JAR
```powershell
# Create manifest
Set-Content -Path "out\MANIFEST.MF" -Value @"
Main-Class: com.chess.engine.Chessv2
Class-Path: guava-18.0.jar
"@

# Package
Copy-Item "Lib\guava-18.0.jar" "out\"
cd out
jar cfm ..\Chess.jar MANIFEST.MF .
cd ..
```

### Step 4: Run
```powershell
# From D:\Chess
java -cp "Chess.jar;Lib\guava-18.0.jar" com.chess.engine.Chessv2
```

Or if you embedded Guava in the JAR (via a fat-jar approach):
```powershell
java -jar Chess.jar
```

---

## Fat JAR (Single Executable)

To create a self-contained JAR:

```powershell
cd "D:\Chess"
New-Item -ItemType Directory -Force -Path "fat"

# Extract Guava
cd fat
jar xf "..\Lib\guava-18.0.jar"
cd ..

# Compile into fat/
$sources = Get-ChildItem -Recurse -Filter "*.java" -Path "src" | ForEach-Object { $_.FullName }
javac -cp "Lib\guava-18.0.jar" -d fat $sources

# Copy art
New-Item -ItemType Directory -Force -Path "fat\com\chess\gui\art"
Copy-Item "src\com\chess\gui\art\*.png" "fat\com\chess\gui\art\" -ErrorAction SilentlyContinue

# Create manifest
Set-Content "fat\MANIFEST.MF" "Main-Class: com.chess.engine.Chessv2`n"

# Package
cd fat
jar cfm ..\Chess-fat.jar MANIFEST.MF .
cd ..

# Run
java -jar Chess-fat.jar
```

---

## Project Structure

```
D:\Chess\
├── Lib\
│   └── guava-18.0.jar          — Google Guava dependency
├── src\com\chess\
│   ├── engine\                  — Core chess engine
│   │   ├── board\               — Board, Tile, Move, BoardUtils
│   │   ├── pieces\              — All 6 piece types
│   │   └── player\              — Players, AI engines
│   │       └── ai\              — 6 AI strategies + infrastructure
│   └── gui\                     — Swing GUI
│       └── art\                 — PNG piece images (optional)
├── out\                         — Compiled .class files (after build)
├── README.md                    — Full project documentation
└── BUILD.md                     — This file
```

---

## Troubleshooting

| Problem | Solution |
|---------|----------|
| `UnsupportedClassVersionError` | Upgrade to Java 17+ |
| Pieces show as boxes (□) | Install a font with Unicode chess symbols, or add PNG art |
| AI is slow | Reduce difficulty or wait — Level 6 uses up to 3s per move |
| `NoClassDefFoundError: com/google/common/...` | Guava is not on the classpath — add `-cp Lib\guava-18.0.jar` |
