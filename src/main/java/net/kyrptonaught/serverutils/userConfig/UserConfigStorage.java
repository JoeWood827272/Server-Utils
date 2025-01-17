package net.kyrptonaught.serverutils.userConfig;

import com.google.gson.JsonObject;
import net.kyrptonaught.serverutils.ServerUtilsMod;
import net.kyrptonaught.serverutils.backendServer.BackendServerModule;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class UserConfigStorage {
    private static final HashMap<UUID, PlayerConfigs> playerCache = new HashMap<>();
    private static final HashMap<Identifier, Set<Identifier>> groups = new HashMap<>();

    public static void setValue(ServerPlayerEntity player, Identifier key, String value) {
        playerCache.get(player.getUuid()).setValue(key, value);
    }

    public static String getValue(ServerPlayerEntity player, Identifier key) {
        return playerCache.get(player.getUuid()).getValue(key);
    }

    public static void removeValue(ServerPlayerEntity player, Identifier key) {
        playerCache.get(player.getUuid()).removeValue(key);
    }

    public static Identifier[] getAllKeys(ServerPlayerEntity player) {
        return playerCache.get(player.getUuid()).configs.keySet().toArray(Identifier[]::new);
    }

    public static void removeGroup(Identifier groupID) {
        groups.remove(groupID);
    }

    public static void addToGroup(Identifier groupID, Identifier key) {
        if (!groups.containsKey(groupID)) groups.put(groupID, new HashSet<>());

        groups.get(groupID).add(key);
    }

    public static void saveGroupToPreset(ServerPlayerEntity player, Identifier groupID, Identifier presetID) {
        playerCache.get(player.getUuid()).saveToPreset(presetID, groups.get(groupID));
    }

    public static void loadGroupFromPreset(ServerPlayerEntity player, Identifier groupID, Identifier presetID) {
        playerCache.get(player.getUuid()).loadFromPreset(presetID, groups.get(groupID));
    }

    public static void unloadPlayer(ServerPlayerEntity player) {
        //todo fix later
        //playerCache.remove(player.getUuid());
    }

    public static void loadPlayer(ServerPlayerEntity player) {
        try {
            playerCache.put(player.getUuid(), new PlayerConfigs());
            BackendServerModule.asyncGet(BackendServerModule.getUrl("getUserConfig", player), (success, response) -> {
                if (success) {
                    playerCache.put(player.getUuid(), PlayerConfigs.load(ServerUtilsMod.getGson().fromJson(response.body(), JsonObject.class)));
                } else {
                    System.out.println("Loading user config for " + player.getDisplayName().getString() + " failed... loading local");
                    playerCache.put(player.getUuid(), PlayerConfigs.load(UserConfigLocalStorage.loadPlayer(player.getUuidAsString())));
                }

            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void syncPlayer(ServerPlayerEntity player) {
        String json = ServerUtilsMod.getGson().toJson(playerCache.get(player.getUuid()));
        BackendServerModule.asyncPost(BackendServerModule.getUrl("syncUserConfig", player), json, (success, response) -> {
            if (!success) {
                System.out.println("Syncing user config for " + player.getDisplayName().getString() + " failed... saving local");
                UserConfigLocalStorage.syncPlayer(player.getUuidAsString(), json);
            }
        });
    }
}
