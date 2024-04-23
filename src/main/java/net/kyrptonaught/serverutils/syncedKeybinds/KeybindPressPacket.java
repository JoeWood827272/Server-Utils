package net.kyrptonaught.serverutils.syncedKeybinds;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

import static net.kyrptonaught.serverutils.ServerUtilsMod.SyncedKeybindsModule;

public record KeybindPressPacket(Identifier keybind) implements CustomPayload {
    public static final CustomPayload.Id<KeybindPressPacket> PACKET_ID = new CustomPayload.Id<>(new Identifier(SyncedKeybindsModule.getMOD_ID(), "sync_keybinds_packet"));
    public static final PacketCodec<RegistryByteBuf, KeybindPressPacket> codec = Identifier.PACKET_CODEC.xmap(KeybindPressPacket::new, KeybindPressPacket::keybind).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}
