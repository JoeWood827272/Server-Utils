package net.kyrptonaught.serverutils;


import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record ServerUtilsPresencePacket(boolean enabled) implements CustomPayload {
    public static final CustomPayload.Id<ServerUtilsPresencePacket> PACKET_ID = new CustomPayload.Id<>(new Identifier(ServerUtilsMod.MOD_ID, "presence"));
    public static final PacketCodec<RegistryByteBuf, ServerUtilsPresencePacket> codec = PacketCodecs.BOOL.xmap(ServerUtilsPresencePacket::new, ServerUtilsPresencePacket::enabled).cast();

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}

