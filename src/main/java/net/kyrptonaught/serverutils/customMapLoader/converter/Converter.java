package net.kyrptonaught.serverutils.customMapLoader.converter;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.kyrptonaught.serverutils.FileHelper;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.customMapLoader.MapSize;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMusic;
import net.kyrptonaught.serverutils.customMapLoader.addons.LobbyMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.ResourcePackList;
import net.kyrptonaught.serverutils.switchableresourcepacks.ResourcePack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.world.storage.ChunkCompressionFormat;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

public class Converter {

    public static void ConvertAll() {
        BattleConvert();
        LobbyConvert();
        LEBModsConvert();
    }

    public static void BattleConvert() {
        BattleMapData[] battlemaps = new BattleMapData[]{
                new BattleMapData("cavern", "lem.base:vanilla", "mappack0", "lem.battle.mapdecider.menu.voting.mappack0"),
                new BattleMapData("cove", "lem.base:vanilla", "mappack0", "lem.battle.mapdecider.menu.voting.mappack0"),
                new BattleMapData("crucible", "lem.base:vanilla", "mappack0", "lem.battle.mapdecider.menu.voting.mappack0"),

                new BattleMapData("lair", "lem.base:fantasy", "mappack1", "lem.battle.mapdecider.menu.voting.mappack1"),
                new BattleMapData("medusa", "lem.base:greek", "mappack1", "lem.battle.mapdecider.menu.voting.mappack1"),
                new BattleMapData("temple", "lem.base:vanilla", "mappack1", "lem.battle.mapdecider.menu.voting.mappack1"),

                new BattleMapData("atlantis", "lem.base:greek", "mappack2", "lem.battle.mapdecider.menu.voting.mappack2"),
                new BattleMapData("ruin", "lem.base:city", "mappack2", "lem.battle.mapdecider.menu.voting.mappack2"),
                new BattleMapData("siege", "lem.base:fantasy", "mappack2", "lem.battle.mapdecider.menu.voting.mappack2"),

                new BattleMapData("castle", "lem.base:vanilla", "mappack3", "lem.battle.mapdecider.menu.voting.mappack3"),
                new BattleMapData("invasion", "lem.base:city", "mappack3", "lem.battle.mapdecider.menu.voting.mappack3"),
                new BattleMapData("shipyard", "lem.base:steampunk", "mappack3", "lem.battle.mapdecider.menu.voting.mappack3"),

                new BattleMapData("dig", "lem.base:vanilla", "mappack4", "lem.battle.mapdecider.menu.voting.mappack4"),
                new BattleMapData("frontier", "lem.base:vanilla", "mappack4", "lem.battle.mapdecider.menu.voting.mappack4"),
                new BattleMapData("shrunk", "lem.base:plastic", "mappack4", "lem.battle.mapdecider.menu.voting.mappack4"),

                new BattleMapData("valley", "lem.base:chinese", "base", ""),
                new BattleMapData("halloween", "lem.base:halloween", "base", ""),
                new BattleMapData("festive", "lem.base:festive", "base", ""),

                new BattleMapData("atomics", "lem.base:fallout", "mappackfallout", "lem.battle.mapdecider.menu.voting.fallout"),
                new BattleMapData("capitol", "lem.base:fallout", "mappackfallout", "lem.battle.mapdecider.menu.voting.fallout"),
                new BattleMapData("libertalia", "lem.base:fallout", "mappackfallout", "lem.battle.mapdecider.menu.voting.fallout"),
        };

        Path input = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("battle").resolve("input");
        Path dimensionTypes = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("battle").resolve("dimension_types");
        Path output = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("battle").resolve("output");

        FileHelper.createDir(input);
        FileHelper.createDir(dimensionTypes);

        try (Stream<Path> files = Files.walk(input, 1)) {
            files.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        String fileName = path.getFileName().toString();

                        BattleMapData map = null;

                        for (BattleMapData battleMapData : battlemaps) {
                            if (battleMapData.mapName.equals(fileName)) {
                                map = battleMapData;
                                break;
                            }
                        }

                        if (map != null) {
                            Path tempOut = output.resolve(map.mapName);

                            FileHelper.createDir(tempOut);

                            BattleMapAddon addon = new BattleMapAddon();
                            addon.addon_id = Identifier.of("4jstudios", map.mapName);
                            addon.addon_type = BattleMapAddon.TYPE;
                            addon.addon_pack = map.mappack;
                            addon.addon_pack_key = map.mappackkey;
                            addon.name_key = "lem.battle.mapdecider.menu.voting.mapname." + map.mapName;
                            addon.description_key = "lem.battle.mapdecider.menu.voting.mapdesc." + map.mapName;
                            addon.authors = "4J Studios";
                            addon.version = "1.0";

                            addon.required_packs = new ResourcePackList();
                            addon.required_packs.packs.add(new DummyPack(map.resourcepack));

                            addon.music_pack = new DummyMusic(map.resourcepack.replace("lem.base", "lem.battle"));
                            if (map.resourcepack.equals("lem.base:fallout"))
                                addon.safe_music_pack = new DummyMusic("lem.battle:vanilla");

                            for (MapSize mapSize : MapSize.values()) {
                                String suffix = (mapSize == MapSize.LARGE ? "" : ("_" + mapSize.fileName));
                                Path dir = input.resolve(map.mapName + suffix);

                                if (!Files.exists(dir)) continue;

                                FileHelper.copyDirectory(dir, tempOut.resolve("world").resolve(mapSize.fileName));

                                HashMap<String, List<String>> entities = getEntitesForMap(dir);

                                BattleMapAddon.MapSizeConfig config = new BattleMapAddon.MapSizeConfig();

                                config.center_coords = entities.get("MapCenter").get(0);
                                config.world_border_coords_1 = entities.get("BorderEntity").get(0);
                                config.world_border_coords_2 = entities.get("BorderEntity").get(1);
                                config.center_spawn_coords = entities.get("CenterTP").toArray(String[]::new);
                                config.random_spawn_coords = entities.get("RandomTP").toArray(String[]::new);
                                config.chest_tracker_coords = entities.get("Chest").toArray(String[]::new);

                                addon.setMapDataForSize(mapSize, config);
                            }

                            fixDimensionTypeFile(dimensionTypes.resolve(map.mapName + ".json"));
                            FileHelper.copyFile(dimensionTypes.resolve(map.mapName + ".json"), tempOut.resolve("dimension_type.json"));

                            String json = ServerUtilsMod.getGson().toJson(addon);
                            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

                            FileHelper.zipDirectory(tempOut, output.resolve(map.mapName + ".lemaddon"));
                            FileHelper.deleteDir(tempOut);

                            System.out.println("Converted: " + map.mapName);
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

    public static void LobbyConvert() {
        LobbyMapData[] lobbyMaps = new LobbyMapData[]{
                new LobbyMapData("lobby_new", "lem.base:vanilla", "-357 70 -341 -90 0"),
                new LobbyMapData("lobby_anniversary", "lem.base:vanilla", ""),
                new LobbyMapData("lobby_festive", "lem.base:festive", "-357 70 -341 -90 0"),
                new LobbyMapData("lobby_halloween", "lem.base:halloween", "-357 70 -341 -90 0"),
                new LobbyMapData("lobby_old", "lem.base:vanilla", ""),

        };

        Path input = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("lobby").resolve("input");
        Path dimensionTypes = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("lobby").resolve("dimension_types");
        Path output = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("lobby").resolve("output");

        FileHelper.createDir(input);
        FileHelper.createDir(dimensionTypes);

        try (Stream<Path> files = Files.walk(input, 1)) {
            files.forEach(path -> {
                try {
                    if (Files.isDirectory(path)) {
                        String fileName = path.getFileName().toString();

                        LobbyMapData lobby = null;

                        for (LobbyMapData lobbyMapData : lobbyMaps) {
                            if (lobbyMapData.mapName.equals(fileName)) {
                                lobby = lobbyMapData;
                                break;
                            }
                        }

                        if (lobby != null) {
                            Path tempOut = output.resolve(lobby.mapName);

                            FileHelper.createDir(tempOut);

                            LobbyMapAddon addon = new LobbyMapAddon();
                            addon.addon_id = Identifier.of("4jstudios", lobby.mapName);
                            addon.addon_type = LobbyMapAddon.TYPE;
                            addon.addon_pack = "base";
                            addon.name_key = "lem.menu.host.config.update.lobby." + lobby.mapName;
                            addon.description_key = "lem.menu.host.config.update.lobby.desc." + lobby.mapName;
                            addon.authors = "4J Studios";
                            addon.version = "1.0";

                            addon.required_packs = new ResourcePackList();
                            addon.required_packs.packs.add(new DummyPack(lobby.resourcepack));

                            Path dir = input.resolve(lobby.mapName);

                            FileHelper.copyDirectory(dir, tempOut.resolve("world").resolve("lobby"));

                            HashMap<String, List<String>> entities = getEntitesForMap(dir);

                            addon.spawn_coords = entities.get("LobbyTP").toArray(String[]::new);
                            addon.center_coords = entities.get("LobbyCenter").get(0);
                            addon.world_border_coords_1 = entities.get("BorderEntity").get(0);
                            addon.world_border_coords_2 = entities.get("BorderEntity").get(1);

                            addon.winner_coords = lobby.winnercoords;

                            fixDimensionTypeFile(dimensionTypes.resolve(lobby.mapName + ".json"));
                            FileHelper.copyFile(dimensionTypes.resolve(lobby.mapName + ".json"), tempOut.resolve("dimension_type.json"));

                            String json = ServerUtilsMod.getGson().toJson(addon);
                            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

                            FileHelper.zipDirectory(tempOut, output.resolve(lobby.mapName + ".lemaddon"));
                            FileHelper.deleteDir(tempOut);
                            System.out.println("Converted: " + lobby.mapName);
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

    public static void LEBModsConvert() {
        Path input = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("lebmods").resolve("input");
        Path output = FabricLoader.getInstance().getGameDir().resolve("addonconverter").resolve("lebmods").resolve("output");

        FileHelper.createDir(input);

        try (Stream<Path> files = Files.walk(input, 2)) {
            files.forEach(path -> {
                try {
                    if (!Files.isDirectory(path) && path.getFileName().toString().endsWith("config.json")) {
                        String configJson = FileHelper.readFile(path);
                        JsonObject rawConfig = ServerUtilsMod.getGson().fromJson(configJson, JsonObject.class);

                        String mapname = rawConfig.get("name").getAsString();
                        Path tempOut = output.resolve(mapname);

                        FileHelper.createDir(tempOut);

                        BattleMapAddon addon = new BattleMapAddon();
                        addon.addon_id = Identifier.of("lemcommunity", rawConfig.get("id").getAsString());
                        addon.addon_type = BattleMapAddon.TYPE;
                        addon.addon_pack = "Other";
                        addon.name = mapname;
                        addon.description = rawConfig.get("description").getAsString();
                        addon.authors = rawConfig.get("authors").getAsString();
                        addon.version = rawConfig.get("version").getAsString();

                        addon.required_packs = new ResourcePackList();
                        addon.required_packs.packs.add(new DummyPack("lem.base:" + rawConfig.get("pack").getAsString()));

                        for (MapSize mapSize : MapSize.values()) {
                            Path dir = path.getParent().resolve("world").resolve(mapSize.fileName);

                            if (!Files.exists(dir)) continue;

                            FileHelper.copyDirectory(dir, tempOut.resolve("world").resolve(mapSize.fileName));

                            HashMap<String, List<String>> entities = getEntitesForMap(dir);

                            BattleMapAddon.MapSizeConfig config = new BattleMapAddon.MapSizeConfig();

                            config.center_coords = entities.get("MapCenter").get(0);
                            config.center_spawn_coords = entities.get("CenterTP").toArray(String[]::new);
                            config.random_spawn_coords = entities.get("RandomTP").toArray(String[]::new);
                            config.chest_tracker_coords = entities.get("Chest").toArray(String[]::new);

                            List<String> borders = entities.get("BorderEntity");
                            if (borders != null && borders.size() >= 2) {
                                config.world_border_coords_1 = borders.get(0);
                                config.world_border_coords_2 = borders.get(1);
                            }

                            addon.setMapDataForSize(mapSize, config);
                        }

                        fixDimensionTypeFile(path.getParent().resolve("world").resolve("dimension_type.json"));
                        FileHelper.copyFile(path.getParent().resolve("world").resolve("dimension_type.json"), tempOut.resolve("dimension_type.json"));

                        String json = ServerUtilsMod.getGson().toJson(addon);
                        FileHelper.writeFile(tempOut.resolve("addon.json"), json);

                        FileHelper.zipDirectory(tempOut, output.resolve(addon.name + ".lemaddon"));
                        FileHelper.deleteDir(tempOut);
                        System.out.println("Converted: " + addon.name);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static HashMap<String, List<String>> getEntitesForMap(Path dir) {
        HashMap<String, List<String>> entities = new HashMap<>();

        try (Stream<Path> files = Files.walk(dir.resolve("entities"), 1)) {
            files.forEach(path -> {
                try {
                    if (!Files.isDirectory(path) && path.getFileName().toString().endsWith(".mca")) {
                        List<NbtCompound> chunks = readMCAFile(path);

                        for (NbtCompound nbt : chunks) {
                            if (nbt.contains("Entities")) {
                                NbtList entityList = nbt.getList("Entities", NbtElement.COMPOUND_TYPE);
                                for (NbtElement raw : entityList) {
                                    EntityData data = EntityData.parse((NbtCompound) raw);

                                    data.tags.forEach(tag -> {
                                        if (!entities.containsKey(tag)) entities.put(tag, new ArrayList<>());
                                        entities.get(tag).add(data.pos);
                                    });
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entities;
    }

    private static List<NbtCompound> readMCAFile(Path file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file.toFile(), "r")) {
            List<NbtCompound> chunks = new ArrayList<>();

            for (int i = 0; i < 1024; i++) {
                raf.seek(i * 4);
                int offset = raf.read() << 16;
                offset |= (raf.read() & 0xFF) << 8;
                offset |= raf.read() & 0xFF;
                if (raf.readByte() == 0) {
                    continue;
                }
                raf.seek(4096 + i * 4);
                int timestamp = raf.readInt();

                raf.seek(4096L * offset + 4); //+4: skip data size

                byte compressionTypeByte = raf.readByte(); //0 - none, 1 - GZIP, 2 deflate - assuming deflate
                chunks.add(NbtIo.readCompound(new DataInputStream(ChunkCompressionFormat.DEFLATE.wrap(new FileInputStream(raf.getFD())))));
            }
            return chunks;
        }
    }

    private static void fixDimensionTypeFile(Path file) {
        JsonObject json = ServerUtilsMod.getGson().fromJson(FileHelper.readFile(file), JsonObject.class);

        JsonObject monsterLight = json.getAsJsonObject("monster_spawn_light_level");

        if (monsterLight.has("value") && monsterLight.get("value").isJsonObject()) {
            JsonObject value = monsterLight.getAsJsonObject("value");
            value.keySet().forEach(key -> monsterLight.add(key, value.get(key)));
            monsterLight.remove("value");
        }

        FileHelper.writeFile(file, ServerUtilsMod.getGson().toJson(json));
    }

    public record BattleMapData(String mapName, String resourcepack, String mappack, String mappackkey) {
    }

    public record LobbyMapData(String mapName, String resourcepack, String winnercoords) {
    }

    public static class DummyPack extends ResourcePack {
        public DummyPack(String pack) {
            super();
            this.packID = Identifier.of(pack);
        }
    }

    public static class DummyMusic extends BattleMusic {
        public DummyMusic(String pack) {
            super();
            this.packID = Identifier.of(pack);
        }
    }

    public static class EntityData {
        public String pos;
        public Set<String> tags;

        public EntityData(String pos) {
            this.pos = pos;
            this.tags = Sets.newHashSet();
        }

        public static EntityData parse(NbtCompound entity) {
            NbtList tags = entity.getList("Tags", NbtElement.STRING_TYPE);
            NbtList pos = entity.getList("Pos", NbtElement.DOUBLE_TYPE);

            EntityData entityData = new EntityData(pos.getDouble(0) + " " + pos.getDouble(1) + " " + pos.getDouble(2));

            for (NbtElement element : tags) {
                entityData.tags.add(element.asString());
            }
            return entityData;
        }
    }
}