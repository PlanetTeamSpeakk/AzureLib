/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.api.client.renderer;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import mod.azure.azurelib.common.api.client.model.GeoModel;
import mod.azure.azurelib.common.api.client.renderer.layer.GeoRenderLayer;
import mod.azure.azurelib.common.internal.client.model.data.EntityModelData;
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
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;

import java.util.List;

/**
 * Base {@link GeoRenderer} class for rendering {@link Entity Entities} specifically.<br>
 * All entities added to be rendered by AzureLib should use an instance of this class.<br>
 * This also includes {@link net.minecraft.world.entity.projectile.Projectile Projectiles}
 */
public class GeoEntityRenderer<T extends Entity & GeoAnimatable> extends EntityRenderer<T> implements GeoRenderer<T> {

    protected final List<GeoRenderLayer<T>> renderLayers = new ObjectArrayList<>();

    protected final GeoModel<T> model;

    protected T animatable;

    protected float scaleWidth = 1;

    protected float scaleHeight = 1;

    protected Matrix4f entityRenderTranslations = new Matrix4f();

    protected Matrix4f modelRenderTranslations = new Matrix4f();

    public GeoEntityRenderer(EntityRendererProvider.Context renderManager, GeoModel<T> model) {
        super(renderManager);

        this.model = model;
    }

    /**
     * Static rendering code for rendering a leash segment.<br>
     * It's a like-for-like from {@link net.minecraft.client.renderer.entity.MobRenderer#addVertexPair} that had to be
     * duplicated here for flexible usage
     */
    private static void renderLeashPiece(
            VertexConsumer buffer,
            Matrix4f positionMatrix,
            float xDif,
            float yDif,
            float zDif,
            int entityBlockLight,
            int holderBlockLight,
            int entitySkyLight,
            int holderSkyLight,
            float width,
            float yOffset,
            float xOffset,
            float zOffset,
            int segment,
            boolean isLeashKnot
    ) {
        float piecePosPercent = segment / 24f;
        int lerpBlockLight = (int) Mth.lerp(piecePosPercent, entityBlockLight, holderBlockLight);
        int lerpSkyLight = (int) Mth.lerp(piecePosPercent, entitySkyLight, holderSkyLight);
        int packedLight = LightTexture.pack(lerpBlockLight, lerpSkyLight);
        float knotColourMod = segment % 2 == (isLeashKnot ? 1 : 0) ? 0.7f : 1f;
        float red = 0.5f * knotColourMod;
        float green = 0.4f * knotColourMod;
        float blue = 0.3f * knotColourMod;
        float x = xDif * piecePosPercent;
        float y = yDif > 0.0f
                ? yDif * piecePosPercent * piecePosPercent
                : yDif - yDif * (1.0f - piecePosPercent) * (1.0f - piecePosPercent);
        float z = zDif * piecePosPercent;

        buffer.addVertex(positionMatrix, x - xOffset, y + yOffset, z + zOffset)
                .setColor(red, green, blue, 1)
                .setLight(
                        packedLight
                );
        buffer.addVertex(positionMatrix, x + xOffset, y + width - yOffset, z - zOffset)
                .setColor(red, green, blue, 1)
                .setLight(
                        packedLight
                );
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
     * Gets the id that represents the current animatable's instance for animation purposes. This is mostly useful for
     * things like items, which have a single registered instance for all objects
     */
    @Override
    public long getInstanceId(T animatable) {
        return animatable.getId();
    }

    /**
     * Shadowing override of {@link EntityRenderer#getTextureLocation}.<br>
     * This redirects the call to {@link GeoRenderer#getTextureLocation}
     */
    @Override
    public @NotNull ResourceLocation getTextureLocation(@NotNull T animatable) {
        return GeoRenderer.super.getTextureLocation(animatable);
    }

    /**
     * Returns the list of registered {@link GeoRenderLayer GeoRenderLayers} for this renderer
     */
    @Override
    public List<GeoRenderLayer<T>> getRenderLayers() {
        return this.renderLayers;
    }

    /**
     * Adds a {@link GeoRenderLayer} to this renderer, to be called after the main model is rendered each frame
     */
    public GeoEntityRenderer<T> addRenderLayer(GeoRenderLayer<T> renderLayer) {
        this.renderLayers.add(renderLayer);

        return this;
    }

    /**
     * Sets a scale override for this renderer, telling AzureLib to pre-scale the model
     */
    public GeoEntityRenderer<T> withScale(float scale) {
        return withScale(scale, scale);
    }

    /**
     * Sets a scale override for this renderer, telling AzureLib to pre-scale the model
     */
    public GeoEntityRenderer<T> withScale(float scaleWidth, float scaleHeight) {
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
        this.entityRenderTranslations = new Matrix4f(poseStack.last().pose());

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
    }

    @Override
    public void render(
            T entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        this.animatable = entity;

        defaultRender(poseStack, entity, bufferSource, null, null, entityYaw, partialTick, packedLight);
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
        poseStack.pushPose();

        LivingEntity livingEntity = animatable instanceof LivingEntity entity ? entity : null;

        boolean shouldSit = animatable.isPassenger() && (animatable.getVehicle() != null);
        float lerpBodyRot = livingEntity == null
                ? 0
                : Mth.rotLerp(
                partialTick,
                livingEntity.yBodyRotO,
                livingEntity.yBodyRot
        );
        float lerpHeadRot = livingEntity == null
                ? 0
                : Mth.rotLerp(
                partialTick,
                livingEntity.yHeadRotO,
                livingEntity.yHeadRot
        );
        float netHeadYaw = lerpHeadRot - lerpBodyRot;

        if (shouldSit && animatable.getVehicle() instanceof LivingEntity livingentity) {
            lerpBodyRot = Mth.rotLerp(partialTick, livingentity.yBodyRotO, livingentity.yBodyRot);
            netHeadYaw = lerpHeadRot - lerpBodyRot;
            float clampedHeadYaw = Mth.clamp(Mth.wrapDegrees(netHeadYaw), -85, 85);
            lerpBodyRot = lerpHeadRot - clampedHeadYaw;

            if (clampedHeadYaw * clampedHeadYaw > 2500f)
                lerpBodyRot += clampedHeadYaw * 0.2f;

            netHeadYaw = lerpHeadRot - lerpBodyRot;
        }

        if (animatable.getPose() == Pose.SLEEPING && livingEntity != null) {
            Direction bedDirection = livingEntity.getBedOrientation();

            if (bedDirection != null) {
                float eyePosOffset = livingEntity.getEyeHeight(Pose.STANDING) - 0.1F;

                poseStack.translate(
                        -bedDirection.getStepX() * eyePosOffset,
                        0,
                        -bedDirection.getStepZ() * eyePosOffset
                );
            }
        }

        float nativeScale = livingEntity != null ? livingEntity.getScale() : 1;
        float ageInTicks = animatable.tickCount + partialTick;
        float limbSwingAmount = 0;
        float limbSwing = 0;

        poseStack.scale(nativeScale, nativeScale, nativeScale);
        applyRotations(animatable, poseStack, ageInTicks, lerpBodyRot, partialTick, nativeScale);

        if (!shouldSit && animatable.isAlive() && livingEntity != null) {
            limbSwingAmount = Mth.lerp(
                    partialTick,
                    livingEntity.walkAnimation.speedOld,
                    livingEntity.walkAnimation.speed()
            );
            limbSwing = livingEntity.walkAnimation.position() - livingEntity.walkAnimation.speed() * (1 - partialTick);

            if (livingEntity.isBaby())
                limbSwing *= 3f;

            if (limbSwingAmount > 1f)
                limbSwingAmount = 1f;
        }

        if (!isReRender) {
            float headPitch = Mth.lerp(partialTick, animatable.xRotO, animatable.getXRot());
            float motionThreshold = getMotionAnimThreshold(animatable);
            Vec3 velocity = animatable.getDeltaMovement();
            float avgVelocity = (float) (Math.abs(velocity.x) + Math.abs(velocity.z) / 2f);
            AnimationState<T> animationState = new AnimationState<T>(
                    animatable,
                    limbSwing,
                    limbSwingAmount,
                    partialTick,
                    avgVelocity >= motionThreshold && limbSwingAmount != 0
            );
            long instanceId = getInstanceId(animatable);

            animationState.setData(DataTickets.TICK, animatable.getTick(animatable));
            animationState.setData(DataTickets.ENTITY, animatable);
            animationState.setData(
                    DataTickets.ENTITY_MODEL_DATA,
                    new EntityModelData(
                            shouldSit,
                            livingEntity != null && livingEntity.isBaby(),
                            -netHeadYaw,
                            -headPitch
                    )
            );
            this.model.addAdditionalStateData(animatable, instanceId, animationState::setData);
            this.model.handleAnimations(animatable, instanceId, animationState);
        }

        this.modelRenderTranslations = new Matrix4f(poseStack.last().pose());

        if (!animatable.isInvisibleTo(Minecraft.getInstance().player))
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

        poseStack.popPose();
    }

    /**
     * Render the various {@link GeoRenderLayer RenderLayers} that have been registered to this renderer
     */
    @Override
    public void applyRenderLayers(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel model,
            RenderType renderType,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay
    ) {
        if (!animatable.isSpectator())
            GeoRenderer.super.applyRenderLayers(
                    poseStack,
                    animatable,
                    model,
                    renderType,
                    bufferSource,
                    buffer,
                    partialTick,
                    packedLight,
                    packedOverlay
            );
    }

    /**
     * Call after all other rendering work has taken place, including reverting the {@link PoseStack}'s state. This
     * method is <u>not</u> called in {@link GeoRenderer#reRender re-render}
     */
    @Override
    public void renderFinal(
            PoseStack poseStack,
            T animatable,
            BakedGeoModel model,
            MultiBufferSource bufferSource,
            VertexConsumer buffer,
            float partialTick,
            int packedLight,
            int packedOverlay,
            int colour
    ) {
        super.render(animatable, 0, partialTick, poseStack, bufferSource, packedLight);

        if (animatable instanceof Mob mob) {
            Entity leashHolder = mob.getLeashHolder();

            if (leashHolder != null)
                renderLeash(mob, partialTick, poseStack, bufferSource, leashHolder);
        }
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
        poseStack.pushPose();
        RenderUtils.translateMatrixToBone(poseStack, bone);
        RenderUtils.translateToPivotPoint(poseStack, bone);
        RenderUtils.rotateMatrixAroundBone(poseStack, bone);
        RenderUtils.scaleMatrixForBone(poseStack, bone);

        if (bone.isTrackingMatrices()) {
            Matrix4f poseState = new Matrix4f(poseStack.last().pose());
            Matrix4f localMatrix = RenderUtils.invertAndMultiplyMatrices(poseState, this.entityRenderTranslations);

            bone.setModelSpaceMatrix(RenderUtils.invertAndMultiplyMatrices(poseState, this.modelRenderTranslations));
            bone.setLocalSpaceMatrix(
                    RenderUtils.translateMatrix(localMatrix, getRenderOffset(this.animatable, 1).toVector3f())
            );
            bone.setWorldSpaceMatrix(
                    RenderUtils.translateMatrix(new Matrix4f(localMatrix), this.animatable.position().toVector3f())
            );
        }

        RenderUtils.translateAwayFromPivotPoint(poseStack, bone);

        if (!isReRender && buffer instanceof BufferBuilder builder && !builder.building)
            buffer = bufferSource.getBuffer(renderType);

        renderCubesOfBone(poseStack, bone, buffer, packedLight, packedOverlay, colour);

        if (!isReRender)
            applyRenderLayersForBone(
                    poseStack,
                    animatable,
                    bone,
                    renderType,
                    bufferSource,
                    buffer,
                    partialTick,
                    packedLight,
                    packedOverlay
            );

        renderChildBones(
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

        poseStack.popPose();
    }

    /**
     * Applies rotation transformations to the renderer prior to render time to account for various entity states, default scale of 1
     */
    protected void applyRotations(
            T animatable,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick
    ) {
        applyRotations(animatable, poseStack, ageInTicks, rotationYaw, partialTick, 1);
    }

    /**
     * Applies rotation transformations to the renderer prior to render time to account for various entity states, scalable
     */
    protected void applyRotations(T animatable, PoseStack poseStack, float ageInTicks, float rotationYaw,
                                  float partialTick, float nativeScale) {
        if (isShaking(animatable))
            rotationYaw += (float)(Math.cos(animatable.tickCount * 3.25d) * Math.PI * 0.4d);

        if (!animatable.hasPose(Pose.SLEEPING))
            poseStack.mulPose(Axis.YP.rotationDegrees(180f - rotationYaw));

        if (animatable instanceof LivingEntity livingEntity) {
            if (livingEntity.deathTime > 0) {
                float deathRotation = (livingEntity.deathTime + partialTick - 1f) / 20f * 1.6f;

                poseStack.mulPose(Axis.ZP.rotationDegrees(Math.min(Mth.sqrt(deathRotation), 1) * getDeathMaxRotation(animatable)));
            }
            else if (livingEntity.isAutoSpinAttack()) {
                poseStack.mulPose(Axis.XP.rotationDegrees(-90f - livingEntity.getXRot()));
                poseStack.mulPose(Axis.YP.rotationDegrees((livingEntity.tickCount + partialTick) * -75f));
            }
            else if (animatable.hasPose(Pose.SLEEPING)) {
                Direction bedOrientation = livingEntity.getBedOrientation();

                poseStack.mulPose(Axis.YP.rotationDegrees(bedOrientation != null ? RenderUtils.getDirectionAngle(bedOrientation) : rotationYaw));
                poseStack.mulPose(Axis.ZP.rotationDegrees(getDeathMaxRotation(animatable)));
                poseStack.mulPose(Axis.YP.rotationDegrees(270f));
            }
            else if (LivingEntityRenderer.isEntityUpsideDown(livingEntity)) {
                poseStack.translate(0, (animatable.getBbHeight() + 0.1f) / nativeScale, 0);
                poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
            }
        }
    }

    /**
     * Gets the max rotation value for dying entities.<br>
     * You might want to modify this for different aesthetics, such as a
     * {@link net.minecraft.world.entity.monster.Spider} flipping upside down on death.<br>
     * Functionally equivalent to {@link net.minecraft.client.renderer.entity.LivingEntityRenderer#getFlipDegrees}
     */
    protected float getDeathMaxRotation(T animatable) {
        return 90f;
    }

    /**
     * Whether the entity's nametag should be rendered or not.<br>
     * Pretty much exclusively used in {@link EntityRenderer#renderNameTag}
     */
    @Override
    public boolean shouldShowName(T animatable) {
        var nameRenderDistance = animatable.isDiscrete() ? 32d : 64d;

        if (!(animatable instanceof LivingEntity))
            return false;

        if (this.entityRenderDispatcher.distanceToSqr(animatable) >= nameRenderDistance * nameRenderDistance)
            return false;

        if (
                animatable instanceof Mob && (!animatable.shouldShowName() && (!animatable.hasCustomName()
                        || animatable != this.entityRenderDispatcher.crosshairPickEntity))
        )
            return false;

        final var minecraft = Minecraft.getInstance();
        var visibleToClient = !animatable.isInvisibleTo(minecraft.player);
        var entityTeam = animatable.getTeam();

        if (entityTeam == null)
            return Minecraft.renderNames() && animatable != minecraft.getCameraEntity() && visibleToClient
                    && !animatable.isVehicle();

        var playerTeam = minecraft.player.getTeam();

        return switch (entityTeam.getNameTagVisibility()) {
            case ALWAYS -> visibleToClient;
            case NEVER -> false;
            case HIDE_FOR_OTHER_TEAMS -> playerTeam == null
                    ? visibleToClient
                    : entityTeam.isAlliedTo(
                    playerTeam
            ) && (entityTeam.canSeeFriendlyInvisibles() || visibleToClient);
            case HIDE_FOR_OWN_TEAM ->
                    playerTeam == null ? visibleToClient : !entityTeam.isAlliedTo(playerTeam) && visibleToClient;
        };
    }

    /**
     * Gets a packed overlay coordinate pair for rendering.<br>
     * Mostly just used for the red tint when an entity is hurt, but can be used for other things like the
     * {@link net.minecraft.world.entity.monster.Creeper} white tint when exploding.
     */
    @Override
    public int getPackedOverlay(T animatable, float u, float partialTick) {
        if (!(animatable instanceof LivingEntity entity))
            return OverlayTexture.NO_OVERLAY;

        return OverlayTexture.pack(OverlayTexture.u(u), OverlayTexture.v(entity.hurtTime > 0 || entity.deathTime > 0));
    }

    /**
     * Static rendering code for rendering a leash segment.<br>
     * It's a like-for-like from {@link net.minecraft.client.renderer.entity.MobRenderer#renderLeash} that had to be
     * duplicated here for flexible usage
     */
    public <E extends Entity, M extends Mob> void renderLeash(
            M mob,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            E leashHolder
    ) {
        double lerpBodyAngle = (Mth.lerp(partialTick, mob.yBodyRotO, mob.yBodyRot) * Mth.DEG_TO_RAD) + Mth.HALF_PI;
        Vec3 leashOffset = mob.getLeashOffset();
        double xAngleOffset = Math.cos(lerpBodyAngle) * leashOffset.z + Math.sin(lerpBodyAngle) * leashOffset.x;
        double zAngleOffset = Math.sin(lerpBodyAngle) * leashOffset.z - Math.cos(lerpBodyAngle) * leashOffset.x;
        double lerpOriginX = Mth.lerp(partialTick, mob.xo, mob.getX()) + xAngleOffset;
        double lerpOriginY = Mth.lerp(partialTick, mob.yo, mob.getY()) + leashOffset.y;
        double lerpOriginZ = Mth.lerp(partialTick, mob.zo, mob.getZ()) + zAngleOffset;
        Vec3 ropeGripPosition = leashHolder.getRopeHoldPosition(partialTick);
        float xDif = (float) (ropeGripPosition.x - lerpOriginX);
        float yDif = (float) (ropeGripPosition.y - lerpOriginY);
        float zDif = (float) (ropeGripPosition.z - lerpOriginZ);
        float offsetMod = Mth.invSqrt(xDif * xDif + zDif * zDif) * 0.025f / 2f;
        float xOffset = zDif * offsetMod;
        float zOffset = xDif * offsetMod;
        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.leash());
        BlockPos entityEyePos = BlockPos.containing(mob.getEyePosition(partialTick));
        BlockPos holderEyePos = BlockPos.containing(leashHolder.getEyePosition(partialTick));
        int entityBlockLight = getBlockLightLevel((T) mob, entityEyePos);
        int holderBlockLight = leashHolder.isOnFire()
                ? 15
                : leashHolder.level()
                .getBrightness(
                        LightLayer.BLOCK,
                        holderEyePos
                );
        int entitySkyLight = mob.level().getBrightness(LightLayer.SKY, entityEyePos);
        int holderSkyLight = mob.level().getBrightness(LightLayer.SKY, holderEyePos);

        poseStack.pushPose();
        poseStack.translate(xAngleOffset, leashOffset.y, zAngleOffset);

        Matrix4f posMatrix = new Matrix4f(poseStack.last().pose());

        for (int segment = 0; segment <= 24; ++segment) {
            GeoEntityRenderer.renderLeashPiece(
                    vertexConsumer,
                    posMatrix,
                    xDif,
                    yDif,
                    zDif,
                    entityBlockLight,
                    holderBlockLight,
                    entitySkyLight,
                    holderSkyLight,
                    0.025f,
                    0.025f,
                    xOffset,
                    zOffset,
                    segment,
                    false
            );
        }

        for (int segment = 24; segment >= 0; --segment) {
            GeoEntityRenderer.renderLeashPiece(
                    vertexConsumer,
                    posMatrix,
                    xDif,
                    yDif,
                    zDif,
                    entityBlockLight,
                    holderBlockLight,
                    entitySkyLight,
                    holderSkyLight,
                    0.025f,
                    0.0f,
                    xOffset,
                    zOffset,
                    segment,
                    true
            );
        }

        poseStack.popPose();
    }

    public boolean isShaking(T entity) {
        return entity.isFullyFrozen();
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
                animatable.getId() + (int) animatable.getTick(animatable)
        );
    }

    /**
     * Create and fire the relevant {@code CompileLayers} event hook for this renderer
     */
    @Override
    public void fireCompileRenderLayersEvent() {
        Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireCompileEntityRenderLayers(this);
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
        return Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireEntityPreRender(this, poseStack, model, bufferSource, partialTick, packedLight);
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
        Services.GEO_RENDER_PHASE_EVENT_FACTORY.fireEntityPostRender(this, poseStack, model, bufferSource, partialTick, packedLight);
    }
}
