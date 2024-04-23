package net.kyrptonaught.serverutils.personatus;

import io.netty.buffer.Unpooled;
import net.kyrptonaught.serverutils.mixin.personatus.PlayerListS2CPacketAccessor;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket;
import net.minecraft.network.packet.s2c.play.TeamS2CPacket;
import net.minecraft.registry.DynamicRegistryManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

public class PacketCopier {

    public static PlayerListS2CPacket copyPlayerList(PlayerListS2CPacket oldListPacket) {
        PlayerListS2CPacket listPacket = new PlayerListS2CPacket(oldListPacket.getActions(), Collections.emptyList());
        ((PlayerListS2CPacketAccessor) listPacket).setEntries(new ArrayList<>(oldListPacket.getEntries()));
        return listPacket;
    }


    public static TeamS2CPacket copyTeamPacket(TeamS2CPacket oldTeamPacket) {
        RegistryByteBuf buf = new RegistryByteBuf(Unpooled.buffer(), DynamicRegistryManager.EMPTY);
        TeamS2CPacket.CODEC.encode(buf, oldTeamPacket);
        return TeamS2CPacket.CODEC.decode(buf);
    }

    public static ChatMessageS2CPacket copyChatPacket(ChatMessageS2CPacket oldChatPacket, UUID newUUID) {
        return new ChatMessageS2CPacket(newUUID, oldChatPacket.index(), oldChatPacket.signature(), oldChatPacket.body(), oldChatPacket.unsignedContent(), oldChatPacket.filterMask(), oldChatPacket.serializedParameters());
    }
}
