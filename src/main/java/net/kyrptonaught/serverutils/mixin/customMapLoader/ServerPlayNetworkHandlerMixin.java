package net.kyrptonaught.serverutils.mixin.customMapLoader;

import net.kyrptonaught.serverutils.customMapLoader.CustomMapLoaderMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.AcknowledgeReconfigurationC2SPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayNetworkHandler.class)
public abstract class ServerPlayNetworkHandlerMixin extends ServerCommonNetworkHandler {

    @Shadow
    public ServerPlayerEntity player;

    public ServerPlayNetworkHandlerMixin(MinecraftServer server, ClientConnection connection, ConnectedClientData clientData) {
        super(server, connection, clientData);
    }

    @Inject(method = "onAcknowledgeReconfiguration", at = @At("TAIL"))
    public void grabReconfig(AcknowledgeReconfigurationC2SPacket packet, CallbackInfo ci) {
        CustomMapLoaderMod.onEnterReconfig((ServerConfigurationNetworkHandler) this.connection.getPacketListener(), this.player);
    }
}
