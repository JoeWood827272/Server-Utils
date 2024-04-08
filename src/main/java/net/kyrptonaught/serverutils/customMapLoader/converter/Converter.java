package net.kyrptonaught.serverutils.customMapLoader.converter;

import com.google.common.collect.Sets;
import com.google.gson.JsonObject;
import net.kyrptonaught.serverutils.FileHelper;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.customMapLoader.MapSize;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.LobbyMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.ResourcePackList;
import net.kyrptonaught.serverutils.switchableresourcepacks.ResourcePackConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.util.Identifier;
import net.minecraft.world.storage.ChunkStreamVersion;

import java.io.*;
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

        String input = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\input";
        String output = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\output";

        for (BattleMapData map : battlemaps) {
            Path tempOut = Path.of(output).resolve(map.map);

            FileHelper.createDir(tempOut);

            BattleMapAddon addon = new BattleMapAddon();
            addon.addon_id = new Identifier("4jstudios", map.map);
            addon.addon_type = BattleMapAddon.TYPE;
            addon.addon_pack = map.mappack;
            addon.addon_pack_key = map.mappackkey;
            addon.name_key = "lem.battle.mapdecider.menu.voting.mapname." + map.map;
            addon.description_key = "lem.battle.mapdecider.menu.voting.mapdesc." + map.map;
            addon.authors = "4J Studios";
            addon.version = "1.0";

            addon.required_packs = new ResourcePackList();
            addon.required_packs.packs.add(new DummyPack(map.resourcepack));

            for (MapSize mapSize : MapSize.values()) {
                String suffix = (mapSize == MapSize.LARGE ? "" : ("_" + mapSize.fileName));
                Path dir = Path.of(input).resolve(map.map + suffix);

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

            FileHelper.copyFile(Path.of(input).resolve("types").resolve(map.map + ".json"), tempOut.resolve("dimension_type.json"));

            String json = ServerUtilsMod.getGson().toJson(addon);
            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

            FileHelper.zipDirectory(tempOut, Path.of(output).resolve(map.map + ".lemaddon"));
            FileHelper.deleteDir(tempOut);
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
        String input = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\input";
        String output = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\output";

        for (LobbyMapData lobby : lobbyMaps) {
            Path tempOut = Path.of(output).resolve(lobby.map);

            FileHelper.createDir(tempOut);

            LobbyMapAddon addon = new LobbyMapAddon();
            addon.addon_id = new Identifier("4jstudios", lobby.map);
            addon.addon_type = LobbyMapAddon.TYPE;
            addon.addon_pack = "base";
            addon.name_key = "lem.menu.host.config.update.lobby." + lobby.map;
            addon.description_key = "lem.menu.host.config.update.lobby.desc." + lobby.map;
            addon.authors = "4J Studios";
            addon.version = "1.0";

            addon.required_packs = new ResourcePackList();
            addon.required_packs.packs.add(new DummyPack(lobby.resourcepack));

            Path dir = Path.of(input).resolve(lobby.map);

            FileHelper.copyDirectory(dir, tempOut.resolve("world").resolve("lobby"));

            HashMap<String, List<String>> entities = getEntitesForMap(dir);

            addon.spawn_coords = entities.get("LobbyTP").toArray(String[]::new);
            addon.center_coords = entities.get("LobbyCenter").get(0);
            addon.world_border_coords_1 = entities.get("BorderEntity").get(0);
            addon.world_border_coords_2 = entities.get("BorderEntity").get(1);

            addon.winner_coords = lobby.winnercoords;

            FileHelper.copyFile(Path.of(input).resolve("types").resolve(lobby.map + ".json"), tempOut.resolve("dimension_type.json"));

            String json = ServerUtilsMod.getGson().toJson(addon);
            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

            FileHelper.zipDirectory(tempOut, Path.of(output).resolve(lobby.map + ".lemaddon"));
            FileHelper.deleteDir(tempOut);
        }
    }

    public static void LEBModsConvert() {
        String input = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\input\\lebmods";
        String output = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\output";

        try (Stream<Path> files = Files.walk(Path.of(input), 2)) {
            files.forEach(path -> {
                try {
                    if (!Files.isDirectory(path) && path.getFileName().toString().endsWith("config.json")) {
                        String configJson = FileHelper.readFile(path);
                        JsonObject rawConfig = ServerUtilsMod.getGson().fromJson(configJson, JsonObject.class);

                        String mapname = rawConfig.get("name").getAsString();
                        Path tempOut = Path.of(output).resolve(mapname);

                        FileHelper.createDir(tempOut);

                        BattleMapAddon addon = new BattleMapAddon();
                        addon.addon_id = new Identifier("lemcommunity", rawConfig.get("id").getAsString());
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
                            config.world_border_coords_1 = entities.get("BorderEntity").get(0);
                            config.world_border_coords_2 = entities.get("BorderEntity").get(1);
                            config.center_spawn_coords = entities.get("CenterTP").toArray(String[]::new);
                            config.random_spawn_coords = entities.get("RandomTP").toArray(String[]::new);
                            config.chest_tracker_coords = entities.get("Chest").toArray(String[]::new);

                            addon.setMapDataForSize(mapSize, config);
                        }
                        FileHelper.copyFile(path.getParent().resolve("world").resolve("dimension_type.json"), tempOut.resolve("dimension_type.json"));

                        String json = ServerUtilsMod.getGson().toJson(addon);
                        FileHelper.writeFile(tempOut.resolve("addon.json"), json);

                        FileHelper.zipDirectory(tempOut, Path.of(output).resolve(addon.name + ".lemaddon"));
                        FileHelper.deleteDir(tempOut);
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

                raf.seek(4096 * offset + 4); //+4: skip data size

                byte compressionTypeByte = raf.readByte(); //0 - none, 1 - GZIP, 2 deflate - assuming deflate
                chunks.add(NbtIo.readCompound(new DataInputStream(ChunkStreamVersion.DEFLATE.wrap(new FileInputStream(raf.getFD())))));
            }
            return chunks;
        }
    }

    public record BattleMapData(String map, String resourcepack, String mappack, String mappackkey) {
    }

    public record LobbyMapData(String map, String resourcepack, String winnercoords) {

    }

    public static class DummyPack extends ResourcePackConfig.RPOption {
        public DummyPack(String pack) {
            super();
            this.packID = new Identifier(pack);
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