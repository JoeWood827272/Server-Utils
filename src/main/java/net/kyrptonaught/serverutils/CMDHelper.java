package net.kyrptonaught.serverutils;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

public class CMDHelper {

    public static void executeAs(PlayerEntity player, String cmd) {
        player.getServer().getCommandManager().executeWithPrefix(player.getCommandSource().withLevel(2).withSilent(), cmd);
    }

    public static void executeAs(PlayerEntity player, Collection<CommandFunction<ServerCommandSource>> functions) {
        for (CommandFunction<ServerCommandSource> commandFunction : functions) {
            player.getServer().getCommandFunctionManager().execute(commandFunction, player.getCommandSource().withLevel(2).withSilent());
        }
    }

    public static void executeFunctionsAs(PlayerEntity player, String id) {
        Collection<CommandFunction<ServerCommandSource>> functions = getFunctions(player.getServer(), id);
        if (functions != null)
            for (CommandFunction<ServerCommandSource> commandFunction : functions) {
                player.getServer().getCommandFunctionManager().execute(commandFunction, player.getCommandSource().withLevel(2).withSilent());
            }
    }

    public static Supplier<Text> getFeedbackLiteral(String text) {
        return getFeedback(Text.literal(text));
    }

    public static Supplier<Text> getFeedbackTranslatable(String text) {
        return getFeedback(Text.translatable(text));
    }

    public static Supplier<Text> getFeedback(Text text) {
        return () -> text;
    }

    public static Collection<CommandFunction<ServerCommandSource>> getFunctions(MinecraftServer server, String id) {
        if (id.startsWith("#")) {
            return server.getCommandFunctionManager().getTag(Identifier.of(id.replaceAll("#", "")));
        }

        Optional<CommandFunction<ServerCommandSource>> function = server.getCommandFunctionManager().getFunction(Identifier.of(id));
        if (function.isPresent()) return Collections.singleton(function.get());

        return Collections.emptySet();
    }
}
