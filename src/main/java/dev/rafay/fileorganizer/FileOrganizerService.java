package dev.rafay.fileorganizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** Plans and performs the sorting of files into category subfolders. */
@Service
public class FileOrganizerService {

    private static final Logger log = LoggerFactory.getLogger(FileOrganizerService.class);

    /** A planned move of one file. */
    public record Move(Path from, Path to) {
    }

    /** Lists regular files directly inside {@code source} (not subfolders), sorted by name. */
    public List<Path> listFiles(Path source) throws IOException {
        try (Stream<Path> stream = Files.list(source)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(p -> !p.getFileName().toString().equals(DeepArchiveService.ARCHIVE_NAME))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();
        }
    }

    /**
     * Builds the list of moves. Name conflicts are resolved deterministically:
     * {@code photo.jpg} becomes {@code photo_1.jpg}, {@code photo_2.jpg}, and so on.
     * Files already on disk and earlier planned moves are both taken into account,
     * so nothing is ever overwritten (and dry-run output matches a real run).
     */
    public List<Move> plan(List<Path> files, Path target) {
        Set<Path> reserved = new HashSet<>();
        List<Move> moves = new ArrayList<>();
        for (Path file : files) {
            String name = file.getFileName().toString();
            Path folder = target.resolve(FileCategory.fromFileName(name).folderName());
            Path destination = resolveConflict(folder, name, reserved);
            reserved.add(destination);
            moves.add(new Move(file, destination));
        }
        return moves;
    }

    Path resolveConflict(Path folder, String fileName, Set<Path> reserved) {
        Path candidate = folder.resolve(fileName);
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        int counter = 1;
        while (Files.exists(candidate) || reserved.contains(candidate)) {
            candidate = folder.resolve(base + "_" + counter + ext);
            counter++;
        }
        return candidate;
    }

    /** Executes (or, in dry-run mode, only logs) the planned moves. Returns the number handled. */
    public int execute(List<Move> moves, boolean dryRun) {
        int handled = 0;
        for (Move move : moves) {
            String display = move.to().getParent().getFileName() + "/" + move.to().getFileName();
            if (dryRun) {
                log.info("[DRY-RUN] Would move {} -> {}", move.from().getFileName(), display);
                handled++;
                continue;
            }
            try {
                Files.createDirectories(move.to().getParent());
                // No REPLACE_EXISTING: the move fails rather than overwrite anything.
                Files.move(move.from(), move.to());
                log.info("Moved {} -> {}", move.from().getFileName(), display);
                handled++;
            } catch (IOException e) {
                log.error("Failed to move {}: {}", move.from().getFileName(), e.getMessage());
            }
        }
        return handled;
    }
}
