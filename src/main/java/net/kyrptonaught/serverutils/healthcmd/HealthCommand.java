package net.kyrptonaught.serverutils.healthcmd;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.ScoreboardObjectiveArgumentType;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;


public class HealthCommand {
    public static final DamageSource[] damageSources = new DamageSource[]{
            DamageSource.IN_FIRE, DamageSource.LIGHTNING_BOLT, DamageSource.ON_FIRE, DamageSource.LAVA, DamageSource.HOT_FLOOR, DamageSource.IN_WALL,
            DamageSource.CRAMMING, DamageSource.DROWN, DamageSource.STARVE, DamageSource.CACTUS, DamageSource.FALL, DamageSource.FLY_INTO_WALL,
            DamageSource.OUT_OF_WORLD, DamageSource.GENERIC, DamageSource.MAGIC, DamageSource.WITHER, DamageSource.ANVIL, DamageSource.FALLING_BLOCK,
            DamageSource.DRAGON_BREATH, DamageSource.DRYOUT, DamageSource.SWEET_BERRY_BUSH, DamageSource.FREEZE, DamageSource.FALLING_STALACTITE,
            DamageSource.STALAGMITE};

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        LiteralArgumentBuilder<ServerCommandSource> healthCMD = CommandManager.literal("health")
                .requires((source) -> source.hasPermissionLevel(2));

        dispatcher.register(healthCMD.then(CommandManager.argument("entity", EntityArgumentType.entities())
                .then(CommandManager.literal("add")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                .executes((commandContext) -> execute(commandContext, HealthCMDMod.ModType.ADD, null)))
                        .then(CommandManager.literal("scoreboard").then(CommandManager.argument("obj", ScoreboardObjectiveArgumentType.scoreboardObjective())
                                .executes(context -> getScoreboard(context, HealthCMDMod.ModType.ADD, null))))
                )));

        dispatcher.register(healthCMD.then(CommandManager.argument("entity", EntityArgumentType.entities())
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                .executes((commandContext) -> execute(commandContext, HealthCMDMod.ModType.SET, null)))
                        .then(CommandManager.literal("scoreboard").then(CommandManager.argument("obj", ScoreboardObjectiveArgumentType.scoreboardObjective())
                                .executes(context -> getScoreboard(context, HealthCMDMod.ModType.SET, null))))
                )));

        for (DamageSource source : damageSources) {
            dispatcher.register(healthCMD.then(CommandManager.argument("entity", EntityArgumentType.entities())
                    .then(CommandManager.literal("remove")
                            .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                    .then(CommandManager.literal(source.name)
                                            .executes((commandContext) -> execute(commandContext, HealthCMDMod.ModType.SUB, source))))
                            .then(CommandManager.literal("scoreboard").then(CommandManager.argument("obj", ScoreboardObjectiveArgumentType.scoreboardObjective())
                                    .then(CommandManager.literal(source.name)
                                            .executes(context -> getScoreboard(context, HealthCMDMod.ModType.SUB, source)))))
                    )));
        }
        dispatcher.register(healthCMD.then(CommandManager.argument("entity", EntityArgumentType.entities())
                .then(CommandManager.literal("remove")
                        .then(CommandManager.argument("amount", FloatArgumentType.floatArg())
                                .then(CommandManager.literal("custom")
                                        .then(CommandManager.argument("deathMessage", StringArgumentType.string())
                                                .executes((commandContext) -> execute(commandContext, HealthCMDMod.ModType.SUB, null)))))
                        .then(CommandManager.literal("scoreboard").then(CommandManager.argument("obj", ScoreboardObjectiveArgumentType.scoreboardObjective())
                                .then(CommandManager.literal("custom")
                                        .then(CommandManager.argument("deathMessage", StringArgumentType.string())
                                                .executes(context -> getScoreboard(context, HealthCMDMod.ModType.SUB, null))))))
                )));
    }

    private static int getScoreboard(CommandContext<ServerCommandSource> commandContext, HealthCMDMod.ModType modType, DamageSource dmgSource) throws CommandSyntaxException {
        ScoreboardObjective obj = ScoreboardObjectiveArgumentType.getObjective(commandContext, "obj");
        if (modType == HealthCMDMod.ModType.SUB && dmgSource == null)
            dmgSource = new CustomDeathMessage(StringArgumentType.getString(commandContext, "deathMessage"));

        for (PlayerEntity player : EntityArgumentType.getPlayers(commandContext, "entity")) {
            int amount = commandContext.getSource().getServer().getScoreboard().getPlayerScore(player.getEntityName(), obj).getScore();
            execute(player, amount, modType, dmgSource);
        }
        return 1;
    }

    private static int execute(CommandContext<ServerCommandSource> commandContext, HealthCMDMod.ModType modType, DamageSource dmgSource) throws CommandSyntaxException {
        float amount = FloatArgumentType.getFloat(commandContext, "amount");
        if (modType == HealthCMDMod.ModType.SUB && dmgSource == null)
            dmgSource = new CustomDeathMessage(StringArgumentType.getString(commandContext, "deathMessage"));

        for (Entity entity : EntityArgumentType.getEntities(commandContext, "entity")) {
            execute(entity, amount, modType, dmgSource);
        }
        return 1;
    }

    private static void execute(Entity entity, float amount, HealthCMDMod.ModType modType, DamageSource dmgSource) {
        if (modType == HealthCMDMod.ModType.ADD) {
            if (entity instanceof LivingEntity livingEntity) {
                livingEntity.heal(amount);
                if (entity instanceof ServerPlayerEntity player)
                    player.markHealthDirty();
            }

        } else if (modType == HealthCMDMod.ModType.SET) {
            if (entity instanceof LivingEntity livingEntity)
                livingEntity.setHealth(amount);

        } else if (modType == HealthCMDMod.ModType.SUB) {
            entity.damage(dmgSource, amount);
        }
    }
}