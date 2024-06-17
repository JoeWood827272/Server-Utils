package net.kyrptonaught.serverutils.switchableresourcepacks;

import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;

public class MusicPack {
    public Identifier packID;
    public PLAY_ORDER play_order = PLAY_ORDER.CHRONOLOGICAL;
    public String delay;

    public LinkedHashMap<Identifier, Integer> songs;

    public enum PLAY_ORDER {
        CHRONOLOGICAL,
        RANDOM
    }

}
