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
import mod.azure.azurelib.common.internal.common.util.JsonUtil;
import net.minecraft.util.GsonHelper;
import org.jetbrains.annotations.Nullable;

/**
 * Container class for generic geometry information, only used in deserialization at startup
 */
public record MinecraftGeometry(
        Bone[] bones,
        @Nullable String cape,
        @Nullable ModelProperties modelProperties
) {

    public static JsonDeserializer<MinecraftGeometry> deserializer() throws JsonParseException {
        return (json, type, context) -> {
            JsonObject obj = json.getAsJsonObject();
            Bone[] bones = JsonUtil.jsonArrayToObjectArray(
                    GsonHelper.getAsJsonArray(obj, "bones", new JsonArray(0)),
                    context,
                    Bone.class
            );
            String cape = GsonHelper.getAsString(obj, "cape", null);
            ModelProperties modelProperties = GsonHelper.getAsObject(
                    obj,
                    "description",
                    null,
                    context,
                    ModelProperties.class
            );

            return new MinecraftGeometry(bones, cape, modelProperties);
        };
    }
}
