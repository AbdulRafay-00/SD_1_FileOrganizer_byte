package dev.rafay.fileorganizer;

import java.util.Locale;
import java.util.Set;

/** Maps file extensions to destination folder names. */
public enum FileCategory {
    IMAGES("Images", Set.of("jpg", "jpeg", "png", "gif", "bmp", "webp", "svg", "tiff", "ico", "heic")),
    DOCUMENTS("Documents", Set.of("pdf", "doc", "docx", "txt", "rtf", "odt", "xls", "xlsx",
            "ppt", "pptx", "csv", "md")),
    ARCHIVES("Archives", Set.of("zip", "rar", "7z", "tar", "gz", "bz2", "xz")),
    AUDIO("Audio", Set.of("mp3", "wav", "flac", "aac", "ogg", "m4a")),
    VIDEO("Video", Set.of("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm")),
    CODE("Code", Set.of("java", "py", "js", "ts", "html", "css", "json", "xml", "c", "cpp", "sql")),
    OTHERS("Others", Set.of());

    private final String folderName;
    private final Set<String> extensions;

    FileCategory(String folderName, Set<String> extensions) {
        this.folderName = folderName;
        this.extensions = extensions;
    }

    public String folderName() {
        return folderName;
    }

    /** Returns the category for a file name; unknown or missing extensions go to OTHERS. */
    public static FileCategory fromFileName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0 || dot == fileName.length() - 1) {
            return OTHERS;
        }
        String ext = fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
        for (FileCategory category : values()) {
            if (category.extensions.contains(ext)) {
                return category;
            }
        }
        return OTHERS;
    }
}
