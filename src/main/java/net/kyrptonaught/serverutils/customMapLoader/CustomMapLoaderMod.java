package net.kyrptonaught.serverutils.customMapLoader;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyrptonaught.serverutils.CMDHelper;
import net.kyrptonaught.serverutils.ModuleWConfig;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.chestTracker.ChestTrackerMod;
import net.kyrptonaught.serverutils.customMapLoader.addons.BaseMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.LobbyMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.addons.ResourcePackList;
import net.kyrptonaught.serverutils.customMapLoader.voting.HostOptions;
import net.kyrptonaught.serverutils.customMapLoader.voting.Votebook;
import net.kyrptonaught.serverutils.customWorldBorder.CustomWorldBorderMod;
import net.kyrptonaught.serverutils.datapackInteractables.DatapackInteractables;
import net.kyrptonaught.serverutils.dimensionLoader.CustomDimHolder;
import net.kyrptonaught.serverutils.dimensionLoader.DimensionLoaderMod;
import net.kyrptonaught.serverutils.discordBridge.MessageSender;
import net.kyrptonaught.serverutils.playerJoinLocation.PlayerJoinLocationMod;
import net.kyrptonaught.serverutils.playerlockdown.PlayerLockdownMod;
import net.kyrptonaught.serverutils.switchableresourcepacks.ResourcePack;
import net.kyrptonaught.serverutils.switchableresourcepacks.SwitchableResourcepacksMod;
import net.minecraft.registry.CombinedDynamicRegistries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.ServerDynamicRegistryType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.network.ServerConfigurationNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.dimension.DimensionType;

import java.nio.file.Path;
import java.util.*;

public class CustomMapLoaderMod extends ModuleWConfig<CustomMapLoaderConfig> {

    public static final HashMap<Identifier, BattleMapAddon> BATTLE_MAPS = new HashMap<>();
    public static final HashMap<Identifier, LobbyMapAddon> LOBBY_MAPS = new HashMap<>();

    private static final HashMap<Identifier, LoadedBattleMapInstance> LOADED_BATTLE_MAPS = new HashMap<>();
    private static final HashMap<Identifier, LobbyMapAddon> LOADED_LOBBIES = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(CustomMapLoaderMod::reloadAddonFiles);
        ServerTickEvents.START_SERVER_TICK.register(CustomMapLoaderMod::serverTick);
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        CustomMapLoaderCommands.registerCommands(dispatcher);
    }

    @Override
    public CustomMapLoaderConfig createDefaultConfig() {
        return new CustomMapLoaderConfig();
    }

    public static void reloadAddonFiles(MinecraftServer server) {
        BATTLE_MAPS.clear();
        LOBBY_MAPS.clear();

        Registry<DimensionType> dimensionTypeRegistry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
        ((RegistryUnfreezer) dimensionTypeRegistry).unfreeze();
        IO.discoverAddons(server);
        dimensionTypeRegistry.freeze();

        Votebook.generateBookLibrary(getAllBattleMaps());


        if (server.getPlayerManager() != null) {
            Collection<CommandFunction<ServerCommandSource>> RECONFIG_FUNCTION = CMDHelper.getFunctions(server, ServerUtilsMod.CustomMapLoaderModule.getConfig().reconfigFunction);
            List<ServerPlayerEntity> players = server.getPlayerManager().getPlayerList();
            for (int i = players.size() - 1; i >= 0; i--) {
                CMDHelper.executeAs(players.get(i), RECONFIG_FUNCTION);
                PlayerJoinLocationMod.excludePlayer(players.get(i));
                players.get(i).networkHandler.reconfigure();
            }
        }
    }

    //todo fix
    public static void onEnterReconfig(ServerConfigurationNetworkHandler handler, ServerPlayerEntity player) {
        CombinedDynamicRegistries<ServerDynamicRegistryType> combinedDynamicRegistries = player.getServer().getCombinedDynamicRegistries();
        // handler.sendPacket(new DynamicRegistriesS2CPacket(new DynamicRegistryManager.ImmutableImpl(SerializableRegistries.streamDynamicEntries(combinedDynamicRegistries)).toImmutable()));
        handler.endConfiguration();
    }

    public static List<BattleMapAddon> getAllBattleMaps() {
        return BATTLE_MAPS.values().stream().sorted(Comparator.comparing((battleMapAddon -> battleMapAddon.addon_id.getPath()))).toList();
    }

    public static void serverTick(MinecraftServer server) {
        Iterator<Map.Entry<Identifier, LoadedBattleMapInstance>> it = LOADED_BATTLE_MAPS.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Identifier, LoadedBattleMapInstance> pair = it.next();
            LoadedBattleMapInstance instance = pair.getValue();

            if (instance.scheduleToRemove) {
                it.remove();
                continue;
            }

            if (instance.finishedLoading) {
                continue;
            }

            if (instance.runPreTriggerCondition(server))
                instance.executeDatapack(server);
        }
    }

    public static void battleLoad(MinecraftServer server, Identifier addon, Identifier dimID, boolean centralSpawnEnabled, Collection<ServerPlayerEntity> players, Collection<CommandFunction<ServerCommandSource>> functions) {
        BattleMapAddon config = CustomMapLoaderMod.BATTLE_MAPS.get(addon);
        MapSize mapSize = CustomMapLoaderMod.autoSelectMapSize(config, server.getCurrentPlayerCount());

        Path path = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(dimID.getNamespace()).resolve(dimID.getPath());
        IO.unZipMap(path, config.filePath, config.getDirectoryInZip(mapSize));

        DimensionLoaderMod.loadDimension(dimID, config.dimensionType_id,
                (server1, customDimHolder) -> {
                    LoadedBattleMapInstance instance = new LoadedBattleMapInstance(centralSpawnEnabled, mapSize, config, dimID);

                    battlePrepare(instance, players);
                    battleSpawn(instance, players);

                    instance.setPreTriggerDatapackCondition(server2 -> {
                        for (ServerPlayerEntity player : players) {
                            if (!SwitchableResourcepacksMod.allPacksLoaded(player))
                                return false;
                        }
                        return true;
                    });

                    instance.setDatapackFunctions(functions);

                    LOADED_BATTLE_MAPS.put(dimID, instance);
                });

        server.getPlayerManager().broadcast(Text.literal("Loading map: ").append(config.getNameText()), false);
        MessageSender.sendGameMessageWMentions(Text.literal("Loading map ").append(config.getNameText()));
    }

    private static void battlePrepare(LoadedBattleMapInstance instance, Collection<ServerPlayerEntity> players) {
        ServerWorld world = instance.getWorld();
        BattleMapAddon.MapSizeConfig sizedConfig = instance.getSizedAddon();
        ParsedPlayerCoords centerPos = parseVec3D(sizedConfig.center_coords);

        if (sizedConfig.world_border_coords_1 != null && sizedConfig.world_border_coords_2 != null)
            CustomWorldBorderMod.getCustomWorldBorderManager(world).setCustomWorldBorder(world, parseBlockPos(sizedConfig.world_border_coords_1), parseBlockPos(sizedConfig.world_border_coords_2));
        else
            CustomWorldBorderMod.getCustomWorldBorderManager(world).setCustomWorldBorder(world, new BlockPos(-10000, -10000, -10000), new BlockPos(10000, 10000, 10000));

        ChestTrackerMod.reset(world.getServer());
        for (String pos : sizedConfig.chest_tracker_coords) {
            ChestTrackerMod.trackedChests.add(parseBlockPos(pos));
        }

        DatapackInteractables.addToBlockList(instance.getWorld().getRegistryKey(), instance.getAddon().interactable_blocklist);
        DatapackInteractables.addToBlockList(instance.getWorld().getRegistryKey(), sizedConfig.interactable_blocklist);

        for (ServerPlayerEntity player : players) {
            loadResourcePacks(instance.getAddon(), player);

            Collection<ServerPlayerEntity> single = Collections.singleton(player);
            ParsedPlayerCoords playerPos = centerPos.fillPlayer(player);

            player.teleport(world, playerPos.x(), playerPos.y(), playerPos.z(), 0, 0);

            PlayerLockdownMod.executeLockdown(single, true);
            PlayerLockdownMod.executeFreeze(single, playerPos.pos, true);
        }
    }

    private static void battleSpawn(LoadedBattleMapInstance instance, Collection<ServerPlayerEntity> players) {
        ParsedPlayerCoords centerPos = parseVec3D(instance.getSizedAddon().center_coords);

        instance.setInitialSpawns(instance.isCentralSpawnEnabled());

        for (ServerPlayerEntity player : players) {
            battleTP(player, instance.getWorld(), centerPos, instance.getNextInitialSpawn(), instance, false, true);
        }
    }

    public static void battleTp(Identifier dimID, boolean initialSpawn, Collection<ServerPlayerEntity> players) {
        LoadedBattleMapInstance instance = LOADED_BATTLE_MAPS.get(dimID);

        ParsedPlayerCoords centerPos = parseVec3D(instance.getSizedAddon().center_coords);

        if (initialSpawn) {
            for (ServerPlayerEntity player : players) {
                battleTP(player, instance.getWorld(), centerPos, instance.getNextInitialSpawn(), instance, true, true);
            }
        } else {
            for (ServerPlayerEntity player : players) {
                battleTP(player, instance.getWorld(), centerPos, instance.getUnusedRandomSpawn(), instance, true, false);
            }
        }
    }

    private static void battleTP(ServerPlayerEntity player, ServerWorld world, ParsedPlayerCoords centerPos, String rawCoords, LoadedBattleMapInstance instance, boolean loadResources, boolean freezePlayer) {
        if (loadResources)
            loadResourcePacks(instance.getAddon(), player);

        if (SwitchableResourcepacksMod.isSafeMusicEnabled(player) && instance.getAddon().safe_music_pack != null) {
            SwitchableResourcepacksMod.setMusicPack(player, instance.getAddon().safe_music_pack);
        } else {
            SwitchableResourcepacksMod.setMusicPack(player, instance.getAddon().music_pack);
        }

        ParsedPlayerCoords playerPos = parseVec3D(rawCoords);

        float yaw = MathHelper.wrapDegrees((float) (MathHelper.atan2(centerPos.z() - playerPos.z(), centerPos.x() - playerPos.x()) * 57.2957763671875) - 90.0f);
        player.teleport(world, playerPos.x(), playerPos.y(), playerPos.z(), yaw, 0);

        if (freezePlayer)
            PlayerLockdownMod.executeFreeze(Collections.singleton(player), playerPos.pos, true);
    }

    public static void prepareLobby(MinecraftServer server, Identifier addon, Identifier dimID, Collection<ServerPlayerEntity> players, Collection<CommandFunction<ServerCommandSource>> functions) {
        LobbyMapAddon config = CustomMapLoaderMod.LOBBY_MAPS.get(addon);

        Path path = server.getSavePath(WorldSavePath.ROOT).resolve("dimensions").resolve(dimID.getNamespace()).resolve(dimID.getPath());
        IO.unZipMap(path, config.filePath, config.getDirectoryInZip());

        DimensionLoaderMod.loadDimension(dimID, config.dimensionType_id, (server1, customDimHolder) -> {
            DatapackInteractables.addToBlockList(customDimHolder.world.getRegistryKey(), config.interactable_blocklist);

            teleportToLobby(dimID, players, null);

            if (functions != null) {
                for (CommandFunction<ServerCommandSource> commandFunction : functions) {
                    server.getCommandFunctionManager().execute(commandFunction, server.getCommandSource().withLevel(2).withSilent());
                }
            }
        });

        LOADED_LOBBIES.put(dimID, config);
    }

    public static void teleportToLobby(Identifier dimID, Collection<ServerPlayerEntity> players, ServerPlayerEntity winner) {
        LobbyMapAddon config = LOADED_LOBBIES.get(dimID);
        CustomDimHolder holder = DimensionLoaderMod.loadedWorlds.get(dimID);
        Random random = new Random();

        List<String> availCoords = new ArrayList<>(Arrays.asList(config.spawn_coords));

        if (winner != null) {
            if (config.winner_coords != null) {
                lobbyTP(holder.world.asWorld(), winner, config.winner_coords.split(" "), config);
            } else {
                players.add(winner);
            }
        }

        for (ServerPlayerEntity player : players) {
            if (availCoords.isEmpty())
                availCoords = new ArrayList<>(Arrays.asList(config.spawn_coords));

            lobbyTP(holder.world.asWorld(), player, availCoords.remove(random.nextInt(availCoords.size())).split(" "), config);
        }
    }

    private static void lobbyTP(ServerWorld world, ServerPlayerEntity player, String[] coords, BaseMapAddon addon) {
        loadResourcePacks(addon, player);

        float yaw = player.getYaw();
        float pitch = player.getPitch();

        if (coords.length == 5) {
            yaw = Float.parseFloat(coords[3]);
            pitch = Float.parseFloat(coords[4]);
        }

        Vec3d pos = new Vec3d(Double.parseDouble(coords[0]), Double.parseDouble(coords[1]), Double.parseDouble(coords[2]));
        player.teleport(world, pos.x, pos.y, pos.z, yaw, pitch);
    }

    private static void loadResourcePacks(BaseMapAddon addon, ServerPlayerEntity player) {
        ResourcePackList list = new ResourcePackList();

        if (addon.required_packs != null && addon.required_packs.packs != null) {
            list.packs.addAll(addon.required_packs.packs);
        }

        if (addon.optional_packs != null && addon.optional_packs.packs != null) {
            for (ResourcePack pack : addon.optional_packs.packs) {
                if (HostOptions.getOverwriteValue(player, addon.addon_id)) {
                    if (HostOptions.getMapResourcePackValue(player, addon.addon_id, pack.packID))
                        list.packs.add(pack);
                } else if (HostOptions.getGlobalAcceptValue(player))
                    list.packs.add(pack);
            }
        }

        SwitchableResourcepacksMod.addPacks(list.packs, player);
    }

    public static void unloadLobbyMap(MinecraftServer server, Identifier dimID, Collection<CommandFunction<ServerCommandSource>> functions) {
        LOADED_LOBBIES.remove(dimID);

        DimensionLoaderMod.unLoadDimension(server, dimID, functions);
    }

    public static void unloadBattleMap(MinecraftServer server, Identifier dimID, Collection<CommandFunction<ServerCommandSource>> functions) {
        if (LOADED_BATTLE_MAPS.containsKey(dimID))
            LOADED_BATTLE_MAPS.get(dimID).scheduleToRemove = true;

        DimensionLoaderMod.unLoadDimension(server, dimID, functions);
    }

    private static BlockPos parseBlockPos(String coords) {
        String[] coords_split = coords.split(" ");
        return new BlockPos((int) Double.parseDouble(coords_split[0]), (int) Double.parseDouble(coords_split[1]), (int) Double.parseDouble(coords_split[2]));
    }

    private static ParsedPlayerCoords parseVec3D(String coords) {
        String[] coords_split = coords.split(" ");
        ParsedPlayerCoords playerCoords = new ParsedPlayerCoords(new Vec3d(Double.parseDouble(coords_split[0]), Double.parseDouble(coords_split[1]), Double.parseDouble(coords_split[2])));
        if (coords_split.length == 5) {
            playerCoords.fillYawPitch(Float.parseFloat(coords_split[3]), Float.parseFloat(coords_split[4]));
        }
        return playerCoords;
    }

    private static MapSize autoSelectMapSize(BattleMapAddon config, int playerCount) {
        switch (HostOptions.selectedMapSize) {
            case SMALL -> {
                if (config.hasSize(MapSize.SMALL))
                    return MapSize.SMALL;
            }
            case LARGE -> {
                if (config.hasSize(MapSize.LARGE))
                    return MapSize.LARGE;
            }
            case LARGE_PLUS -> {
                if (config.hasSize(MapSize.LARGE_PLUS))
                    return MapSize.LARGE_PLUS;
            }
            case REMASTERED -> {
                if (config.hasSize(MapSize.REMASTERED))
                    return MapSize.REMASTERED;
            }
        }

        //no size available, or Auto selected
        if (playerCount >= 0 && playerCount <= 6) {
            if (config.hasSize(MapSize.SMALL)) return MapSize.SMALL;
            if (config.hasSize(MapSize.LARGE)) return MapSize.LARGE;
            if (config.hasSize(MapSize.LARGE_PLUS)) return MapSize.LARGE_PLUS;
            if (config.hasSize(MapSize.REMASTERED)) return MapSize.REMASTERED;
        }
        if (playerCount >= 7 && playerCount <= 8) {
            if (config.hasSize(MapSize.LARGE)) return MapSize.LARGE;
            if (config.hasSize(MapSize.LARGE_PLUS)) return MapSize.LARGE_PLUS;
            if (config.hasSize(MapSize.SMALL)) return MapSize.SMALL;
            if (config.hasSize(MapSize.REMASTERED)) return MapSize.REMASTERED;
        }
        if (playerCount >= 9) {
            if (config.hasSize(MapSize.LARGE_PLUS)) return MapSize.LARGE_PLUS;
            if (config.hasSize(MapSize.LARGE)) return MapSize.LARGE;
            if (config.hasSize(MapSize.SMALL)) return MapSize.SMALL;
            if (config.hasSize(MapSize.REMASTERED)) return MapSize.REMASTERED;
        }

        return null;
    }

    public static final class ParsedPlayerCoords {
        private final Vec3d pos;
        private Float yaw;
        private Float pitch;

        public ParsedPlayerCoords(Vec3d pos, float yaw, float pitch) {
            this.pos = pos;
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public ParsedPlayerCoords(Vec3d pos) {
            this.pos = pos;
            this.yaw = null;
            this.pitch = null;
        }

        public ParsedPlayerCoords fillPlayer(ServerPlayerEntity player) {
            return new ParsedPlayerCoords(pos, yaw == null ? player.getYaw() : yaw, pitch == null ? player.getPitch() : pitch);
        }

        public void fillYawPitch(float yaw, float pitch) {
            this.yaw = yaw;
            this.pitch = pitch;
        }

        public Vec3d pos() {
            return pos;
        }

        public double x() {
            return pos.x;
        }

        public double y() {
            return pos.y;
        }

        public double z() {
            return pos.z;
        }

        public float yaw() {
            return yaw;
        }

        public float pitch() {
            return pitch;
        }
    }
}
