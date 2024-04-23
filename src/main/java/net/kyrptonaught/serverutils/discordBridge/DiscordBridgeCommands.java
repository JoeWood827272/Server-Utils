package net.kyrptonaught.serverutils.discordBridge;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.kyrptonaught.serverutils.CMDHelper;
import net.kyrptonaught.serverutils.discordBridge.bot.BotCommands;
import net.kyrptonaught.serverutils.discordBridge.linking.LinkingManager;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.TextArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;

public class DiscordBridgeCommands {

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(CommandManager.literal("discordLink").executes(context -> {
            ServerPlayerEntity player = context.getSource().getPlayer();
            if (player != null)
                LinkingManager.beginLink(player);
            return 1;
        }));

        dispatcher.register(CommandManager.literal("discordMSG").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("msg", TextArgumentType.text(registryAccess))
                        .executes(context -> {
                            Text text = TextArgumentType.getTextArgument(context, "msg");
                            MessageSender.sendGameMessageWMentions(text);
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("discordChatMSG").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("msg", TextArgumentType.text(registryAccess))
                        .executes(context -> {
                            Text text = TextArgumentType.getTextArgument(context, "msg");
                            MessageSender.sendGameMessageWMentions(text);
                            context.getSource().getServer().getPlayerManager().broadcast(Texts.parse(context.getSource(), text, null, 0), false);
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("sus").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            String mcname = StringArgumentType.getString(context, "player");
                            BotCommands.susCommandExecute(mcname, (result) -> context.getSource().sendFeedback(CMDHelper.getFeedbackLiteral(result), false));
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("unsus").requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.argument("player", StringArgumentType.word())
                        .executes(context -> {
                            String mcname = StringArgumentType.getString(context, "player");
                            BotCommands.unsusCommandExecute(mcname, (result) -> context.getSource().sendFeedback(CMDHelper.getFeedbackLiteral(result), false));
                            return 1;
                        })));
    }
}
