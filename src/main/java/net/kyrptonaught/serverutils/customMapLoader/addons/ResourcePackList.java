package net.kyrptonaught.serverutils.customMapLoader.addons;

import com.google.gson.*;
import net.kyrptonaught.serverutils.switchableresourcepacks.ResourcePack;
import net.kyrptonaught.serverutils.switchableresourcepacks.SwitchableResourcepacksMod;
import net.minecraft.util.Identifier;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ResourcePackList {

    public List<ResourcePack> packs = new ArrayList<>();

    public static class Serializer implements JsonSerializer<ResourcePackList>, JsonDeserializer<ResourcePackList> {
        @Override
        public ResourcePackList deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            ResourcePackList packList = new ResourcePackList();
            JsonArray array = json.getAsJsonArray();

            for (int i = 0; i < array.size(); i++) {
                if (array.get(i).isJsonObject()) {
                    packList.packs.add(context.deserialize(array.get(i), ResourcePack.class));
                } else {
                    packList.packs.add(SwitchableResourcepacksMod.ResourcePacks.get(Identifier.of(array.get(i).getAsString())));
                }
            }

            return packList;
        }

        @Override
        public JsonElement serialize(ResourcePackList src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            for (int i = 0; i < src.packs.size(); i++) {
                array.add(src.packs.get(i).packID.toString());
            }

            return array;
        }
    }
}