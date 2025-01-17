/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
/*
 * Copyright (c) 2020. Author: Bernie G. (Gecko)
 */

package mod.azure.azurelib.core.keyframe;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.azurelib.core.animation.EasingType;
import mod.azure.azurelib.core.math.IValue;

import java.util.List;
import java.util.Objects;

/**
 * Animation keyframe data
 *
 * @param length     The length (in ticks) the keyframe lasts for
 * @param startValue The value to start the keyframe's transformation with
 * @param endValue   The value to end the keyframe's transformation with
 * @param easingType The {@code EasingType} to use for transformations
 * @param easingArgs The arguments to provide to the easing calculation
 */
public record Keyframe<T extends IValue>(
        double length,
        T startValue,
        T endValue,
        EasingType easingType,
        List<T> easingArgs
) {

    public Keyframe(double length, T startValue, T endValue) {
        this(length, startValue, endValue, EasingType.LINEAR);
    }

    public Keyframe(double length, T startValue, T endValue, EasingType easingType) {
        this(length, startValue, endValue, easingType, new ObjectArrayList<>(0));
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.length, this.startValue, this.endValue, this.easingType, this.easingArgs);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;

        if (obj == null || getClass() != obj.getClass())
            return false;

        return hashCode() == obj.hashCode();
    }
}
