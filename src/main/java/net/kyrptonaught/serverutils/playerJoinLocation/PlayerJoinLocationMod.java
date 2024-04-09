package net.kyrptonaught.serverutils.playerJoinLocation;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.kyrptonaught.serverutils.ModuleWConfig;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.UUID;

public class PlayerJoinLocationMod extends ModuleWConfig<PlayerJoinLocationConfig> {
    public static boolean ENABLED = false;

    private static final HashSet<UUID> EXCLUDED_PLAYERS = new HashSet<>();

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("playerJoinLocation")
                .requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("enable").then(CommandManager.argument("enable", BoolArgumentType.bool())
                        .executes(context -> {
                            ENABLED = BoolArgumentType.getBool(context, "enable");
                            return 1;
                        }))));
    }

    public static void excludePlayer(ServerPlayerEntity player) {
        EXCLUDED_PLAYERS.add(player.getUuid());
    }

    public static void removePlayer(ServerPlayerEntity player) {
        EXCLUDED_PLAYERS.remove(player.getUuid());
    }

    public static boolean isExcludedPlayer(ServerPlayerEntity player) {
        return EXCLUDED_PLAYERS.contains(player.getUuid());
    }

    @Override
    public PlayerJoinLocationConfig createDefaultConfig() {
        return new PlayerJoinLocationConfig();
    }
}
