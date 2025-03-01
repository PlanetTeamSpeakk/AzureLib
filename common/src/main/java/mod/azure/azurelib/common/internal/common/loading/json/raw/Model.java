/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.internal.common.loading.json.raw;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import mod.azure.azurelib.common.internal.common.loading.json.FormatVersion;
import mod.azure.azurelib.common.internal.common.util.JsonUtil;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Container class for model information, only used in deserialization at startup
 */
public record Model(
        @Nullable FormatVersion formatVersion,
        MinecraftGeometry[] minecraftGeometry
) {

    public static JsonDeserializer<Model> deserializer() throws JsonParseException {
        return (json, type, context) -> {
            JsonObject obj = json.getAsJsonObject();
            FormatVersion formatVersion = context.deserialize(obj.get("format_version"), FormatVersion.class);
            MinecraftGeometry[] minecraftGeometry = JsonUtil.jsonArrayToObjectArray(
                    GsonHelper.getAsJsonArray(obj, "minecraft:geometry", new JsonArray(0)),
                    context,
                    MinecraftGeometry.class
            );

            return new Model(formatVersion, minecraftGeometry);
        };
    }
}
