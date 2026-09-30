# File Organizer with "Black Hole" Cleanup (Spring Boot)

A command-line file organizer built with **Spring Boot 3 / Java 17**. It sorts files into subfolders by type and,
optionally, sweeps away files you haven't touched in over a year into an **AES-256 encrypted `Deep_Archive.zip`**.

Task 1 of the Arithmatrix Virtual Internship Program (AVIP) 2026, Software Developer track.

## Features

- Sorts files into `Images`, `Documents`, `Archives`, `Audio`, `Video`, `Code` and `Others`
  (extensions are case-insensitive; unknown extensions go to `Others`)
- **Never overwrites.** Name clashes are resolved deterministically: `photo.jpg`, `photo_1.jpg`, `photo_2.jpg`, ...
  (existing files and earlier planned moves are both accounted for)
- Configurable **source** and **target** base directory via CLI arguments
- **Dry-run mode** lists every planned move without touching anything
- **Black Hole deep archive** (opt-in): files unused for more than N days (default 365) are stored in an
  AES-256 encrypted `Deep_Archive.zip`. Each original is deleted only after its archive entry is verified.
  Honours dry-run.
- Console log plus `organizer.log` file for a sample run log
- Unit tests for categories, conflict handling, dry-run and archiving

## Requirements

- Java 17+
- Maven 3.9+

## Build

```bash
mvn clean package
```

## Usage

```bash
java -jar target/file-organizer-1.0.0.jar --organizer.source=<dir> [options]
```

| Option                              | Description                                                        |
|-------------------------------------|--------------------------------------------------------------------|
| `--organizer.source=<dir>`          | Directory to organize (required)                                   |
| `--organizer.target=<dir>`          | Base directory for the category folders (default: same as source)  |
| `--organizer.dry-run=true`          | Only list planned moves                                            |
| `--organizer.deep-archive=true`     | Enable the Black Hole cleanup                                      |
| `--organizer.archive-after-days=N`  | Age threshold in days (default `365`)                              |
| `--organizer.archive-password=<pw>` | Archive password (or set the `ARCHIVE_PASSWORD` env variable)      |

Only files directly inside the source folder are processed (subfolders are left alone).

### Examples

```bash
# 1. Preview what would happen (safe)
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads --organizer.dry-run=true

# 2. Organize in place
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads

# 3. Organize into a different base folder
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads --organizer.target=/home/me/Sorted

# 4. Include the Black Hole cleanup (preview first!)
export ARCHIVE_PASSWORD='choose-a-strong-password'
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads \
     --organizer.deep-archive=true --organizer.dry-run=true
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads --organizer.deep-archive=true

# 5. Custom threshold: archive anything untouched for 180 days
java -jar target/file-organizer-1.0.0.jar --organizer.source=/home/me/Downloads \
     --organizer.deep-archive=true --organizer.archive-after-days=180
```

Prefer the environment variable for the password: values passed with `--organizer.archive-password` can end up in
your shell history.

## Example output (format)

```
09:15:02 INFO  Source : /home/me/Downloads
09:15:02 INFO  Target : /home/me/Downloads
09:15:02 INFO  Mode   : DRY-RUN (no changes will be made)
09:15:02 INFO  [DRY-RUN] Would move holiday.jpg -> Images/holiday.jpg
09:15:02 INFO  [DRY-RUN] Would move holiday(copy).jpg -> Images/holiday(copy).jpg
09:15:02 INFO  [DRY-RUN] Would move notes.pdf -> Documents/notes.pdf
09:15:02 INFO  [DRY-RUN] Would move song.mp3 -> Audio/song.mp3
09:15:02 INFO  Done. 4 file(s) would be organized.
```

See `docs/` for a real sample run log and before/after screenshots.

## How the Black Hole cleanup works

1. Files in the source folder are checked using the later of their last-access and last-modified time.
   (Some systems, especially Windows, don't update last-access time reliably, so modified time is a safety net.)
2. Files older than the threshold are excluded from sorting and added to `Deep_Archive.zip` (AES-256).
   If the archive already exists, new files are appended; duplicate names inside it are renamed the same way.
3. Each file is verified inside the archive (entry exists, size matches) before the original is deleted.
   If verification fails, the original is kept.

To read the archive later, use any tool that supports AES-256 zip (7-Zip, WinRAR, Keka) with your password.

## Project structure

```
src/main/java/dev/rafay/fileorganizer/
    FileOrganizerApplication.java  entry point
    OrganizerProperties.java       CLI/config binding
    OrganizerRunner.java           argument validation + orchestration
    FileOrganizerService.java      planning, conflict resolution, moving
    DeepArchiveService.java        stale-file detection + encrypted archive
    FileCategory.java              extension -> folder mapping
src/test/java/dev/rafay/fileorganizer/
    FileCategoryTest, FileOrganizerServiceTest, DeepArchiveServiceTest
```

## Run the tests

```bash
mvn test
```
