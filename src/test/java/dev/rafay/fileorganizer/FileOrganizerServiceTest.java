package dev.rafay.fileorganizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileOrganizerServiceTest {

    private final FileOrganizerService service = new FileOrganizerService();

    @Test
    void movesFilesIntoCategoryFolders(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.jpg"), "img");
        Files.writeString(dir.resolve("b.pdf"), "doc");

        service.execute(service.plan(service.listFiles(dir), dir), false);

        assertTrue(Files.exists(dir.resolve("Images/a.jpg")));
        assertTrue(Files.exists(dir.resolve("Documents/b.pdf")));
    }

    @Test
    void neverOverwritesExistingFiles(@TempDir Path dir) throws IOException {
        Files.createDirectories(dir.resolve("Images"));
        Files.writeString(dir.resolve("Images/a.jpg"), "original");
        Files.writeString(dir.resolve("a.jpg"), "new");

        service.execute(service.plan(service.listFiles(dir), dir), false);

        assertEquals("original", Files.readString(dir.resolve("Images/a.jpg")));
        assertEquals("new", Files.readString(dir.resolve("Images/a_1.jpg")));
    }

    @Test
    void plannedConflictsAreResolvedDeterministically(@TempDir Path dir) throws IOException {
        Path src = Files.createDirectories(dir.resolve("src"));
        Path a = Files.writeString(src.resolve("a.jpg"), "1");
        Path b = Files.writeString(Files.createDirectories(dir.resolve("src2")).resolve("a.jpg"), "2");

        List<FileOrganizerService.Move> moves = service.plan(List.of(a, b), dir);

        assertEquals(dir.resolve("Images/a.jpg"), moves.get(0).to());
        assertEquals(dir.resolve("Images/a_1.jpg"), moves.get(1).to());
    }

    @Test
    void dryRunChangesNothing(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.jpg"), "img");

        service.execute(service.plan(service.listFiles(dir), dir), true);

        assertTrue(Files.exists(dir.resolve("a.jpg")));
        assertTrue(Files.notExists(dir.resolve("Images")));
    }
}
