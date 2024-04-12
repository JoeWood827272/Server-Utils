package net.kyrptonaught.serverutils.customUI;

import com.google.gson.JsonParseException;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.sgui.api.elements.GuiElementBuilder;
import eu.pb4.sgui.api.gui.SimpleGui;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.kyrptonaught.serverutils.CMDHelper;
import net.kyrptonaught.serverutils.Module;
import net.kyrptonaught.serverutils.serverTranslator.ServerTranslator;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.network.packet.s2c.play.UpdateSelectedSlotS2CPacket;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.resource.ResourceType;
import net.minecraft.scoreboard.ScoreHolder;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.ServerScoreboard;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class CustomUI extends Module {
    private static final HashMap<String, ScreenConfig> screens = new HashMap<>();
    private static final HashMap<String, ScreenConfig.SlotDefinition> slotPresets = new HashMap<>();

    private static final HashMap<UUID, Stack<String>> screenHistory = new HashMap<>();

    public static void showScreenFor(String screen, ServerPlayerEntity player) {
        ScreenConfig config = screens.get(screen);

        SimpleGui gui = new SimpleGui(ScreenHandlerType.GENERIC_9X6, player, true) {
            @Override
            public void onClose() {
                playSound(player, config.escSound);
                if (screenHistory.get(player.getUuid()).size() < 2 && !config.escToClose)
                    showScreenFor(screen, player);
                else showLastScreen(player);
            }
        };
        gui.setTitle(getAsText(config.title));


        int count = 0;
        for (ScreenConfig.SlotDefinition slot : config.dynamicSlotList) {
            ScreenConfig.SlotDefinition slotDefinition = getSlotDefinition(slot, player);
            if (slotDefinition.hidden()) continue;
            setClickHandlers(gui, count++, player, slotDefinition);
        }

        for (String slot : config.slots.keySet()) {
            ScreenConfig.SlotDefinition slotDefinition = getSlotDefinition(config.slots.get(slot), player);
            if (slotDefinition.hidden()) continue;
            for (Integer slotNum : expandSlotString(slot)) setClickHandlers(gui, slotNum, player, slotDefinition);
        }

        if (!screenHistory.containsKey(player.getUuid()))
            screenHistory.put(player.getUuid(), new Stack<>());

        screenHistory.get(player.getUuid()).push(screen);

        if (config.forceHotBarSlot > -1) {
            player.getInventory().selectedSlot = config.forceHotBarSlot;
            player.networkHandler.sendPacket(new UpdateSelectedSlotS2CPacket(config.forceHotBarSlot));
        }

        gui.open();
    }

    public static void replaceScreen(String screen, ServerPlayerEntity player) {
        if (!screenHistory.get(player.getUuid()).isEmpty())
            screenHistory.get(player.getUuid()).pop();
        showScreenFor(screen, player);
    }

    public static void refreshScreen(ServerPlayerEntity player) {
        String screen = screenHistory.get(player.getUuid()).pop();
        showScreenFor(screen, player);
    }

    private static void showLastScreen(ServerPlayerEntity player) {
        if (!screenHistory.containsKey(player.getUuid()) || screenHistory.get(player.getUuid()).size() < 2) {
            return;
        }
        screenHistory.get(player.getUuid()).pop();
        String screenID = screenHistory.get(player.getUuid()).pop();
        showScreenFor(screenID, player);
    }

    private static void setClickHandlers(SimpleGui gui, int slotNum, ServerPlayerEntity player, ScreenConfig.SlotDefinition slotDefinition) {
        gui.setSlot(slotNum, GuiElementBuilder.from(slotDefinition.generatedStack)
                .setCallback((index, type, action) -> {
                    if (type.isLeft)
                        handleClick(player, slotDefinition.leftClickAction, slotDefinition.leftClickSound, slotDefinition);
                    if (type.isRight)
                        handleClick(player, slotDefinition.rightClickAction, slotDefinition.rightClickSound, slotDefinition);
                })
        );
    }

    private static ScreenConfig.SlotDefinition getSlotDefinition(ScreenConfig.SlotDefinition slotDefinition, ServerPlayerEntity player) {
        copyFromPreset(slotDefinition);

        if (slotDefinition.isDynamic()) {
            ScreenConfig.SlotDefinition.DynamicItem model = slotDefinition.dynamicItem;
            ServerScoreboard scoreboard = player.getServer().getScoreboard();
            ScoreboardObjective objective = scoreboard.getNullableObjective(model.score);
            String playerName = model.player;
            if (playerName.equals("@s"))
                playerName = player.getNameForScoreboard();
            int score = scoreboard.getOrCreateScore(ScoreHolder.fromName(playerName), objective).getScore();

            if (model.items != null && model.items.containsKey(score)) {
                ScreenConfig.SlotDefinition value = model.items.get(score);
                value.dynamicItem = ScreenConfig.SlotDefinition.EMPTY_ITEM;
                copyFromPreset(value);
                value.copyFrom(slotDefinition);
                slotDefinition = value;
            }
        }

        slotDefinition.generatedStack = Registries.ITEM.get(new Identifier(slotDefinition.itemID)).getDefaultStack();

        if (slotDefinition.itemNBT != null)
            try {
                NbtCompound compound = StringNbtReader.parse(slotDefinition.itemNBT);
                slotDefinition.generatedStack.setNbt(compound);
            } catch (CommandSyntaxException e) {
                e.printStackTrace();
            }

        if (slotDefinition.customModelData != null)
            try {
                String value = ServerTranslator.translate(player, slotDefinition.customModelData);
                int intValue = Integer.parseInt(value);
                slotDefinition.generatedStack.getOrCreateNbt().putInt("CustomModelData", intValue);
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }

        if (slotDefinition.displayName != null)
            slotDefinition.generatedStack.setCustomName(getAsText(slotDefinition.displayName));

        return slotDefinition;
    }

    private static void copyFromPreset(ScreenConfig.SlotDefinition slotDefinition) {
        if (slotDefinition.presetID != null)
            slotDefinition.copyFrom(slotPresets.get(slotDefinition.presetID));
    }

    private static List<Integer> expandSlotString(String slot) {
        if (slot.contains("-")) {
            int start = Integer.parseInt(slot.substring(0, slot.indexOf("-")));
            int end = Integer.parseInt(slot.substring(slot.indexOf("-") + 1));

            List<Integer> slots = new ArrayList<>();
            while (start <= end)
                slots.add(start++);
            return slots;
        }
        if (slot.contains(",")) {
            return Arrays.stream(slot.split(",")).map(Integer::parseInt).toList();
        }

        return List.of(Integer.parseInt(slot));
    }

    private static void handleClick(ServerPlayerEntity player, String action, String soundID, ScreenConfig.SlotDefinition slot) {
        playSound(player, soundID);
        if (action == null) return;

        String cmd = action.substring(action.indexOf("/") + 1).trim();
        if (action.startsWith("command/")) {
            CMDHelper.executeAs(player, cmd);
        } else if (action.startsWith("function/")) {
            CMDHelper.executeFunctionsAs(player, cmd);
        } else if (action.startsWith("openUI/")) {
            if (slot.replaceOpenScreen()) replaceScreen(cmd, player);
            else showScreenFor(cmd, player);
        } else if (action.startsWith("close/")) {
            player.closeHandledScreen();
        } else if (action.startsWith("back/")) {
            showLastScreen(player);
        } else if (action.startsWith("kick/")) {
            player.networkHandler.disconnect(Text.literal(cmd));
        }

        if (slot.refreshOnInteract()) {
            refreshScreen(player);
        }
    }

    private static Text getAsText(String text) {
        try {
            return Objects.requireNonNullElseGet(Text.Serialization.fromJson(text), () -> Text.literal(text));
        } catch (JsonParseException var4) {
            return Text.literal(text);
        }
    }

    private static void playSound(ServerPlayerEntity player, String soundID) {
        if (soundID != null) {
            RegistryEntry<SoundEvent> sound = RegistryEntry.of(SoundEvent.of(new Identifier(soundID)));
            Vec3d pos = player.getPos();
            player.networkHandler.sendPacket(new PlaySoundS2CPacket(sound, SoundCategory.MASTER, pos.x, pos.y, pos.z, 1, 1, player.getRandom().nextLong()));
        }
    }

    public static void addScreen(String screenID, ScreenConfig screenConfig) {
        screens.put(screenID, screenConfig);
    }

    public static void addPresets(String screenID, ScreenConfig screenConfig) {
        for (String presetID : screenConfig.presets.keySet()) {
            slotPresets.put(screenID + ":" + presetID, screenConfig.presets.get(presetID));
        }
    }

    public static void reload() {
        screens.clear();
        slotPresets.clear();
    }

    @Override
    public void onInitialize() {
        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new ScreenConfigLoader());
    }

    @Override
    public void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        var arg = CommandManager.argument("screenID", StringArgumentType.greedyString())
                .suggests((context, builder) -> {
                    screens.keySet().forEach(builder::suggest);
                    return builder.buildFuture();
                });

        dispatcher.register(CommandManager.literal("showCustomScreen")
                .requires((source) -> source.hasPermissionLevel(2))
                .then(CommandManager.literal("append")
                        .then(arg.executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            String screenID = StringArgumentType.getString(context, "screenID");
                            showScreenFor(screenID, player);
                            return 1;
                        })))
                .then(arg.executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (screenHistory.containsKey(player.getUuid()))
                        screenHistory.get(player.getUuid()).clear();

                    String screenID = StringArgumentType.getString(context, "screenID");
                    showScreenFor(screenID, player);
                    return 1;
                })));
    }
}
