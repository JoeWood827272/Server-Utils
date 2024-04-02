package net.kyrptonaught.serverutils.customMapLoader.addons;

public class LobbyMapAddon extends BaseMapAddon {
    public static final String TYPE = "lobby_map";

    public String center_coords;
    public String world_border_coords_1;
    public String world_border_coords_2;
    public String[] spawn_coords;
    public String winner_coords;

    public String getDirectoryInZip() {
        return "world/lobby/";
    }
}
