package net.kyrptonaught.serverutils.syncedKeybinds;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;

import java.util.HashMap;

public class SyncedKeybindsNetworking {

    public static void registerReceivePacket() {
        PayloadTypeRegistry.playC2S().register(KeybindPressPacket.PACKET_ID, KeybindPressPacket.codec);
        PayloadTypeRegistry.playS2C().register(SyncKeybindsPacket.PACKET_ID, SyncKeybindsPacket.codec);

        ServerPlayNetworking.registerGlobalReceiver(KeybindPressPacket.PACKET_ID, (payload, context) -> {
            context.player().getServer().execute(() -> {
                SyncedKeybinds.keybindPressed(context.player(), payload.keybind());

            });
        });
    }

    public static void syncKeybindsToClient(HashMap<Identifier, SyncedKeybindsConfig.KeybindConfigItem> keybinds, PacketSender packetSender) {
        packetSender.sendPacket(new SyncKeybindsPacket(keybinds));
    }
}
