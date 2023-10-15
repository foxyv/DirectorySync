package ninja.foxyv.vsync.entities;

import java.nio.file.Path;
import java.util.List;

public record DirectoryFingerprint(Path directoryPath, List<DirectoryFingerprint> subDirectories, List<FileFingerprint> children) {
}
