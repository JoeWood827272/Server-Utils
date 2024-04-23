package net.kyrptonaught.serverutils.SpectateSqueaker;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class SpectateSqueakereNetworking {

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playC2S().register(SqueakPacket.PACKET_ID, SqueakPacket.codec);

        ServerPlayNetworking.registerGlobalReceiver(SqueakPacket.PACKET_ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                SpectateSqueakerMod.playerSqueaks(context.player());

            });
        });
    }
}

