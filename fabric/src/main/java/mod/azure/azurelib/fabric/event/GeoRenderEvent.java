/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.fabric.event;

import com.mojang.blaze3d.vertex.PoseStack;
import mod.azure.azurelib.common.api.client.renderer.*;
import mod.azure.azurelib.common.api.client.renderer.layer.GeoRenderLayer;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import mod.azure.azurelib.common.api.common.animatable.GeoReplacedEntity;
import mod.azure.azurelib.common.internal.client.renderer.GeoRenderer;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

public interface GeoRenderEvent {
    
    /**
     * Returns the renderer for this event
     *
     * @see GeoArmorRenderer GeoArmorRenderer
     * @see GeoBlockRenderer GeoBlockRenderer
     * @see GeoEntityRenderer GeoEntityRenderer
     * @see GeoItem GeoItem
     * @see GeoObjectRenderer GeoObjectRenderer
     * @see GeoReplacedEntityRenderer GeoReplacedEntityRenderer
     */
    GeoRenderer<?> getRenderer();

    /**
     * Renderer events for armor pieces being rendered by {@link GeoArmorRenderer}
     */
    abstract class Armor implements GeoRenderEvent {
        private final GeoArmorRenderer<?> renderer;

        public Armor(GeoArmorRenderer<?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoArmorRenderer<?> getRenderer() {
            return this.renderer;
        }

        /**
         * Shortcut method for retrieving the entity being rendered
         */
        @Nullable
        public net.minecraft.world.entity.Entity getEntity() {
            return getRenderer().getCurrentEntity();
        }

        /**
         * Shortcut method for retrieving the ItemStack relevant to the armor piece being rendered
         */
        @Nullable
        public ItemStack getItemStack() {
            return getRenderer().getCurrentStack();
        }

        /**
         * Shortcut method for retrieving the equipped slot of the armor piece being rendered
         */
        @Nullable
        public EquipmentSlot getEquipmentSlot() {
            return getRenderer().getCurrentSlot();
        }

        /**
         * Pre-render event for armor pieces being rendered by {@link GeoArmorRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the armor piece will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends Armor {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoArmorRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Armor.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for armor pieces being rendered by {@link GeoEntityRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends Armor {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoArmorRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Armor.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoArmorRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends Armor {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoArmorRenderer<?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the Armor.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }

    /**
     * Renderer events for {@link BlockEntity BlockEntities} being rendered by {@link GeoBlockRenderer}
     */
    abstract class Block implements GeoRenderEvent {
        private final GeoBlockRenderer<?> renderer;

        public Block(GeoBlockRenderer<?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoBlockRenderer<?> getRenderer() {
            return this.renderer;
        }

        /**
         * Shortcut method for retrieving the block entity being rendered
         */
        public BlockEntity getBlockEntity() {
            return getRenderer().getAnimatable();
        }

        /**
         * Pre-render event for block entities being rendered by {@link GeoBlockRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the block entity will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends Block {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoBlockRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Block.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for block entities being rendered by {@link GeoBlockRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends Block {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoBlockRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Block.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoBlockRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends Block {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoBlockRenderer<?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the Armor.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }

    /**
     * Renderer events for {@link net.minecraft.world.entity.Entity Entities} being rendered by {@link GeoEntityRenderer}
     */
    abstract class Entity implements GeoRenderEvent {
        private final GeoEntityRenderer<?> renderer;

        public Entity(GeoEntityRenderer<?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoEntityRenderer<?> getRenderer() {
            return this.renderer;
        }

        /**
         * Shortcut method for retrieving the entity being rendered
         */
        public net.minecraft.world.entity.Entity getEntity() {
            return this.renderer.getAnimatable();
        }

        /**
         * Pre-render event for entities being rendered by {@link GeoEntityRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the entity will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends Entity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoEntityRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Armor.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for entities being rendered by {@link GeoEntityRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends Entity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoEntityRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Entity.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoEntityRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends Entity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoEntityRenderer<?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the Entity.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }

    /**
     * Renderer events for {@link ItemStack Items} being rendered by {@link GeoItemRenderer}
     */
    abstract class Item implements GeoRenderEvent {
        private final GeoItemRenderer<?> renderer;

        public Item(GeoItemRenderer<?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoItemRenderer<?> getRenderer() {
            return this.renderer;
        }

        /**
         * Shortcut method for retrieving the ItemStack being rendered
         */
        public ItemStack getItemStack() {
            return getRenderer().getCurrentItemStack();
        }

        /**
         * Pre-render event for armor being rendered by {@link GeoItemRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the ItemStack will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends Item {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoItemRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Item.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for ItemStacks being rendered by {@link GeoItemRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends Item {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoItemRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Item.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoItemRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends Item {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoItemRenderer<?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the Item.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }

    /**
     * Renderer events for miscellaneous {@link GeoAnimatable animatables} being rendered by {@link GeoObjectRenderer}
     */
    abstract class Object implements GeoRenderEvent {
        private final GeoObjectRenderer<?> renderer;

        public Object(GeoObjectRenderer<?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoObjectRenderer<?> getRenderer() {
            return this.renderer;
        }

        /**
         * Pre-render event for miscellaneous animatables being rendered by {@link GeoObjectRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the object will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends Object {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoObjectRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Object.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for miscellaneous animatables being rendered by {@link GeoObjectRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends Object {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoObjectRenderer<?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the Object.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoObjectRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends Object {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoObjectRenderer<?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the Object.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }

    /**
     * Renderer events for miscellaneous {@link GeoReplacedEntity replaced entities} being rendered by {@link GeoReplacedEntityRenderer}
     */
    abstract class ReplacedEntity implements GeoRenderEvent {
        private final GeoReplacedEntityRenderer<?, ?> renderer;

        public ReplacedEntity(GeoReplacedEntityRenderer<?, ?> renderer) {
            this.renderer = renderer;
        }

        /**
         * Returns the renderer for this event
         */
        @Override
        public GeoReplacedEntityRenderer<?, ?> getRenderer() {
            return this.renderer;
        }

        /**
         * Shortcut method to get the Entity currently being rendered
         */
        public net.minecraft.world.entity.Entity getReplacedEntity() {
            return getRenderer().getCurrentEntity();
        }

        /**
         * Pre-render event for replaced entities being rendered by {@link GeoReplacedEntityRenderer}
         * <p>
         * This event is called before rendering, but after {@link GeoRenderer#preRender}
         * <p>
         * This event is <u>cancellable</u><br>
         * If the event is cancelled by returning false in the {@link Listener}, the entity will not be rendered and the corresponding {@link Post} event will not be fired.
         */
        public static class Pre extends ReplacedEntity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, event -> true, listeners -> event -> {
                for (Listener listener : listeners) {
                    if (!listener.handle(event))
                        return false;
                }

                return true;
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Pre(GeoReplacedEntityRenderer<?, ?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the ReplacedEntity.Pre GeoRenderEvent
             * <p>
             * Return false to cancel the render pass
             */
            @FunctionalInterface
            public interface Listener {
                boolean handle(Pre event);
            }
        }

        /**
         * Post-render event for replaced entities being rendered by {@link GeoReplacedEntityRenderer}
         * <p>
         * This event is called after {@link GeoRenderer#postRender}
         */
        public static class Post extends ReplacedEntity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            private final PoseStack poseStack;
            private final BakedGeoModel model;
            private final MultiBufferSource bufferSource;
            private final float partialTick;
            private final int packedLight;

            public Post(GeoReplacedEntityRenderer<?, ?> renderer, PoseStack poseStack, BakedGeoModel model, MultiBufferSource bufferSource, float partialTick, int packedLight) {
                super(renderer);

                this.poseStack = poseStack;
                this.model = model;
                this.bufferSource = bufferSource;
                this.partialTick = partialTick;
                this.packedLight = packedLight;
            }

            public PoseStack getPoseStack() {
                return this.poseStack;
            }

            public BakedGeoModel getModel() {
                return this.model;
            }

            public MultiBufferSource getBufferSource() {
                return this.bufferSource;
            }

            public float getPartialTick() {
                return this.partialTick;
            }

            public int getPackedLight() {
                return this.packedLight;
            }

            /**
             * Event listener interface for the ReplacedEntity.Post GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(Post event);
            }
        }

        /**
         * One-time event for a {@link GeoReplacedEntityRenderer} called on first initialisation
         * <p>
         * Use this event to add render layers to the renderer as needed
         */
        public static class CompileRenderLayers extends ReplacedEntity {
            public static final Event<Listener> EVENT = EventFactory.createArrayBacked(Listener.class, post -> {}, listeners -> event -> {
                for (Listener listener : listeners) {
                    listener.handle(event);
                }
            });

            public CompileRenderLayers(GeoReplacedEntityRenderer<?, ?> renderer) {
                super(renderer);
            }

            /**
             * Adds a {@link GeoRenderLayer} to the renderer
             * <p>
             * Type-safety is not checked here, so ensure that your layer is compatible with this animatable and renderer
             */
            public void addLayer(GeoRenderLayer renderLayer) {
                getRenderer().addRenderLayer(renderLayer);
            }

            /**
             * Event listener interface for the ReplacedEntity.CompileRenderLayers GeoRenderEvent
             */
            @FunctionalInterface
            public interface Listener {
                void handle(CompileRenderLayers event);
            }
        }
    }
}
