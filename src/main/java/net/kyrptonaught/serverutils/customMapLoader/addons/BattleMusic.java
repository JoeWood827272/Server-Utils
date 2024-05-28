package net.kyrptonaught.serverutils.customMapLoader.addons;

import com.google.gson.*;
import net.kyrptonaught.serverutils.switchableresourcepacks.MusicPack;
import net.kyrptonaught.serverutils.switchableresourcepacks.SwitchableResourcepacksMod;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;

public class BattleMusic extends MusicPack {

    public static class Serializer implements JsonSerializer<BattleMusic>, JsonDeserializer<BattleMusic> {
        @Override
        public BattleMusic deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            BattleMusic music;

            MusicPack baseMusic;
            if (json.isJsonObject()) {
                baseMusic = context.deserialize(json, MusicPack.class);
            } else {
                Identifier id = context.deserialize(json, Identifier.class);
                baseMusic = SwitchableResourcepacksMod.MusicPacks.get(id);
            }

            music = new BattleMusic();
            music.packID = baseMusic.packID;
            music.play_order = baseMusic.play_order;
            music.songs = baseMusic.songs;
            return music;
        }

        @Override
        public JsonElement serialize(BattleMusic src, Type typeOfSrc, JsonSerializationContext context) {
            return context.serialize(src.packID);
        }
    }
}