package net.kyrptonaught.serverutils.takeEverything;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public class TakeEverythingNetworking {

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playC2S().register(TakeEverythingPacket.PACKET_ID, TakeEverythingPacket.codec);

        ServerPlayNetworking.registerGlobalReceiver(TakeEverythingPacket.PACKET_ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                TakeEverythingHelper.takeEverything(context.player());
            });
        });
    }
}
