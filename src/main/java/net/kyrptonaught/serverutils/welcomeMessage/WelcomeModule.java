package net.kyrptonaught.serverutils.welcomeMessage;

import net.kyrptonaught.serverutils.CMDHelper;
import net.kyrptonaught.serverutils.ModuleWConfig;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;

import java.util.HashSet;
import java.util.UUID;

public class WelcomeModule extends ModuleWConfig<WelcomeMessageConfig> {

    private static final HashSet<UUID> playerMsgSent = new HashSet<>();

    public static void trySendWelcomeMessage(MinecraftServer server, ServerPlayerEntity player) {
        if (playerMsgSent.contains(player.getUuid()))
            return;
        WelcomeMessageConfig config = ServerUtilsMod.WelcomeMessageModule.getConfig();
        CMDHelper.executeFunctionsAs(player, config.function);
        playerMsgSent.add(player.getUuid());
    }


    @Override
    public WelcomeMessageConfig createDefaultConfig() {
        return new WelcomeMessageConfig();
    }
}
