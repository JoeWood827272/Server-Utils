package net.kyrptonaught.serverutils.customWorldBorder;

import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

public record CustomWorldBorderPacket(double xCenter, double zCenter, double xSize,
                                      double zSize) implements CustomPayload {
    public static final CustomPayload.Id<CustomWorldBorderPacket> PACKET_ID = new CustomPayload.Id<>(new Identifier(ServerUtilsMod.CustomWorldBorder.getMOD_ID(), "customborder"));
    public static final PacketCodec<RegistryByteBuf, CustomWorldBorderPacket> codec = PacketCodec.of(CustomWorldBorderPacket::write, CustomWorldBorderPacket::read);

    public static CustomWorldBorderPacket read(RegistryByteBuf buf) {
        return new CustomWorldBorderPacket(buf.readDouble(), buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public void write(RegistryByteBuf buf) {
        buf.writeDouble(xCenter);
        buf.writeDouble(zCenter);
        buf.writeDouble(xSize);
        buf.writeDouble(zSize);
    }

    @Override
    public Id<? extends CustomPayload> getId() {
        return PACKET_ID;
    }
}