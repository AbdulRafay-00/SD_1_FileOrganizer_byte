package dev.rafay.fileorganizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributeView;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.lingala.zip4j.ZipFile;

class DeepArchiveServiceTest {

    private final DeepArchiveService service = new DeepArchiveService();

    private void age(Path file, int days) throws IOException {
        FileTime old = FileTime.from(Instant.now().minus(days, ChronoUnit.DAYS));
        Files.getFileAttributeView(file, BasicFileAttributeView.class).setTimes(old, old, null);
    }

    @Test
    void findsOnlyOldFiles(@TempDir Path dir) throws IOException {
        Path oldFile = Files.writeString(dir.resolve("old.txt"), "old");
        Path newFile = Files.writeString(dir.resolve("new.txt"), "new");
        age(oldFile, 400);

        List<Path> stale = service.findStale(List.of(oldFile, newFile), 365);

        assertEquals(List.of(oldFile), stale);
    }

    @Test
    void archivesWithEncryptionAndDeletesOriginal(@TempDir Path dir) throws Exception {
        Path file = Files.writeString(dir.resolve("old.txt"), "secret content");
        Path archive = dir.resolve("Deep_Archive.zip");
        char[] password = "pw123".toCharArray();

        int archived = service.archive(List.of(file), archive, password, false);

        assertEquals(1, archived);
        assertTrue(Files.notExists(file));
        try (ZipFile zip = new ZipFile(archive.toFile(), password)) {
            assertTrue(zip.isEncrypted());
            assertTrue(zip.getFileHeader("old.txt") != null);
        }
    }

    @Test
    void dryRunKeepsOriginals(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("old.txt"), "x");

        service.archive(List.of(file), dir.resolve("Deep_Archive.zip"), "pw".toCharArray(), true);

        assertTrue(Files.exists(file));
        assertTrue(Files.notExists(dir.resolve("Deep_Archive.zip")));
    }
}
