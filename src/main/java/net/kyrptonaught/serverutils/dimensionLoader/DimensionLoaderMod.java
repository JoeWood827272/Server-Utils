package net.kyrptonaught.serverutils.dimensionLoader;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.kyrptonaught.serverutils.FileHelper;
import net.kyrptonaught.serverutils.Module;
import net.kyrptonaught.serverutils.customWorldBorder.CustomWorldBorderMod;
import net.kyrptonaught.serverutils.datapackInteractables.DatapackInteractables;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage;
import xyz.nucleoid.fantasy.Fantasy;
import xyz.nucleoid.fantasy.RuntimeWorldConfig;
import xyz.nucleoid.fantasy.mixin.MinecraftServerAccess;
import xyz.nucleoid.fantasy.util.VoidChunkGenerator;

import java.nio.file.Path;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.BiConsumer;

public class DimensionLoaderMod extends Module {
    public static final HashMap<Identifier, CustomDimHolder> loadedWorlds = new HashMap<>();

    @Override
    public void onInitialize() {
        ServerTickEvents.START_SERVER_TICK.register(DimensionLoaderMod::serverTickWorldAdd);
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        DimensionLoaderCommand.registerCommands(dispatcher);
    }

    public static void loadDimension(Identifier id, Identifier dimID, BiConsumer<MinecraftServer, CustomDimHolder> onComplete) {
        loadedWorlds.put(id, new CustomDimHolder(id, dimID).setCompleteTask(onComplete));
    }

    public static Text loadDimension(MinecraftServer server, Identifier id, Identifier dimID, Collection<CommandFunction<ServerCommandSource>> functions) {
        if (loadedWorlds.containsKey(id)) {
            return Text.literal("Dim already registered");
        }

        DimensionType dimensionType = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).get(dimID);
        if (dimensionType == null) {
            return Text.literal("No Dimension Type found");
        }

        if (!backupArenaMap(server, id, dimID)) {
            return Text.literal("Failed creating temp directory");
        }

        loadedWorlds.put(id, new CustomDimHolder(id, dimID).setCompleteTask(functions));
        return Text.literal("Preparing Dimension");
    }

    public static Text unLoadDimension(MinecraftServer server, Identifier id, Collection<CommandFunction<ServerCommandSource>> functions) {
        CustomDimHolder holder = loadedWorlds.get(id);
        if (holder == null)
            return Text.literal("Dimension not found");

        holder.setCompleteTask(functions);
        holder.scheduleToDelete();
        return Text.literal("Unloading Dimension");
    }

    public static Text whereAmI(ServerPlayerEntity player) {
        Identifier dimID = player.getWorld().getRegistryKey().getValue();
        if (loadedWorlds.containsKey(dimID))
            dimID = loadedWorlds.get(dimID).copyFromID;
        return Text.translatable("key.world." + dimID.toTranslationKey());
    }

    public static RegistryKey<World> tryGetWorldKey(ServerWorld world) {
        RegistryKey<World> ogKey = world.getRegistryKey();
        if (loadedWorlds.containsKey(ogKey.getValue()))
            return RegistryKey.of(RegistryKeys.WORLD, loadedWorlds.get(ogKey.getValue()).copyFromID);
        return ogKey;
    }

    public static RegistryEntry<DimensionType> tryGetDimKey(ServerWorld world) {
        RegistryKey<World> ogKey = world.getRegistryKey();
        if (loadedWorlds.containsKey(ogKey.getValue())) {
            Registry<DimensionType> registry = world.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
            return registry.getEntry(registry.getKey(registry.get(loadedWorlds.get(ogKey.getValue()).copyFromID)).get()).get();
        }
        return world.getDimensionEntry();
    }

    public static void serverTickWorldAdd(MinecraftServer server) {
        Fantasy fantasy = Fantasy.get(server);
        Iterator<Map.Entry<Identifier, CustomDimHolder>> it = loadedWorlds.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Identifier, CustomDimHolder> pair = it.next();
            CustomDimHolder holder = pair.getValue();

            if (holder.scheduledDelete()) {
                if (holder.deleteFinished(fantasy)) {
                    holder.executeComplete(server);
                    CustomWorldBorderMod.onDimensionUnload(holder.world.asWorld());
                    DatapackInteractables.unloadWorld(holder.world.getRegistryKey());
                    it.remove();
                }
            } else if (!holder.wasRegistered()) {
                Registry<DimensionType> registry = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE);
                RegistryEntry<DimensionType> entry = registry.getEntry(registry.getKey(registry.get(holder.copyFromID)).get()).get();

                RuntimeWorldConfig worldConfig = new RuntimeWorldConfig()
                        .setDimensionType(entry)
                        .setMirrorOverworldGameRules(true)
                        .setGenerator(new VoidChunkGenerator(server.getRegistryManager().get(RegistryKeys.BIOME).entryOf(BiomeKeys.THE_VOID)));

                holder.register(fantasy.openTemporaryWorld(holder.dimID, worldConfig));
                holder.executeComplete(server);
            }
        }
    }

    public static boolean backupArenaMap(MinecraftServer server, Identifier id, Identifier newWorld) {
        LevelStorage.Session session = ((MinecraftServerAccess) server).getSession();

        Path worldDir = session.getDirectory(WorldSavePath.ROOT);
        Path arenaDir = getWorldDir(worldDir, id);
        Path newArenaDir = getWorldDir(worldDir, newWorld);

        if (!FileHelper.deleteDir(arenaDir)) return false;
        if (!FileHelper.createDir(arenaDir)) return false;
        return FileHelper.copyDirectory(newArenaDir, arenaDir);
    }

    public static Path getWorldDir(Path worldDirectory, Identifier world) {
        return worldDirectory.resolve("dimensions").resolve(world.getNamespace()).resolve(world.getPath());
    }
}


