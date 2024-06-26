package net.kyrptonaught.serverutils.syncedKeybinds;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import java.util.HashMap;

import static net.kyrptonaught.serverutils.ServerUtilsMod.SyncedKeybindsModule;

public record SyncKeybindsPacket(
        HashMap<Identifier, SyncedKeybindsConfig.KeybindConfigItem> keybinds) implements CustomPayload {
    public static final CustomPayload.Id<SyncKeybindsPacket> PACKET_ID = new CustomPayload.Id<>(Identifier.of(SyncedKeybindsModule.getMOD_ID(), "keybind_pressed_packet"));
    public static final PacketCodec<RegistryByteBuf, SyncKeybindsPacket> codec = PacketCodec.of(SyncKeybindsPacket::write, SyncKeybindsPacket::read);

    public static SyncKeybindsPacket read(RegistryByteBuf buf) {
        //return new SyncKeybindsPacket(buf.readDouble() ,buf.readDouble(),buf.readDouble(), buf.readDouble());
        return null;
    }

    public void write(RegistryByteBuf buf) {
        buf.writeInt(keybinds.size());
        keybinds.forEach((s, keybindConfigItem) -> keybindConfigItem.writeToPacket(s, buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}