package dev.rafay.fileorganizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.model.enums.AesKeyStrength;
import net.lingala.zip4j.model.enums.CompressionMethod;
import net.lingala.zip4j.model.enums.EncryptionMethod;

/**
 * The "Black Hole" cleanup: finds files that have not been opened for a long time,
 * stores them in an AES-256 encrypted Deep_Archive.zip and deletes the originals,
 * but only after verifying that each file was written to the archive correctly.
 */
@Service
public class DeepArchiveService {

    public static final String ARCHIVE_NAME = "Deep_Archive.zip";

    private static final Logger log = LoggerFactory.getLogger(DeepArchiveService.class);

    /**
     * Returns files whose most recent activity is older than {@code days}.
     * "Most recent activity" is the later of last-access and last-modified time, because
     * many systems (Windows especially) do not update last-access time reliably.
     */
    public List<Path> findStale(List<Path> files, int days) throws IOException {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        List<Path> stale = new ArrayList<>();
        for (Path file : files) {
            BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
            Instant lastAccess = attrs.lastAccessTime().toInstant();
            Instant lastModified = attrs.lastModifiedTime().toInstant();
            Instant lastUsed = lastAccess.isAfter(lastModified) ? lastAccess : lastModified;
            if (lastUsed.isBefore(cutoff)) {
                stale.add(file);
            }
        }
        return stale;
    }

    /**
     * Adds the files to an encrypted archive and deletes each original once it has been
     * verified inside the archive. In dry-run mode nothing is touched. Returns the number archived.
     */
    public int archive(List<Path> files, Path archivePath, char[] password, boolean dryRun) throws IOException {
        if (dryRun) {
            for (Path file : files) {
                log.info("[DRY-RUN] Would archive and delete {} (not opened for a long time)",
                        file.getFileName());
            }
            return files.size();
        }

        Files.createDirectories(archivePath.toAbsolutePath().getParent());
        int archived = 0;
        try (ZipFile zip = new ZipFile(archivePath.toFile(), password)) {
            for (Path file : files) {
                String entryName = uniqueEntryName(zip, file.getFileName().toString());
                try {
                    ZipParameters params = new ZipParameters();
                    params.setCompressionMethod(CompressionMethod.DEFLATE);
                    params.setEncryptFiles(true);
                    params.setEncryptionMethod(EncryptionMethod.AES);
                    params.setAesKeyStrength(AesKeyStrength.KEY_STRENGTH_256);
                    params.setFileNameInZip(entryName);
                    zip.addFile(file.toFile(), params);

                    FileHeader header = zip.getFileHeader(entryName);
                    if (header == null || header.getUncompressedSize() != Files.size(file)) {
                        log.error("Verification failed for {}; original kept", file.getFileName());
                        continue;
                    }
                    Files.delete(file);
                    log.info("Archived {} -> {}/{} and deleted the original",
                            file.getFileName(), ARCHIVE_NAME, entryName);
                    archived++;
                } catch (IOException e) {
                    log.error("Failed to archive {}: {}; original kept", file.getFileName(), e.getMessage());
                }
            }
        }
        return archived;
    }

    private String uniqueEntryName(ZipFile zip, String fileName) throws IOException {
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot > 0 ? fileName.substring(dot) : "";
        String candidate = fileName;
        int counter = 1;
        while (zip.getFileHeader(candidate) != null) {
            candidate = base + "_" + counter + ext;
            counter++;
        }
        return candidate;
    }
}
