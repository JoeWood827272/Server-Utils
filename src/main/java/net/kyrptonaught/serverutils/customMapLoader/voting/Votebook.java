package net.kyrptonaught.serverutils.customMapLoader.voting;

import eu.pb4.sgui.api.gui.BookGui;
import net.kyrptonaught.serverutils.customMapLoader.CustomMapLoaderMod;
import net.kyrptonaught.serverutils.customMapLoader.MapSize;
import net.kyrptonaught.serverutils.customMapLoader.addons.BattleMapAddon;
import net.kyrptonaught.serverutils.customMapLoader.voting.pages.BookPage;
import net.kyrptonaught.serverutils.customMapLoader.voting.pages.DynamicData;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.function.BiConsumer;

public class Votebook {

    private static final HashMap<String, BookPage> bookLibrary = new HashMap<>();
    private static final HashMap<String, BiConsumer<DynamicData, BookPage>> dynamicLibrary = new HashMap<>();

    public static void generateBookLibrary(List<BattleMapAddon> addons) {
        bookLibrary.clear();
        dynamicLibrary.clear();

        bookLibrary.put("title", createBasicPage().addPage(
                Text.literal("Legacy Edition Battle").formatted(Formatting.BLUE),
                Text.translatable("lem.mapdecider.menu.header").formatted(Formatting.DARK_AQUA),
                Text.translatable("lem.mapdecider.menu.credit", withLink(Text.literal("DBTDerpbox & Kyrptonaught").styled(style -> style.withColor(0x99C1F1).withUnderline(true)), "https://www.legacyminigames.net/credits")).styled(style -> style.withColor(0x62A0EA)),
                Text.empty(),
                Text.translatable("lem.mapdecider.menu.supportplease", withLink(Text.literal("Patreon").styled(style -> style.withUnderline(true).withColor(0xFF424D)), "https://www.legacyminigames.net/patreon")).formatted(Formatting.GOLD),
                Text.empty(),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.mapvoting"), "mapVoting"),
                withOpenCmd(bracketTrans("lem.generic.options"), "options")
        ));

        bookLibrary.put("mapVoting", createBasicPage().addPage(
                dashTrans("lem.mapdecider.menu.mapvoting"),
                Text.empty(),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.basegame"), "baseMaps"),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.mods"), "customMaps"),
                Text.empty(),
                withCmd(bracketTrans("lem.mapdecider.menu.removevote").formatted(Formatting.RED), "/custommaploader voting removeVote"),
                Text.empty(),
                backButton("title")
        ));

        bookLibrary.put("options", createBasicPage().addPage(
                dashTrans("lem.generic.options"),
                Text.empty(),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.packpolicy.global"), "player_rp_settings"),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.hosts"), "hostConfig"),
                Text.empty(),
                backButton("title")
        ));

        bookLibrary.put("hostConfig", createBasicPage().addPage(
                dashTrans("lem.mapdecider.menu.hosts"),
                Text.empty(),
                withCmd(bracketTrans("lem.mapdecider.menu.gameoptions"), "/trigger lem.gamecfg"),
                withCmd(bracketTrans("lem.mapdecider.menu.transferhost"), "/trigger lem.gamecfg set 103"),
                withOpenCmd(bracketTrans("lem.mapdecider.menu.mapsettings"), "host_map_settings"),
                Text.empty(),
                backButton("options")
        ));

        dynamicLibrary.put("host_map_settings", Votebook::generateHostSettings);
        dynamicLibrary.put("host_map_enable_disable", Votebook::generateMapEnableDisable);
        dynamicLibrary.put("player_rp_settings", Votebook::generatePlayerPackPolicy);
        dynamicLibrary.put("map_player_rp_settings", Votebook::generateMapExtras);

        generateMapPacks(false, addons.stream().filter(config -> !config.isBaseAddon).toList());
        generateMapPacks(true, addons.stream().filter(config -> config.isBaseAddon).toList());
        generateMapPages(addons);
    }

    private static BookPage createBasicPage() {
        return new BookPage("Voting Book", "LEM");
    }

    public static BookGui getPage(ServerPlayerEntity player, String page, String[] args) {
        if (dynamicLibrary.containsKey(page)) {
            BookPage book = createBasicPage();
            dynamicLibrary.get(page).accept(new DynamicData(player, CustomMapLoaderMod.getAllBattleMaps(), args), book);
            return book.build(player);
        }

        BookPage book = bookLibrary.get(page);
        if (book == null) book = createBasicPage().addPage(
                Text.translatable("lem.mapdecider.menu.missing"),
                Text.literal(page),
                Text.empty(),
                backButton("title")
        );
        return book.build(player);
    }

    private static void generateMapPacks(boolean isBase, List<BattleMapAddon> addons) {
        HashMap<String, List<BattleMapAddon>> packs = new HashMap<>();

        for (BattleMapAddon config : addons) {
            String pack = config.addon_pack;

            if (!packs.containsKey(pack)) {
                packs.put(pack, new ArrayList<>());
            }

            packs.get(pack).add(config);
        }

        List<Text> basePackOrdered = new ArrayList<>();

        List<Text> packsText = new ArrayList<>();
        packsText.add(dashTrans("lem.mapdecider.menu.mapvoting"));
        packsText.add(Text.empty());

        for (String pack : packs.keySet()) {
            if (pack.equals("base_base")) continue;

            BookPage builder = createBasicPage();

            List<BattleMapAddon> packAddons = packs.get(pack);
            List<List<Text>> packPages = generateMapPackPages(packAddons, true);

            MutableText hover = Text.empty();

            for (int i = 0; i < 6 && i < packAddons.size(); i++) {
                hover.append(packAddons.get(i).getNameText());
                if (i != packAddons.size() - 1) hover.append("\n");
                if (i == 5 && packAddons.size() > 6) hover.append("...");
            }

            for (int i = 0; i < packPages.size(); i++) {
                packPages.get(i).add(Text.empty());

                if (packPages.size() == 1) {
                    packPages.get(i).add(backButton(isBase ? "baseMaps" : "customMaps"));
                    builder.addPage(packPages.get(i).toArray(Text[]::new));
                    continue;
                }

                if (i == packPages.size() - 1) {
                    packPages.get(i).add(nextButton("gui.back", i));
                } else if (i == 0) {
                    packPages.get(i).add(backButton(isBase ? "baseMaps" : "customMaps").append(" ").append(nextButton("createWorld.customize.custom.next", i + 2)));
                } else {
                    packPages.get(i).add(nextButton("gui.back", i).append(" ").append(nextButton("createWorld.customize.custom.next", i + 2)));
                }

                builder.addPage(packPages.get(i).toArray(Text[]::new));
            }

            if (isBase)
                basePackOrdered.add(withOpenCmd(bracket(packs.get(pack).get(0).getAddonPackText()).formatted(Formatting.GOLD), "mapPack_" + pack, hover));
            else
                packsText.add(withOpenCmd(bracket(packs.get(pack).get(0).getAddonPackText()).formatted(Formatting.GOLD), "mapPack_" + pack, hover));
            bookLibrary.put("mapPack_" + pack, builder);
        }

        if (isBase && packs.containsKey("base_base")) {
            List<List<Text>> base_pages = generateMapPackPages(packs.get("base_base"), false);
            basePackOrdered.addAll(base_pages.get(0));
        }

        if (isBase) {
            basePackOrdered.sort(Comparator.comparingInt(text -> {
                String str = text.getString();

                if (str.startsWith("[M"))
                    return Integer.parseInt(str.substring(str.length() - 2, str.length() - 1));
                if (str.startsWith("[V"))
                    return 100;
                if (str.startsWith("[H"))
                    return 101;
                if (str.startsWith("[Fe"))
                    return 102;
                if (str.startsWith("[Fa"))
                    return 103;

                return 0;
            }));
            packsText.addAll(basePackOrdered);
        }

        packsText.add(Text.empty());
        packsText.add(backButton("mapVoting"));
        bookLibrary.put(isBase ? "baseMaps" : "customMaps", createBasicPage().addPage(packsText.toArray(Text[]::new)));
    }

    private static List<List<Text>> generateMapPackPages(List<BattleMapAddon> packMods, boolean includeHeader) {
        int maxPerPage = 10;
        int lastUsed = 0;

        List<List<Text>> pages = new ArrayList<>();

        while (lastUsed < packMods.size()) {
            List<Text> pageText = new ArrayList<>();
            if (includeHeader) {
                pageText.add(dash(packMods.get(0).getAddonPackText()));
                pageText.add(Text.empty());
            }

            for (; lastUsed < packMods.size() && lastUsed - (pages.size() * maxPerPage) < maxPerPage; lastUsed++) {
                BattleMapAddon config = packMods.get(lastUsed);

                pageText.add(withHover(withOpenCmd(bracket(trimName(config.getNameText(), 20)), "map_" + config.addon_id), generateMapTooltip(config)));
            }
            pages.add(pageText);
        }

        return pages;
    }

    private static void generateMapPages(List<BattleMapAddon> lemmods) {
        for (BattleMapAddon config : lemmods) {
            List<Text> mapText = new ArrayList<>();

            mapText.add(withHover(config.getNameText().formatted(Formatting.BLUE), generateMapTooltip(config)));
            mapText.add(config.getDescriptionText().formatted(Formatting.DARK_AQUA));
            mapText.add(Text.empty());

            MutableText backBtn = backButton("mapPack_" + config.addon_pack);
            if (config.addon_pack.equals("base_base"))
                backBtn = backButton("baseMaps");

            mapText.add(voteButton(config.addon_id).append(" ").append(backBtn.append(" ").append(withOpenCmd(bracketTrans("lem.generic.more"), "map_player_rp_settings", "index,0," + config.addon_id.toString()))));

            bookLibrary.put("map_" + config.addon_id, createBasicPage().addPage(mapText.toArray(Text[]::new)));
        }
    }

    private static Text generateMapTooltip(BattleMapAddon config) {
        MutableText availableTypes = Text.empty();
        if (config.hasSize(MapSize.SMALL))
            availableTypes.append(Text.translatable("lem.battle.menu.host.config.maps.option.small")).append(", ");
        if (config.hasSize(MapSize.LARGE))
            availableTypes.append(Text.translatable("lem.battle.menu.host.config.maps.option.large")).append(", ");
        if (config.hasSize(MapSize.LARGE_PLUS))
            availableTypes.append(Text.translatable("lem.battle.menu.host.config.maps.option.largeplus")).append(", ");
        if (config.hasSize(MapSize.REMASTERED))
            availableTypes.append(Text.translatable("lem.battle.menu.host.config.maps.option.remastered")).append(", ");
        availableTypes.getSiblings().remove(availableTypes.getSiblings().size() - 1);

        return config.getNameText().append("\n")
                .append(Text.translatable("mco.template.select.narrate.authors", config.authors)).append("\n")
                .append(Text.translatable("mco.version", config.version)).append("\n")
                //.append(Text.translatable("lem.mapdecider.menu.voting.pack", Text.translatable("lem.resource." + config.resource_pack + ".name"))).append("\n")
                .append(Text.translatable("lem.mapdecider.menu.voting.typelist", availableTypes));
    }

    private static void generateHostSettings(DynamicData data, BookPage bookPage) {
        MapSize selected = HostOptions.selectedMapSize;

        bookPage.addPage(
                dashTrans("lem.mapdecider.menu.mapsettings"),
                Text.empty(),
                withOpenCmd(withHover(bracketTrans("lem.menu.host.config.maps.enabled.header"), Text.translatable("lem.menu.host.config.maps.enabled.tooltip")), "host_map_enable_disable"),
                Text.empty(),
                withHover(coloredTrans("lem.battle.menu.host.config.maps.option.selectedsize", Formatting.GOLD), Text.translatable("lem.battle.menu.host.config.maps.option.selectedsize.tooltip")),
                withOpenAfterCmd(colored(bracketTrans("lem.battle.menu.host.config.maps.option.auto"), selected == MapSize.AUTO ? Formatting.GREEN : Formatting.BLUE), "host_map_settings", "custommaploader hostOptions mapSize set Auto"),
                withOpenAfterCmd(colored(bracketTrans("lem.battle.menu.host.config.maps.option.small"), selected == MapSize.SMALL ? Formatting.GREEN : Formatting.BLUE), "host_map_settings", "custommaploader hostOptions mapSize set Small"),
                withOpenAfterCmd(colored(bracketTrans("lem.battle.menu.host.config.maps.option.large"), selected == MapSize.LARGE ? Formatting.GREEN : Formatting.BLUE), "host_map_settings", "custommaploader hostOptions mapSize set Large"),
                withOpenAfterCmd(colored(bracketTrans("lem.battle.menu.host.config.maps.option.largeplus"), selected == MapSize.LARGE_PLUS ? Formatting.GREEN : Formatting.BLUE), "host_map_settings", "custommaploader hostOptions mapSize set Large+"),
                withOpenAfterCmd(colored(bracketTrans("lem.battle.menu.host.config.maps.option.remastered"), selected == MapSize.REMASTERED ? Formatting.GREEN : Formatting.BLUE), "host_map_settings", "custommaploader hostOptions mapSize set Remastered"),
                Text.empty(),
                backButton("hostConfig"));
    }

    private static void generateMapEnableDisable(DynamicData data, BookPage bookPage) {
        List<BattleMapAddon> packMods = data.addons();

        Text header = dashTrans("lem.menu.host.config.maps.enabled.header");
        splitAcrossPages(bookPage, 10, packMods, header, "host_map_settings", false, (config, index, pageText) -> {
            boolean enabled = config.isAddonEnabled;
            String args = "index," + bookPage.size();
            String cmd = "custommaploader hostOptions enableMap " + config.addon_id + " " + (!enabled);
            pageText.add(withHover(withOpenAfterCmd(colored(bracket(trimName(config.getNameText(), 20)), enabled ? Formatting.GREEN : Formatting.RED), "host_map_enable_disable", args, cmd), Text.translatable("lem.menu.host.config.maps.enabled.toggle")));
        });
    }

    private static void generateMapExtras(DynamicData data, BookPage bookPage) {
        BattleMapAddon addon = CustomMapLoaderMod.BATTLE_MAPS.get(Identifier.of(data.arg(2)));

        Text requiredHeader = withHover(dashTrans("lem.mapdecider.menu.requiredpacks"), trimName(addon.getNameText(), 20));
        if (addon.required_packs == null || addon.required_packs.packs.isEmpty()) {
            splitAcrossPages(bookPage, 1, List.of(Text.translatable("gui.none")), requiredHeader, "map_" + addon.addon_id, true, (rpOption, index, pageText) -> {
                pageText.add(colored(rpOption, Formatting.DARK_GRAY));
            });
        } else {
            splitAcrossPages(bookPage, 10, addon.required_packs.packs, requiredHeader, "map_" + addon.addon_id, true, (rpOption, index, pageText) -> {
                MutableText hover = rpOption.getNameText().append("\n").append(rpOption.getDescriptionText());
                pageText.add(colored(withHover(trimName(rpOption.getNameText(), 20), hover), Formatting.DARK_GRAY));
            });
        }

        Text optionalHeader = withHover(dashTrans("lem.mapdecider.menu.optionalpacks"), trimName(addon.getNameText(), 20));
        if (addon.optional_packs == null || addon.optional_packs.packs.isEmpty()) {
            splitAcrossPages(bookPage, 1, List.of(Text.translatable("gui.none")), optionalHeader, "map_" + addon.addon_id, false, (rpOption, index, pageText) -> {
                pageText.add(colored(rpOption, Formatting.DARK_GRAY));
            });
        } else {
            splitAcrossPages(bookPage, 8, addon.optional_packs.packs, optionalHeader, "map_" + addon.addon_id, false, (rpOption, index, pageText) -> {
                boolean overwrite = HostOptions.getOverwriteValue(data.player(), addon.addon_id);
                String args = "index," + bookPage.size() + "," + addon.addon_id.toString();

                if (index == 0) {
                    String cmd = "custommaploader playerOptions optionalPacks globalAccept overwrite " + addon.addon_id;
                    MutableText enabled = toggleCheckBox(overwrite, "map_player_rp_settings", args, cmd);
                    pageText.add(colored(Text.translatable("lem.mapdecider.menu.optionalpacks.overwriteglobal", enabled), Formatting.DARK_GRAY));
                    pageText.add(Text.empty());
                }

                if (overwrite) {
                    MutableText hover = rpOption.getNameText().append("\n").append(rpOption.getDescriptionText());
                    String cmd = "custommaploader playerOptions optionalPacks " + addon.addon_id + " accept " + rpOption.packID;
                    MutableText enabled = toggleCheckBox(HostOptions.getMapResourcePackValue(data.player(), addon.addon_id, rpOption.packID), "map_player_rp_settings", args, cmd).append(" ");
                    pageText.add(enabled.append(colored(withHover(trimName(rpOption.getNameText(), 20), hover), Formatting.DARK_GRAY)));
                } else {
                    MutableText hover = Text.translatable("lem.mapdecider.menu.optionalpacks.enableoverwrite");
                    pageText.add(colored(withHover(toggleCheckBox(false).append(" ").append(trimName(rpOption.getNameText(), 20)), hover), Formatting.GRAY));
                }
            });
        }
    }

    private static void generatePlayerPackPolicy(DynamicData data, BookPage bookPage) {
        boolean policy = HostOptions.getGlobalAcceptValue(data.player());

        String cmd = "custommaploader playerOptions optionalPacks globalAccept";
        MutableText enabled = toggleCheckBox(policy, "player_rp_settings", "player_rp_settings", cmd);
        bookPage.addPage(
                dashTrans("lem.mapdecider.menu.packpolicy.global"),
                Text.empty(),
                coloredTrans("lem.mapdecider.menu.packpolicy.promptpolicy", Formatting.GOLD),
                colored(Text.translatable("lem.mapdecider.menu.packpolicy.optionalpacks", enabled), Formatting.DARK_GRAY),
                Text.empty(),
                withOpenAfterCmd(colored(bracketTrans("lem.mapdecider.menu.packpolicy.reset"), Formatting.DARK_RED), "player_rp_settings", "custommaploader playerOptions optionalPacks reset"),
                Text.empty(),
                backButton("options"));
    }

    private static <T> void splitAcrossPages(BookPage bookPage, int maxPerPage, List<T> items, Text header, String previousPage, boolean alwaysShowNext, SplitData<T, Integer, List<Text>> forEachItem) {
        int startingPageIndex = bookPage.size();
        int lastUsed = 0;
        int pages = 0;

        while (lastUsed < items.size()) {
            List<Text> pageText = new ArrayList<>();

            if (header != null) {
                pageText.add(header);
                pageText.add(Text.empty());
            }

            for (; lastUsed < items.size() && lastUsed - (pages * maxPerPage) < maxPerPage; lastUsed++) {
                forEachItem.accept(items.get(lastUsed), lastUsed, pageText);
            }

            pageText.add(Text.empty());
            if (pages == 0) {
                if (startingPageIndex > 0) pageText.add(nextButton("gui.back", startingPageIndex));
                else pageText.add(backButton(previousPage));
            } else pageText.add(nextButton("gui.back", startingPageIndex + pages));

            if (lastUsed < items.size() || alwaysShowNext)
                ((MutableText) pageText.get(pageText.size() - 1)).append(" ").append(nextButton("createWorld.customize.custom.next", startingPageIndex + pages + 2));

            bookPage.addPage(pageText.toArray(Text[]::new));
            pages++;
        }
    }

    private static MutableText toggleCheckBox(boolean value, String page, String arg, String cmd) {
        return colored(withOpenAfterCmd(toggleCheckBox(value), page, arg, cmd + (value ? " false" : " true")), value ? Formatting.GREEN : Formatting.RED);
    }

    private static MutableText toggleCheckBox(boolean value) {
        return withHover(Text.literal(value ? "[✔]" : "[❌]"), Text.translatable("lem.mapdecider.menu.clicktoggle"));
    }

    private static MutableText trimName(MutableText text, int maxLength) {
        if (text.getContent() instanceof PlainTextContent content) {
            String value = content.string();

            if (value.length() > maxLength) {
                value = value.substring(0, maxLength - 3) + "...";
            }

            return Text.literal(value);
        }

        return text;
    }

    private static MutableText colored(MutableText text, Formatting... formatting) {
        return text.formatted(formatting);
    }

    private static MutableText colored(String text, Formatting... formatting) {
        return colored(Text.literal(text), formatting);
    }

    private static MutableText coloredTrans(String transKey, Formatting... formatting) {
        return colored(Text.translatable(transKey), formatting);
    }

    private static MutableText bracketTrans(String transKey) {
        return bracket(Text.translatable(transKey));
    }

    private static MutableText bracketLit(String text) {
        return bracket(Text.literal(text));
    }

    private static MutableText bracket(MutableText text) {
        return colored(Text.literal("[").append(text).append("]"), Formatting.GOLD);
    }

    private static MutableText dashTrans(String transKey) {
        return dash(Text.translatable(transKey));
    }

    private static MutableText dashLit(String text) {
        return dash(Text.literal(text));
    }

    private static MutableText dash(MutableText text) {
        return colored(Text.literal("- ").append(text).append(" -"), Formatting.BLUE);
    }

    private static MutableText voteButton(Identifier map) {
        return bracketTrans("lem.mapdecider.menu.vote").styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/custommaploader voting vote " + map)));
    }

    private static MutableText backButton(String page) {
        return colored(withOpenCmd(bracketTrans("gui.back"), page), Formatting.GRAY);
    }

    private static MutableText nextButton(String transKey, int page) {
        return colored(withClickEvent(bracketTrans(transKey), ClickEvent.Action.CHANGE_PAGE, "" + page), Formatting.GRAY);
    }

    private static MutableText withHover(MutableText text, Text hover) {
        return text.styled(style -> style.withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, hover)));
    }

    private static MutableText withOpenCmd(MutableText text, String page) {
        return withCmd(text, "/custommaploader voting showBookPage \"" + page + "\"");
    }

    private static MutableText withOpenCmd(MutableText text, String page, String arg) {
        return withCmd(text, "/custommaploader voting showBookPage \"" + page + "\" arg \"" + arg + "\"");
    }

    private static MutableText withOpenCmd(MutableText text, String page, Text hover) {
        return withHover(withOpenCmd(text, page), hover);
    }

    private static MutableText withOpenAfterCmd(MutableText text, String page, String afterCmd) {
        return withCmd(text, "/custommaploader voting showBookPage \"" + page + "\" after " + afterCmd);
    }

    private static MutableText withOpenAfterCmd(MutableText text, String page, String arg, String afterCmd) {
        return withCmd(text, "/custommaploader voting showBookPage \"" + page + "\" arg \"" + arg + "\" after " + afterCmd);
    }

    private static MutableText withCmd(MutableText text, String cmd) {
        return withClickEvent(text, ClickEvent.Action.RUN_COMMAND, cmd);
    }

    private static MutableText withClickEvent(MutableText text, ClickEvent.Action action, String option) {
        return text.styled(style -> style.withClickEvent(new ClickEvent(action, option)));
    }

    private static MutableText withLink(MutableText text, String url) {
        return text.styled(style -> style.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)).withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(url))));
    }

    @FunctionalInterface
    public interface SplitData<T, U, V> {
        void accept(T t, U u, V V);
    }
}
