package net.kyrptonaught.serverutils.customMapLoader.addons;

import net.minecraft.resource.*;
import net.minecraft.resource.fs.ResourceFileSystem;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.path.SymlinkEntry;
import net.minecraft.util.path.SymlinkFinder;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public class AddonResourcePackProvider implements ResourcePackProvider {
    public static final ResourcePackSource LEMADDON = ResourcePackSource.create(name -> name, true);
    private static final ResourcePackPosition POSITION = new ResourcePackPosition(false, ResourcePackProfile.InsertionPosition.TOP, false);

    private final HashMap<Identifier, Path> loadedAddons = new HashMap<>();

    public void loadAddon(Identifier id, Path path) {
        loadedAddons.put(id, path.resolve("datapack"));
    }

    public void unloadAddon(Identifier id) {
        loadedAddons.remove(id);
    }

    @Override
    public void register(Consumer<ResourcePackProfile> profileAdder) {
        loadedAddons.forEach((id, path) -> {
            try {
                ResourcePackProfile.PackFactory packFactory = new PackOpenerImpl(new SymlinkFinder(path2 -> true)).open(path, List.of());

                ResourcePackInfo resourcePackInfo = new ResourcePackInfo(getID(id), Text.literal(id.toString()), LEMADDON, Optional.empty());
                ResourcePackProfile resourcePackProfile = ResourcePackProfile.create(resourcePackInfo, packFactory, ResourceType.SERVER_DATA, POSITION);
                if (resourcePackProfile != null) {
                    profileAdder.accept(resourcePackProfile);
                }
            } catch (IOException iOException) {
                System.out.println("Error loading lemaddon data pack: " + id);
            }

        });

    }

    public static String getID(Identifier identifier) {
        return "lemaddon/" + identifier;
    }

    static class PackOpenerImpl extends ResourcePackOpener<ResourcePackProfile.PackFactory> {
        protected PackOpenerImpl(SymlinkFinder symlinkFinder) {
            super(symlinkFinder);
        }

        @Nullable
        @Override
        public ResourcePackProfile.PackFactory open(Path path, List<SymlinkEntry> foundSymlinks) throws IOException {
            return new DirectoryResourcePack.DirectoryBackedFactory(path);
        }

        @Nullable
        @Override
        protected ResourcePackProfile.PackFactory openZip(Path path) {
            FileSystem fileSystem = path.getFileSystem();
            if (fileSystem == FileSystems.getDefault() || fileSystem instanceof ResourceFileSystem) {
                return new ZipResourcePack.ZipBackedFactory(path);
            }
            return null;
        }

        @Nullable
        @Override
        protected ResourcePackProfile.PackFactory openDirectory(Path path) {
            return new DirectoryResourcePack.DirectoryBackedFactory(path);
        }
    }
}
