/**
 * This class is a fork of the matching class found in the Configuration repository.
 * Original source: https://github.com/Toma1O6/Configuration
 * Copyright © 2024 Toma1O6.
 * Licensed under the MIT License.
 */
package mod.azure.azurelib.common.internal.common.config.value;

import mod.azure.azurelib.common.internal.common.config.adapter.TypeAdapter;
import mod.azure.azurelib.common.internal.common.config.exception.ConfigValueMissingException;
import mod.azure.azurelib.common.internal.common.config.format.IConfigFormat;
import net.minecraft.network.FriendlyByteBuf;

import java.lang.reflect.Field;

public final class CharValue extends ConfigValue<Character> {

    public CharValue(ValueData<Character> valueData) {
        super(valueData);
    }

    @Override
    protected void serialize(IConfigFormat format) {
        format.writeChar(this.getId(), this.get());
    }

    @Override
    protected void deserialize(IConfigFormat format) throws ConfigValueMissingException {
        this.set(format.readChar(this.getId()));
    }

    public static final class Adapter extends TypeAdapter {

        @Override
        public ConfigValue<?> serialize(
                String name,
                String[] comments,
                Object value,
                TypeSerializer serializer,
                AdapterContext context
        ) throws IllegalAccessException {
            return new CharValue(ValueData.of(name, (char) value, context, comments));
        }

        @Override
        public void encodeToBuffer(ConfigValue<?> value, FriendlyByteBuf buffer) {
            buffer.writeChar((Integer) value.get());
        }

        @Override
        public Object decodeFromBuffer(ConfigValue<?> value, FriendlyByteBuf buffer) {
            return buffer.readChar();
        }

        @Override
        public void setFieldValue(Field field, Object instance, Object value) throws IllegalAccessException {
            field.setChar(instance, (char) value);
        }
    }
}
