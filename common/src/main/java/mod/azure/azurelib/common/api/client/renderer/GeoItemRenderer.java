/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.api.client.renderer;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.client.renderer.layer.GeoRenderLayer;
import mod.azure.azurelib.common.api.client.renderer.layer.GeoRenderLayersContainer;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import mod.azure.azurelib.common.internal.client.renderer.GeoRenderer;
import mod.azure.azurelib.common.internal.client.util.RenderUtils;
import mod.azure.azurelib.common.internal.common.cache.object.BakedGeoModel;
import mod.azure.azurelib.common.internal.common.cache.object.GeoBone;
import mod.azure.azurelib.common.internal.common.cache.texture.AnimatableTexture;
import mod.azure.azurelib.common.internal.common.constant.DataTickets;
import mod.azure.azurelib.common.platform.Services;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.core.animation.AnimationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Base {@link GeoRenderer} class for rendering {@link Item Items} specifically.<br>
 * All items added to be rendered by AzureLib should use an instance of this class.
 */
public class GeoItemRenderer<T extends Item & GeoAnimatable> extends BlockEntityWithoutLevelRenderer implements GeoRenderer<T> {

    protected final GeoRenderLayersContainer<T> renderLayers = new GeoRenderLayersContainer<>(this);

    protected final GeoModel<T> model;

    protected ItemStack currentItemStack;

    protected ItemDisplayContext renderPerspective;

    protected T animatable;

    protected float scaleWidth = 1;

    protected float scaleHeight = 1;

    protected boolean useEntityGuiLighting = false;

    protected Matrix4f itemRenderTranslations = new Matrix4f();

    protected Matrix4f modelRenderTranslations = new Matrix4f();

    public GeoItemRenderer(GeoModel<T> model) {
        this(
                Minecraft.getInstance().getBlockEntityRenderDispatcher(),
                Minecraft.getInstance().getEntityModels(),
                model
        );
    }

    public GeoItemRenderer(BlockEntityRenderDispatcher dispatcher, EntityModelSet modelSet, GeoModel<T> model) {
        super(dispatcher, modelSet);

        this.model = model;
    }

    /**
     * Gets the model instance for this renderer
     */
    @Override
    public GeoModel<T> getGeoModel() {
        return this.model;
    }

    /**
     * Gets the {@link GeoAnimatable} instance currently being rendered
     */
    @Override
    public T getAnimatable() {
        return this.animatable;
    }

    /**
     * Returns the current ItemStack being rendered
     */
    public ItemStack getCurrentItemStack() {
        return this.currentItemStack;
    }

    /**
     * Mark this renderer so that it uses an alternate lighting scheme when rendering the item in GUI
     * <p>
     * This can help with improperly lit 3d models
     */
    public GeoItemRenderer<T> useAlternateGuiLighting() {
        this.useEntityGuiLighting = true;

        return this;
    }

    /**
     * Gets the id that represents the current animatable's instance for animation purposes. This is mostly useful for
     * things like items, which have a single registered instance for all objects
     */
    @Override
    public long getInstanceId(T animatable) {
        return GeoItem.getId(this.currentItemStack);
    }

    /**
     * Shadowing override of {@link EntityRenderer#getTextureLocation}.<br>
     * This redirects the call to {@link GeoRenderer#getTextureLocation}
     */
    @Override
    public ResourceLocation getTextureLocation(T animatable) {
        return GeoRenderer.super.getTextureLocation(animatable);
    }

    /**
     * Returns the list of registered {@link GeoRenderLayer GeoRenderLayers} for this renderer
     */
    @Override
    public List<GeoRenderLayer<T>> getRenderLayers() {
        return this.renderLayers.getRenderLayers();
    }

    /**
     * Adds a {@link GeoRenderLayer} to this renderer, to be called after the main model is rendered each frame
     */
    public GeoItemRenderer<T> addRenderLayer(GeoRenderLayer<T> renderLayer) {
        this.renderLayers.addLayer(renderLayer);

        return this;
    }

    /**
     * Sets a scale override for this renderer, telling AzureLib to pre-scale the model
     */
    public GeoItemRenderer<T> withScale(float scale) {
        return withScale(scale, scale);
    }

    /**
     * Sets a scale override for this renderer, telling AzureLib to pre-scale the model
     */
    public GeoItemRenderer<T> withScale(float scaleWidth, float scaleHeight) {
        this.scaleWidth = scaleWidth;
        this.scaleHeight = scaleHeight;

        return this;
    }

    /**
     * Called before rendering the model to buffer. Allows for render modifications and preparatory work such as scaling
     * and translating.<br>
     * {@link PoseStack} translations made here are kept until the end of the render process
     */
    @Override
    public void preRender(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        this.itemRenderTranslations = new Matrix4f(poseStack.last().pose());

        scaleModelForRender(
                this.scaleWidth,
                this.scaleHeight,
                poseStack,
                animatable,
                model,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay
        );

        if (!isReRender)
            poseStack.translate(0.5f, 0.51f, 0.5f);
    }

    @Override
    public void renderByItem(
            ItemStack stack,
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        this.animatable = (T) stack.getItem();
        this.currentItemStack = stack;
        this.renderPerspective = transformType;

        if (transformType == ItemDisplayContext.GUI) {
            renderInGui(transformType, poseStack, bufferSource, packedLight, packedOverlay);
        } else {
            RenderType renderType = getRenderType(
                    this.animatable,
                    getTextureLocation(this.animatable),
                    bufferSource,
                    Minecraft.getInstance().getTimer().getGameTimeDeltaTicks()
            );
            VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(
                    bufferSource,
                    renderType,
                    false,
                    this.currentItemStack != null && this.currentItemStack.hasFoil()
            );

            defaultRender(
                    poseStack,
                    this.animatable,
                    bufferSource,
                    renderType,
                    buffer,
                    0,
                    Minecraft.getInstance().getTimer().getGameTimeDeltaTicks(),
                    packedLight
            );
        }
    }

    /**
     * Wrapper method to handle rendering the item in a GUI context (defined by
     * {@link net.minecraft.world.item.ItemDisplayContext#GUI} normally).<br>
     * Just includes some additional required transformations and settings.
     */
    protected void renderInGui(
            ItemDisplayContext transformType,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay
    ) {
        if (this.useEntityGuiLighting) {
            Lighting.setupForEntityInInventory();
        }
        else {
            Lighting.setupForFlatItems();
        }
        MultiBufferSource.BufferSource defaultBufferSource =
                bufferSource instanceof MultiBufferSource.BufferSource bufferSource2
                        ? bufferSource2
                        : Minecraft.getInstance().levelRenderer.renderBuffers.bufferSource();
        RenderType renderType = getRenderType(
                this.animatable,
                getTextureLocation(this.animatable),
                defaultBufferSource,
                Minecraft.getInstance().getTimer().getGameTimeDeltaTicks()
        );
        VertexConsumer buffer = ItemRenderer.getFoilBufferDirect(
                bufferSource,
                renderType,
                true,
                this.currentItemStack != null && this.currentItemStack.hasFoil()
        );

        poseStack.pushPose();
        defaultRender(
                poseStack,
                this.animatable,
                defaultBufferSource,
                renderType,
                buffer,
                0,
                Minecraft.getInstance().getTimer().getGameTimeDeltaTicks(),
                packedLight
        );
        defaultBufferSource.endBatch();
        RenderSystem.enableDepthTest();
        Lighting.setupFor3DItems();
        poseStack.popPose();
    }

    /**
     * The actual render method that subtype renderers should override to handle their specific rendering tasks.<br>
     * {@link GeoRenderer#preRender} has already been called by this stage, and {@link GeoRenderer#postRender} will be
     * called directly after
     */
    @Override
    public void actuallyRender(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        if (!isReRender) {
            AnimationState<T> animationState = new AnimationState<>(animatable, 0, 0, partialTick, false);
            long instanceId = getInstanceId(animatable);

            animationState.setData(DataTickets.TICK, animatable.getTick(this.currentItemStack));
            animationState.setData(DataTickets.ITEM_RENDER_PERSPECTIVE, this.renderPerspective);
            animationState.setData(DataTickets.ITEMSTACK, this.currentItemStack);
            animatable.getAnimatableInstanceCache()
                    .getManagerForId(instanceId)
                    .setData(DataTickets.ITEM_RENDER_PERSPECTIVE, this.renderPerspective);
            this.model.addAdditionalStateData(animatable, instanceId, animationState::setData);
            this.model.handleAnimations(animatable, instanceId, animationState);
        }

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        GeoRenderer.super.actuallyRender(
                poseStack,
                animatable,
                model,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                colour
        );
    }

    /**
     * Renders the provided {@link GeoBone} and its associated child bones
     */
    @Override
    public void renderRecursively(
            PoseStack poseStack,
            T animatable,
            GeoBone bone,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            boolean isReRender,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.itemRenderTranslations);

            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(
                    RenderUtils.translateMatrix(localMatrix, getRenderOffset(this.animatable, 1).toVector3f())
            );
        }

        GeoRenderer.super.renderRecursively(
                poseStack,
                animatable,
                bone,
                renderType,
                bufferSource,
                buffer,
                isReRender,
                partialTick,
                packedLight,
                packedOverlay,
                colour
        );
    }

    public Vec3 getRenderOffset(Item entity, float f) {
        return Vec3.ZERO;
    }

    /**
     * Update the current frame of a {@link AnimatableTexture potentially animated} texture used by this
     * GeoRenderer.<br>
     * This should only be called immediately prior to rendering, and only
     *
     * @see AnimatableTexture#setAndUpdate(ResourceLocation, int)
     */
    @Override
    public void updateAnimatedTextureFrame(T animatable) {
        AnimatableTexture.setAndUpdate(
                getTextureLocation(animatable),
                Item.getId(animatable) + (int) animatable.getTick(animatable)
        );
    }

    /**
     * Create and fire the relevant {@code CompileLayers} event hook for this renderer
     */
    @Override
    public void fireCompileRenderLayersEvent() {
        Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireCompileItemRenderLayers(this);
    }

    /**
     * Create and fire the relevant {@code Pre-Render} event hook for this renderer.<br>
     *
     * @return Whether the renderer should proceed based on the cancellation state of the event
     */
    @Override
    public boolean firePreRenderEvent(
            PoseStack poseStack,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight
    ) {
        return Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireItemPreRender(this, poseStack, model, bufferSource, partialTick, packedLight);
    }

    /**
     * Create and fire the relevant {@code Post-Render} event hook for this renderer
     */
    @Override
    public void firePostRenderEvent(
            PoseStack poseStack,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            float partialTick,
            int packedLight
    ) {
        Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireItemPostRender(this, poseStack, model, bufferSource, partialTick, packedLight);
    }
}
