/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.api.common.animatable;

import com.google.common.base.Suppliers;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.AzureLib;
import mod.azure.azurelib.common.internal.common.animatable.SingletonGeoAnimatable;
import mod.azure.azurelib.common.internal.common.cache.AnimatableIdCache;
import mod.azure.azurelib.common.internal.common.constant.DataTickets;
import mod.azure.azurelib.common.platform.Services;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.core.animatable.instance.AnimatableInstanceCache;
import mod.azure.azurelib.core.animatable.instance.SingletonAnimatableInstanceCache;
import mod.azure.azurelib.core.animation.AnimatableManager;
import mod.azure.azurelib.core.animation.ContextAwareAnimatableManager;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * The {@link GeoAnimatable GeoAnimatable} interface specific to {@link net.minecraft.world.item.Item Items}. This also
 * applies to armor, as they are just items too.
 */
public interface GeoItem extends SingletonGeoAnimatable {

    String ID_NBT_KEY = "AzureLibID";

    /**
     * Safety wrapper to distance the client-side code from common code.<br>
     * This should be cached in your {@link net.minecraft.world.item.Item Item} class
     */
    static Supplier<Object> makeRenderer(GeoItem item) {
        if (Services.PLATFORM.isServerEnvironment())
            return () -> null;

        return Suppliers.memoize(() -> {
            AtomicReference<Object> renderProvider = new AtomicReference<>();
            item.createRenderer(renderProvider::set);
            return renderProvider.get();
        });
    }

    /**
     * Register this as a synched {@code GeoAnimatable} instance with AzureLib's networking functions
     * <p>
     * This should be called inside the constructor of your object.
     */
    static void registerSyncedAnimatable(GeoAnimatable animatable) {
        SingletonGeoAnimatable.registerSyncedAnimatable(animatable);
    }

    /**
     * Gets the unique identifying number from this ItemStack's {@link net.minecraft.nbt.Tag NBT}, or
     * {@link Long#MAX_VALUE} if one hasn't been assigned
     */
    static long getId(ItemStack stack) {
        return Optional.ofNullable(stack.getComponentsPatch().get(AzureLib.STACK_ANIMATABLE_ID_COMPONENT.get()))
                .filter(Optional::isPresent)
                .<Long>map(Optional::get)
                .orElse(Long.MAX_VALUE);
    }

    /**
     * Gets the unique identifying number from this ItemStack's {@link net.minecraft.nbt.Tag NBT}.<br>
     * If no ID has been reserved for this stack yet, it will reserve a new id and assign it
     */
    static long getOrAssignId(ItemStack stack, ServerLevel level) {
        if (!(stack.getComponents() instanceof PatchedDataComponentMap components))
            return Long.MAX_VALUE;

        Long id = components.get(AzureLib.STACK_ANIMATABLE_ID_COMPONENT.get());

        if (id == null)
            components.set(AzureLib.STACK_ANIMATABLE_ID_COMPONENT.get(), id = AnimatableIdCache.getFreeId(level));
        return id;
    }

    /**
     * Returns the current age/tick of the animatable instance.<br>
     * By default this is just the animatable's age in ticks, but this method allows for non-ticking custom animatables
     * to provide their own values
     *
     * @param itemStack The ItemStack representing this animatable
     * @return The current tick/age of the animatable, for animation purposes
     */
    @Override
    default double getTick(Object itemStack) {
        return RenderUtils.getCurrentTick();
    }

    /**
     * Whether this item animatable is perspective aware, handling animations differently depending on the
     * {@link net.minecraft.world.item.ItemDisplayContext render perspective}
     */
    default boolean isPerspectiveAware() {
        return false;
    }

    /**
     * Replaces the default AnimatableInstanceCache for GeoItems if {@link GeoItem#isPerspectiveAware()} is true, for
     * perspective-dependent handling
     */
    @Nullable
    @Override
    default AnimatableInstanceCache animatableCacheOverride() {
        if (isPerspectiveAware())
            return new ContextBasedAnimatableInstanceCache(this);

        return SingletonGeoAnimatable.super.animatableCacheOverride();
    }

    /**
     * AnimatableInstanceCache specific to GeoItems, for doing render perspective based animations
     */
    class ContextBasedAnimatableInstanceCache extends SingletonAnimatableInstanceCache {

        public ContextBasedAnimatableInstanceCache(GeoAnimatable animatable) {
            super(animatable);
        }

        /**
         * Gets an {@link AnimatableManager} instance from this cache, cached under the id provided, or a new one if one
         * doesn't already exist.<br>
         * This subclass assumes that all animatable instances will be sharing this cache instance, and so
         * differentiates data by ids.
         */
        @Override
        public AnimatableManager<?> getManagerForId(long uniqueId) {
            if (!this.managers.containsKey(uniqueId))
                this.managers.put(
                        uniqueId,
                        new ContextAwareAnimatableManager<GeoItem, ItemDisplayContext>(this.animatable) {

                            @Override
                            protected Map<ItemDisplayContext, AnimatableManager<GeoItem>> buildContextOptions(
                                    GeoAnimatable animatable
                            ) {
                                Map<ItemDisplayContext, AnimatableManager<GeoItem>> map = new EnumMap<>(
                                        ItemDisplayContext.class
                                );

                                for (ItemDisplayContext context : ItemDisplayContext.values()) {
                                    map.put(context, new AnimatableManager<>(animatable));
                                }

                                return map;
                            }

                            @Override
                            public ItemDisplayContext getCurrentContext() {
                                ItemDisplayContext context = getData(DataTickets.ITEM_RENDER_PERSPECTIVE);

                                return context == null ? ItemDisplayContext.NONE : context;
                            }
                        }
                );

            return this.managers.get(uniqueId);
        }
    }
}
