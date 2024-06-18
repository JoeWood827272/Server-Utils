package net.kyrptonaught.serverutils.customMapLoader;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.serverutils.FileHelper;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.customMapLoader.addons.BaseAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.LobbyMapAddon;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.SimpleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static net.kyrptonaught.serverutils.customMapLoader.CustomMapLoaderMod.BATTLE_MAPS;
import static net.kyrptonaught.serverutils.customMapLoader.CustomMapLoaderMod.LOBBY_MAPS;

public class IO {
    public static void discoverAddons(MinecraftServer server) {
        Path dir = FabricLoader.getInstance().getGameDir().resolve("lemaddons");
        try (Stream<Path> files = Files.walk(dir, 2)) {
            files.forEach(path -> {
                try {
                    if (!Files.isDirectory(path) && path.getFileName().toString().endsWith(".lemaddon")) {
                        String configJson = FileHelper.readFileFromZip(path, "addon.json");

                        BaseAddon config = null;
                        JsonObject rawConfig = ServerUtilsMod.getGson().fromJson(configJson, JsonObject.class);

                        String type = rawConfig.get("addon_type").getAsString();
                        if (BattleMapAddon.TYPE.equals(type))
                            config = ServerUtilsMod.getGson().fromJson(rawConfig, BattleMapAddon.class);
                        if (LobbyMapAddon.TYPE.equals(type))
                            config = ServerUtilsMod.getGson().fromJson(rawConfig, LobbyMapAddon.class);

                        if (path.getParent().getFileName().toString().equals("base")) {
                            config.isBaseAddon = true;
                            config.addon_pack = "base_" + config.addon_pack;
                        } else if (!path.getParent().getFileName().toString().equals("lemaddons")) {
                            config.addon_pack = path.getParent().getFileName().toString();
                        }

                        if (config.addon_pack == null || config.addon_pack.isEmpty()) {
                            config.addon_pack = "Other";
                        }

                        config.addon_pack = FileHelper.cleanFileName(config.addon_pack);

                        config.filePath = path;

                        if (config instanceof BattleMapAddon battleConfig) {
                            if (battleConfig.dimensionType_id == null) {
                                battleConfig.dimensionType_id = config.addon_id;
                                battleConfig.loadedDimensionType = addDimensionType(server, battleConfig.filePath, battleConfig.dimensionType_id);
                            }
                            BATTLE_MAPS.put(config.addon_id, battleConfig);
                        } else if (config instanceof LobbyMapAddon lobbyConfig) {
                            if (lobbyConfig.dimensionType_id == null) {
                                lobbyConfig.dimensionType_id = config.addon_id;
                                lobbyConfig.loadedDimensionType = addDimensionType(server, lobbyConfig.filePath, lobbyConfig.dimensionType_id);
                            }
                            LOBBY_MAPS.put(config.addon_id, lobbyConfig);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void unZipMap(Path outputPath, Path filepath, String baseDirectory) {
        try (ZipFile zip = new ZipFile(filepath.toFile())) {

            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                try {
                    ZipEntry entry = entries.nextElement();
                    String entryName = FileHelper.fixPathSeparator(entry.getName());
                    if (entryName.startsWith(baseDirectory)) {
                        extractFile(zip, entry, outputPath.resolve(entryName.replace(baseDirectory, "")));
                    } else if (entryName.startsWith("datapack/")) {
                        extractFile(zip, entry, outputPath.resolve(entryName));
                    }
                } catch (FileAlreadyExistsException ignored) {
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void extractFile(ZipFile zip, ZipEntry entry, Path newOut) throws IOException {
        if (entry.isDirectory()) {
            Files.createDirectories(newOut);
        } else {
            Files.createDirectories(newOut.getParent());
            Files.copy(zip.getInputStream(entry), newOut, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static DimensionType addDimensionType(MinecraftServer server, Path addonPath, Identifier dimID) {
        String dimJson = FileHelper.readFileFromZip(addonPath, "dimension_type.json");
        if (dimJson != null) {
            DataResult<DimensionType> result = DimensionType.CODEC.parse(JsonOps.INSTANCE, ServerUtilsMod.getGson().fromJson(dimJson, JsonElement.class));
            DimensionType type = result.result().get();

            SimpleRegistry<DimensionType> dimensionTypeRegistry = (SimpleRegistry<DimensionType>) server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);

            if (!dimensionTypeRegistry.containsId(dimID))
                Registry.register(dimensionTypeRegistry, dimID, type);
            return type;
        }
        return null;
    }
}
