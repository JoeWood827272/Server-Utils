package net.kyrptonaught.serverutils.switchableresourcepacks;

import net.kyrptonaught.serverutils.AbstractConfigFile;

import java.util.ArrayList;
import java.util.List;

public class ResourcePackConfig extends AbstractConfigFile {
    public String playerStartFunction;
    public String playerCompleteFunction;
    public String playerFailedFunction;

    public String message = "plz use me";

    public List<ResourcePack> packs = new ArrayList<>();

    public List<MusicPack> musicPacks = new ArrayList<>();

}
