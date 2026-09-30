package dev.rafay.fileorganizer;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Settings bound from application.properties or command-line arguments,
 * e.g. {@code --organizer.source=/path --organizer.dry-run=true}.
 */
@ConfigurationProperties(prefix = "organizer")
public record OrganizerProperties(
        String source,
        String target,
        @DefaultValue("false") boolean dryRun,
        @DefaultValue("false") boolean deepArchive,
        String archivePassword,
        @DefaultValue("365") int archiveAfterDays) {
}
