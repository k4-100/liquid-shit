package com.example.cuboidcheck.utl;

import javax.annotation.Nullable;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.TagParser;

public record BlockData(
                int x, int y, int z,
                JsonElement blockState,
                @Nullable CompoundTag blockEntityTag) {

        public JsonObject toJson() {
                JsonObject json = new JsonObject();
                json.addProperty("x", x);
                json.addProperty("y", y);
                json.addProperty("z", z);
                json.add("blockState", blockState);
                if (blockEntityTag != null) {
                        json.addProperty("nbt", blockEntityTag.getAsString());
                }
                return json;
        }

        public static BlockData fromJson(JsonObject json) {
                int x = json.get("x").getAsInt();
                int y = json.get("y").getAsInt();
                int z = json.get("z").getAsInt();
                JsonElement state = json.get("blockState");

                CompoundTag nbt = null;
                if (json.has("nbt")) {
                        try {
                                nbt = TagParser.parseTag(json.get("nbt").getAsString());
                        } catch (Exception ignored) {
                        }
                }
                return new BlockData(x, y, z, state, nbt);
        }
}
