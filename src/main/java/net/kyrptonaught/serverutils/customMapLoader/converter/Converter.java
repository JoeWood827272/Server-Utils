package net.kyrptonaught.serverutils.customMapLoader.converter;

import com.google.common.collect.Sets;
import net.kyrptonaught.serverutils.FileHelper;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.customMapLoader.MapSize;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.LobbyMapAddon;
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

    public static void BattleConvert() {
        long time = System.nanoTime();
        String[] maps = new String[]{
                "atlantis",
                "atomics",
                "capitol",
                "castle",
                "dig",
                "festive",
                "halloween",
                "invasion",
                "lair",
                "libertalia",
                "medusa",
                "ruin",
                "siege",
                "shipyard",
                "valley",
                "castle",
                "cavern",
                "cove",
                "crucible",
                "frontier",
                "shrunk",
                "temple"
        };

        String input = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\input";
        String output = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\output";

        for (String map : maps) {
            Path tempOut = Path.of(output).resolve(map);

            FileHelper.createDir(tempOut);

            BattleMapAddon addon = new BattleMapAddon();
            addon.addon_id = new Identifier("4jstudios", map);
            addon.addon_type = BattleMapAddon.TYPE;
            addon.addon_pack = "mappack2";
            addon.addon_pack_key = "lem.battle.mapdecider.menu.voting.mappack2";
            addon.name_key = "lem.battle.mapdecider.menu.voting.mapname." + map;
            addon.description_key = "lem.battle.mapdecider.menu.voting.mapdesc." + map;
            addon.authors = "4J Studios";
            addon.version = "1.0";

            for (MapSize mapSize : MapSize.values()) {
                String suffix = (mapSize == MapSize.LARGE ? "" : ("_" + mapSize.fileName));
                Path dir = Path.of(input).resolve(map + suffix);

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

            FileHelper.copyFile(Path.of(input).resolve("types").resolve(map + ".json"), tempOut.resolve("dimension_type.json"));

            String json = ServerUtilsMod.getGson().toJson(addon);
            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

            FileHelper.zipDirectory(tempOut, Path.of(output).resolve(map + ".lemaddon"));
            FileHelper.deleteDir(tempOut);

            System.out.println(addon.addon_id);
        }

        System.out.println("Took: " + (System.nanoTime() - time));
    }

    public static void LobbyConvert() {
        long time = System.nanoTime();
        String[] maps = new String[]{
                "lobby_new",
                "lobby_anniversary",
                "lobby_festive",
                "lobby_halloween",
                "lobby_old"
        };

        String input = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\input";
        String output = "C:\\Users\\antho\\Desktop\\Minecraft Mod Dev\\LEMAddonConverter\\output";

        for (String map : maps) {
            Path tempOut = Path.of(output).resolve(map);

            FileHelper.createDir(tempOut);

            LobbyMapAddon addon = new LobbyMapAddon();
            addon.addon_id = new Identifier("4jstudios", map);
            addon.addon_type = LobbyMapAddon.TYPE;
            addon.addon_pack = "base";
            addon.name_key = "lem.menu.host.config.update.lobby." + map;
            addon.description_key = "lem.menu.host.config.update.lobby.desc." + map;
            addon.authors = "4J Studios";
            addon.version = "1.0";

            Path dir = Path.of(input).resolve(map);

            FileHelper.copyDirectory(dir, tempOut.resolve("world").resolve("lobby"));

            HashMap<String, List<String>> entities = getEntitesForMap(dir);

            addon.spawn_coords = entities.get("LobbyTP").toArray(String[]::new);
            addon.center_coords = entities.get("LobbyCenter").get(0);
            addon.world_border_coords_1 = entities.get("BorderEntity").get(0);
            addon.world_border_coords_2 = entities.get("BorderEntity").get(1);

            addon.winner_coords = "-357 70 -341 -90 0";

            FileHelper.copyFile(Path.of(input).resolve("types").resolve(map + ".json"), tempOut.resolve("dimension_type.json"));

            String json = ServerUtilsMod.getGson().toJson(addon);
            FileHelper.writeFile(tempOut.resolve("addon.json"), json);

            FileHelper.zipDirectory(tempOut, Path.of(output).resolve(map + ".lemaddon"));
            FileHelper.deleteDir(tempOut);

            System.out.println(addon.addon_id);
        }

        System.out.println("Took: " + (System.nanoTime() - time));
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
