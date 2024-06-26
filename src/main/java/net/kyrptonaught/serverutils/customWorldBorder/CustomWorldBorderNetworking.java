package net.kyrptonaught.serverutils.customWorldBorder;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class CustomWorldBorderNetworking {

    public static void sendCustomWorldBorderPacket(ServerPlayerEntity player, double xCenter, double zCenter, double xSize, double zSize) {
        ServerPlayNetworking.send(player, new CustomWorldBorderPacket(xCenter, zCenter, xSize, zSize));
    }

    @Environment(EnvType.CLIENT)
    public static void registerReceive() {

    }
}