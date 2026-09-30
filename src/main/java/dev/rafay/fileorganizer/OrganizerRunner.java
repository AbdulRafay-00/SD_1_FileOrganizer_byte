package dev.rafay.fileorganizer;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/** Command-line entry: validates arguments and orchestrates organizing + deep archive. */
@Component
public class OrganizerRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OrganizerRunner.class);

    private final OrganizerProperties properties;
    private final FileOrganizerService organizer;
    private final DeepArchiveService archiver;

    public OrganizerRunner(OrganizerProperties properties, FileOrganizerService organizer,
                           DeepArchiveService archiver) {
        this.properties = properties;
        this.organizer = organizer;
        this.archiver = archiver;
    }

    @Override
    public void run(String... args) {
        if (properties.source() == null || properties.source().isBlank()) {
            printUsage();
            return;
        }
        Path source = Path.of(properties.source()).toAbsolutePath().normalize();
        if (!Files.isDirectory(source)) {
            log.error("Source directory does not exist: {}", source);
            return;
        }
        Path target = (properties.target() == null || properties.target().isBlank())
                ? source
                : Path.of(properties.target()).toAbsolutePath().normalize();

        boolean dryRun = properties.dryRun();
        boolean deepArchive = properties.deepArchive();
        String password = properties.archivePassword();

        if (deepArchive && (password == null || password.isBlank())) {
            log.error("--organizer.deep-archive=true requires a password. "
                    + "Set the ARCHIVE_PASSWORD environment variable or --organizer.archive-password.");
            return;
        }
        if (properties.archiveAfterDays() < 1) {
            log.error("--organizer.archive-after-days must be at least 1.");
            return;
        }

        log.info("Source : {}", source);
        log.info("Target : {}", target);
        log.info("Mode   : {}{}", dryRun ? "DRY-RUN (no changes will be made)" : "LIVE",
                deepArchive ? " + deep archive (files unused for " + properties.archiveAfterDays() + "+ days)" : "");

        try {
            List<Path> files = organizer.listFiles(source);
            List<Path> stale = deepArchive
                    ? archiver.findStale(files, properties.archiveAfterDays())
                    : List.of();

            List<Path> toOrganize = new ArrayList<>(files);
            toOrganize.removeAll(stale);

            int organized = organizer.execute(organizer.plan(toOrganize, target), dryRun);
            int archived = stale.isEmpty()
                    ? 0
                    : archiver.archive(stale, target.resolve(DeepArchiveService.ARCHIVE_NAME),
                            password.toCharArray(), dryRun);

            log.info("Done. {} file(s) {}{}.", organized, dryRun ? "would be organized" : "organized",
                    deepArchive ? ", " + archived + (dryRun ? " would be archived" : " archived") : "");
        } catch (IOException e) {
            log.error("Failed: {}", e.getMessage());
        }
    }

    private void printUsage() {
        System.out.println("""
                File Organizer

                Usage:
                  java -jar file-organizer-1.0.0.jar --organizer.source=<dir> [options]

                Options:
                  --organizer.source=<dir>            Directory to organize (required)
                  --organizer.target=<dir>            Base directory for category folders (default: source)
                  --organizer.dry-run=true            List planned moves without changing anything
                  --organizer.deep-archive=true       Zip files unused for a long time into an encrypted
                                                      Deep_Archive.zip and delete the originals
                  --organizer.archive-after-days=365  Age threshold for deep archive (default: 365)
                  --organizer.archive-password=<pw>   Archive password (or set env ARCHIVE_PASSWORD)
                """);
    }
}
