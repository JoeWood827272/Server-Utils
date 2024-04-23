package net.kyrptonaught.serverutils.armorHudToggle;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.kyrptonaught.serverutils.Module;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.Collection;

public class ArmorHudMod extends Module {

    @Override
    public void onInitialize() {
        super.onInitialize();
        PayloadTypeRegistry.playS2C().register(ArmorHudPacket.PACKET_ID, ArmorHudPacket.codec);
    }

    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("armorHud")
                .requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("entity", EntityArgumentType.players()).then(CommandManager.argument("state", BoolArgumentType.bool())
                        .executes(context -> {
                            Collection<ServerPlayerEntity> players = EntityArgumentType.getPlayers(context, "entity");
                            boolean state = BoolArgumentType.getBool(context, "state");

                            if (players != null)
                                for (ServerPlayerEntity player : players) {
                                    ServerPlayNetworking.send(player, new ArmorHudPacket(state));
                                }
                            return 1;
                        }))));
    }
}