package net.kyrptonaught.serverutils.mixin.scoreboardplayerinfo;

import net.kyrptonaught.serverutils.scoreboardPlayerInfo.ScoreboardPlayerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.handshake.ConnectionIntent;
import net.minecraft.network.packet.c2s.handshake.HandshakeC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerHandshakeNetworkHandler;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerHandshakeNetworkHandler.class)
public abstract class ServerHandshakeNetworkHandlerMixin {
    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private ClientConnection connection;

    @Inject(method = "onHandshake", at = @At("HEAD"))
    public void getProtocolVersion(HandshakeC2SPacket packet, CallbackInfo ci) {
        server.execute(() -> {
            if (packet.intendedState() == ConnectionIntent.LOGIN) {
                int protocol = packet.protocolVersion();
                ScoreboardPlayerInfo.addClientConnectionProtocol(connection, protocol);
            }
        });
    }
}
