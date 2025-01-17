package net.kyrptonaught.serverutils.switchableresourcepacks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.kyrptonaught.serverutils.CMDHelper;
import net.kyrptonaught.serverutils.ModuleWConfig;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.switchableresourcepacks.status.PackStatus;
import net.kyrptonaught.serverutils.switchableresourcepacks.status.PlayerStatus;
import net.kyrptonaught.serverutils.userConfig.UserConfigStorage;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.network.packet.s2c.common.ResourcePackRemoveS2CPacket;
import net.minecraft.network.packet.s2c.common.ResourcePackSendS2CPacket;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.function.CommandFunction;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.nio.charset.StandardCharsets;
import java.util.*;

public class SwitchableResourcepacksMod extends ModuleWConfig<ResourcePackConfig> {
    public static final HashMap<Identifier, ResourcePack> ResourcePacks = new HashMap<>();
    public static final HashMap<Identifier, MusicPack> MusicPacks = new HashMap<>();

    private static final HashMap<UUID, PlayerStatus> playerLoaded = new HashMap<>();

    private static Collection<CommandFunction<ServerCommandSource>> RP_FAILED_FUNCTIONS, RP_LOADED_FUNCTIONS, RP_STARTED_FUNCTIONS;
    private static final Identifier CUSTOMPACKID = Identifier.of(ServerUtilsMod.MOD_ID, "srp_custompack");
    private static final Identifier SAFEMUSICID = Identifier.of(ServerUtilsMod.MOD_ID, "srp_safemusic");
    private static final UUID CUSTOMPACKUUID = UUID.nameUUIDFromBytes(CUSTOMPACKID.toString().getBytes(StandardCharsets.UTF_8));

    public void onConfigLoad(ResourcePackConfig config) {
        RP_FAILED_FUNCTIONS = null;
        RP_LOADED_FUNCTIONS = null;
        ResourcePacks.clear();
        MusicPacks.clear();
        config.packs.forEach(pack -> ResourcePacks.put(pack.packID, pack));
        config.musicPacks.forEach(pack -> MusicPacks.put(pack.packID, pack));
    }

    @Override
    public void onInitialize() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> playerLoaded.remove(handler.getPlayer().getUuid()));
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            long currentTime = System.currentTimeMillis();
            server.getPlayerManager().getPlayerList().forEach(player -> getPlayerStatus(player).tick(player, currentTime));
        });
    }

    @Override
    public ResourcePackConfig createDefaultConfig() {
        return new ResourcePackConfig();
    }

    public static boolean allPacksLoaded(ServerPlayerEntity player) {
        PlayerStatus packs = getPlayerStatus(player);
        for (UUID pack : packs.getPacks().keySet()) {
            if (!packs.isComplete(pack))
                return false;
        }

        return true;
    }

    public static boolean didPackFail(ServerPlayerEntity player) {
        PlayerStatus packs = getPlayerStatus(player);
        for (UUID pack : packs.getPacks().keySet()) {
            if (packs.didFail(pack))
                return true;
        }

        return false;
    }

    public static void packStatusUpdate(ServerPlayerEntity player, UUID packname, PackStatus.LoadingStatus status) {
        getPlayerStatus(player).setPackLoadStatus(packname, status);

        if (allPacksLoaded(player)) {
            if (RP_LOADED_FUNCTIONS == null)
                RP_LOADED_FUNCTIONS = CMDHelper.getFunctions(player.getServer(), ServerUtilsMod.SwitchableResourcepacksModule.getConfig().playerCompleteFunction);
            if (RP_FAILED_FUNCTIONS == null)
                RP_FAILED_FUNCTIONS = CMDHelper.getFunctions(player.getServer(), ServerUtilsMod.SwitchableResourcepacksModule.getConfig().playerFailedFunction);

            if (didPackFail(player))
                CMDHelper.executeAs(player, RP_FAILED_FUNCTIONS);
            else
                CMDHelper.executeAs(player, RP_LOADED_FUNCTIONS);
        }
    }

    private static void addPackStatus(ServerPlayerEntity player, UUID packname, boolean temp) {
        getPlayerStatus(player).addPack(packname, temp);
    }

    public static PlayerStatus getPlayerStatus(ServerPlayerEntity player) {
        if (!playerLoaded.containsKey(player.getUuid()))
            playerLoaded.put(player.getUuid(), new PlayerStatus());

        return playerLoaded.get(player.getUuid());
    }

    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("loadresource")
                .requires((source) -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("packid", IdentifierArgumentType.identifier())
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            Identifier packID = IdentifierArgumentType.getIdentifier(context, "packid");
                            ResourcePack pack = ResourcePacks.get(packID);
                            if (pack == null) {
                                context.getSource().sendFeedback(CMDHelper.getFeedbackLiteral("Packname: " + packID + " was not found"), false);
                            } else {
                                execute(pack, player);
                                context.getSource().sendFeedback(CMDHelper.getFeedback(Text.literal("Enabled pack: ").append(pack.getNameText())), false);
                            }
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("custompack")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            ServerPlayerEntity player = context.getSource().getPlayer();

                            UserConfigStorage.setValue(player, CUSTOMPACKID, String.valueOf(enabled));
                            UserConfigStorage.syncPlayer(player);

                            getPlayerStatus(player).getPacks().remove(CUSTOMPACKUUID);

                            player.sendMessage(Text.translatable(enabled ? "lem.config.custompack.enable" : "lem.config.custompack.disable"));
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("safemusic")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.argument("enabled", BoolArgumentType.bool())
                        .executes(context -> {
                            boolean enabled = BoolArgumentType.getBool(context, "enabled");
                            ServerPlayerEntity player = context.getSource().getPlayer();

                            UserConfigStorage.setValue(player, SAFEMUSICID, String.valueOf(enabled));
                            UserConfigStorage.syncPlayer(player);

                            player.sendMessage(Text.translatable(enabled ? "lem.config.safemusic.enable" : "lem.config.safemusic.disable"));
                            return 1;
                        })));

        dispatcher.register(CommandManager.literal("music")
                .requires(source -> source.hasPermissionLevel(0))
                .then(CommandManager.literal("play")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            getPlayerStatus(player).startMusic();
                            return 1;
                        }))
                .then(CommandManager.literal("set")
                        .then(CommandManager.argument("packid", IdentifierArgumentType.identifier())
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    Identifier packid = IdentifierArgumentType.getIdentifier(context, "packid");

                                    setMusicPack(player, MusicPacks.get(packid));
                                    return 1;
                                })))
                .then(CommandManager.literal("skip")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            getPlayerStatus(player).skipSong(player);
                            return 1;
                        }))
                .then(CommandManager.literal("stop")
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            getPlayerStatus(player).stopMusic();
                            return 1;
                        })));
    }

    public static boolean isCustomPackEnabled(ServerPlayerEntity player) {
        return Boolean.parseBoolean(UserConfigStorage.getValue(player, CUSTOMPACKID));
    }

    public static boolean isSafeMusicEnabled(ServerPlayerEntity player) {
        return Boolean.parseBoolean(UserConfigStorage.getValue(player, SAFEMUSICID));
    }

    public static void setMusicPack(ServerPlayerEntity player, MusicPack pack) {
        getPlayerStatus(player).setMusic(pack);
    }

    private void execute(ResourcePack pack, ServerPlayerEntity player) {
        if (RP_STARTED_FUNCTIONS == null)
            RP_STARTED_FUNCTIONS = CMDHelper.getFunctions(player.getServer(), ServerUtilsMod.SwitchableResourcepacksModule.getConfig().playerStartFunction);

        CMDHelper.executeAs(player, RP_STARTED_FUNCTIONS);

        if (isCustomPackEnabled(player)) {
            addPackStatus(player, CUSTOMPACKUUID, false);
            packStatusUpdate(player, CUSTOMPACKUUID, PackStatus.LoadingStatus.FINISHED);
            return;
        }

        UUID packUUID = UUID.nameUUIDFromBytes(pack.packID.toString().getBytes(StandardCharsets.UTF_8));
        if (getPlayerStatus(player).getPacks().containsKey(packUUID))
            return;

        addPackStatus(player, packUUID, false);
        player.networkHandler.sendPacket(new ResourcePackSendS2CPacket(packUUID, pack.url, pack.hash, true, Optional.of(Text.literal(getConfig().message))));
    }

    public static void addPacks(List<ResourcePack> packList, ServerPlayerEntity player) {
        if (!hasNewPacks(packList, player)) return;

        if (RP_STARTED_FUNCTIONS == null)
            RP_STARTED_FUNCTIONS = CMDHelper.getFunctions(player.getServer(), ServerUtilsMod.SwitchableResourcepacksModule.getConfig().playerStartFunction);

        CMDHelper.executeAs(player, RP_STARTED_FUNCTIONS);

        if (isCustomPackEnabled(player)) {
            addPackStatus(player, CUSTOMPACKUUID, false);
            packStatusUpdate(player, CUSTOMPACKUUID, PackStatus.LoadingStatus.FINISHED);
            return;
        }

        clearTempPacks(player);

        for (ResourcePack pack : packList) {
            UUID packUUID = UUID.nameUUIDFromBytes(pack.packID.toString().getBytes(StandardCharsets.UTF_8));
            addPackStatus(player, packUUID, true);
            player.networkHandler.sendPacket(new ResourcePackSendS2CPacket(packUUID, pack.url, pack.hash, true, Optional.of(Text.literal(ServerUtilsMod.SwitchableResourcepacksModule.getConfig().message))));
        }
    }

    private static void clearTempPacks(ServerPlayerEntity player) {
        getPlayerStatus(player).getPacks().entrySet().removeIf(uuidStatusEntry -> {
            if (uuidStatusEntry.getValue().isTempPack()) {
                player.networkHandler.sendPacket(new ResourcePackRemoveS2CPacket(Optional.of(uuidStatusEntry.getKey())));
                return true;
            }
            return false;
        });
    }

    private static boolean hasNewPacks(List<ResourcePack> packList, ServerPlayerEntity player) {
        List<UUID> c = new ArrayList<>();
        getPlayerStatus(player).getPacks().forEach((uuid, status) -> c.add(uuid));

        List<UUID> n = new ArrayList<>();
        for (ResourcePack pack : packList) {
            UUID packUUID = UUID.nameUUIDFromBytes(pack.packID.toString().getBytes(StandardCharsets.UTF_8));
            n.add(packUUID);
        }

        if (c.size() != n.size())
            return true;

        Collections.sort(c);
        Collections.sort(n);

        return !c.equals(n);
    }
}