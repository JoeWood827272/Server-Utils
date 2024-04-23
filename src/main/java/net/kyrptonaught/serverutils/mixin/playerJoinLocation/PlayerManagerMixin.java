package net.kyrptonaught.serverutils.mixin.playerJoinLocation;

import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.playerJoinLocation.PlayerJoinLocationConfig;
import net.kyrptonaught.serverutils.playerJoinLocation.PlayerJoinLocationMod;
import net.minecraft.network.ClientConnection;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Redirect(method = "onPlayerConnect", at = @At(target = "Lnet/minecraft/server/MinecraftServer;getWorld(Lnet/minecraft/registry/RegistryKey;)Lnet/minecraft/server/world/ServerWorld;", value = "INVOKE"))
    public @Nullable ServerWorld forceSpawnLocation(MinecraftServer instance, RegistryKey<World> key, ClientConnection connection, ServerPlayerEntity player) {
        if (PlayerJoinLocationMod.ENABLED && !PlayerJoinLocationMod.isExcludedPlayer(player)) {
            PlayerJoinLocationMod.removePlayer(player);
            PlayerJoinLocationConfig config = ServerUtilsMod.playerJoinLocationMod.getConfig();
            player.setPos(config.posX, config.posY, config.posZ);
            return instance.getWorld(World.OVERWORLD);
        }
        return instance.getWorld(key);
    }
}
