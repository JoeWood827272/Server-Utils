package net.kyrptonaught.serverutils.customMapLoader.addons;

import net.kyrptonaught.serverutils.datapackInteractables.BlockList;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionType;

public class BaseMapAddon extends BaseAddon {
    public BlockList interactable_blocklist;

    public ResourcePackList required_packs;
    public ResourcePackList optional_packs;

    public BattleMusic music_pack;
    public BattleMusic safe_music_pack;

    public Identifier dimensionType_id;
    public transient DimensionType loadedDimensionType;

}
