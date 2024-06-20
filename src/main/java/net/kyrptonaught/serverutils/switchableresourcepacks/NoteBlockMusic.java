package net.kyrptonaught.serverutils.switchableresourcepacks;

import net.kyrptonaught.noteblockplayer.Commands;
import net.raphimc.noteblocklib.format.nbs.NbsSong;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class NoteBlockMusic {

    public static void cacheSong(String id, Path songPath) {
        NbsSong song = Commands.loadSong(songPath);
        Commands.songCache.put(id, song);
    }

    public static void clearCache() {
        Commands.songCache.clear();
    }

    public static void cacheAll(Path input) {
        clearCache();
        try (Stream<Path> files = Files.walk(input)) {
            files.forEach(path -> {
                if (path.getFileName().toString().endsWith(".nbs")) {
                    String id = path.getFileName().toString().replace(".nbs", "");
                    cacheSong(id, path);
                }
            });
        } catch (Exception ignored) {
        }
    }
}