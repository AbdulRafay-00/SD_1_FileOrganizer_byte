package dev.rafay.fileorganizer;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FileCategoryTest {

    @Test
    void mapsCommonExtensions() {
        assertEquals(FileCategory.IMAGES, FileCategory.fromFileName("photo.JPG"));
        assertEquals(FileCategory.DOCUMENTS, FileCategory.fromFileName("report.pdf"));
        assertEquals(FileCategory.ARCHIVES, FileCategory.fromFileName("backup.zip"));
        assertEquals(FileCategory.AUDIO, FileCategory.fromFileName("song.mp3"));
        assertEquals(FileCategory.VIDEO, FileCategory.fromFileName("clip.mkv"));
    }

    @Test
    void unknownOrMissingExtensionGoesToOthers() {
        assertEquals(FileCategory.OTHERS, FileCategory.fromFileName("README"));
        assertEquals(FileCategory.OTHERS, FileCategory.fromFileName(".gitignore"));
        assertEquals(FileCategory.OTHERS, FileCategory.fromFileName("weird.xyz"));
        assertEquals(FileCategory.OTHERS, FileCategory.fromFileName("trailingdot."));
    }
}
