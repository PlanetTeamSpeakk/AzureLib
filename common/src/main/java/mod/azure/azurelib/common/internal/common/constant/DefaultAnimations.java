/**
 * This class is a fork of the matching class found in the Geckolib repository.
 * Original source: https://github.com/bernie-g/geckolib
 * Copyright © 2024 Bernie-G.
 * Licensed under the MIT License.
 * https://github.com/bernie-g/geckolib/blob/main/LICENSE
 */
package mod.azure.azurelib.common.internal.common.constant;

import mod.azure.azurelib.common.api.common.animatable.GeoBlockEntity;
import mod.azure.azurelib.common.api.common.animatable.GeoEntity;
import mod.azure.azurelib.common.api.common.animatable.GeoItem;
import mod.azure.azurelib.common.api.common.animatable.GeoReplacedEntity;
import mod.azure.azurelib.core.animatable.GeoAnimatable;
import mod.azure.azurelib.core.animation.AnimationController;
import mod.azure.azurelib.core.animation.AnimationState;
import mod.azure.azurelib.core.animation.RawAnimation;
import mod.azure.azurelib.core.object.PlayState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;

/**
 * Optionally usable class that holds constants for recommended animation paths.<br>
 * Using these won't affect much, but it may help keep some consistency in animation namings.<br>
 * Additionally, it encourages use of cached {@link RawAnimation RawAnimations}, to reduce overheads.
 */
public record DefaultAnimations() {

    public static final RawAnimation ITEM_ON_USE = RawAnimation.begin().thenPlay("item.use");

    public static final RawAnimation IDLE = RawAnimation.begin().thenLoop("misc.idle");

    public static final RawAnimation LIVING = RawAnimation.begin().thenLoop("misc.living");

    public static final RawAnimation SPAWN = RawAnimation.begin().thenPlay("misc.spawn");

    public static final RawAnimation INTERACT = RawAnimation.begin().thenPlay("misc.interact");

    public static final RawAnimation DEPLOY = RawAnimation.begin().thenPlay("misc.deploy");

    public static final RawAnimation REST = RawAnimation.begin().thenPlay("misc.rest");

    public static final RawAnimation SIT = RawAnimation.begin().thenPlayAndHold("misc.sit");

    public static final RawAnimation WALK = RawAnimation.begin().thenLoop("move.walk");

    public static final RawAnimation SWIM = RawAnimation.begin().thenLoop("move.swim");

    public static final RawAnimation RUN = RawAnimation.begin().thenLoop("move.run");

    public static final RawAnimation DRIVE = RawAnimation.begin().thenLoop("move.drive");

    public static final RawAnimation FLY = RawAnimation.begin().thenLoop("move.fly");

    public static final RawAnimation CRAWL = RawAnimation.begin().thenLoop("move.crawl");

    public static final RawAnimation JUMP = RawAnimation.begin().thenPlay("move.jump");

    public static final RawAnimation SNEAK = RawAnimation.begin().thenLoop("move.sneak");

    public static final RawAnimation ATTACK_CAST = RawAnimation.begin().thenPlay("attack.cast");

    public static final RawAnimation ATTACK_SWING = RawAnimation.begin().thenPlay("attack.swing");

    public static final RawAnimation ATTACK_THROW = RawAnimation.begin().thenPlay("attack.throw");

    public static final RawAnimation ATTACK_BITE = RawAnimation.begin().thenPlay("attack.bite");

    public static final RawAnimation ATTACK_SLAM = RawAnimation.begin().thenPlay("attack.slam");

    public static final RawAnimation ATTACK_STOMP = RawAnimation.begin().thenPlay("attack.stomp");

    public static final RawAnimation ATTACK_STRIKE = RawAnimation.begin().thenPlay("attack.strike");

    public static final RawAnimation ATTACK_FLYING_ATTACK = RawAnimation.begin().thenPlay("attack.flying_attack");

    public static final RawAnimation ATTACK_SHOOT = RawAnimation.begin().thenPlay("attack.shoot");

    public static final RawAnimation ATTACK_BLOCK = RawAnimation.begin().thenPlay("attack.block");

    public static final RawAnimation ATTACK_CHARGE = RawAnimation.begin().thenPlay("attack.charge");

    public static final RawAnimation ATTACK_CHARGE_END = RawAnimation.begin().thenPlay("attack.charge_end");

    public static final RawAnimation ATTACK_POWERUP = RawAnimation.begin().thenPlay("attack.powerup");

    /**
     * A basic predicate-based {@link AnimationController} implementation.<br>
     * Provide an {@code option A} {@link RawAnimation animation} and an {@code option B} animation, and use the
     * predicate to determine which to play.<br>
     * Outcome table:
     *
     * <pre>
     *   true  -> Animation Option A
     * false -> Animation Option B
     * null  -> Stop Controller
     * </pre>
     */
    public static <T extends GeoAnimatable> AnimationController<T> basicPredicateController(
            T animatable,
            RawAnimation optionA,
            RawAnimation optionB,
            BiPredicate<T, AnimationState<?>> predicate
    ) {
        return new AnimationController<>(animatable, "Generic", 10, state -> {
            var result = predicate.test(animatable, state);

            return state.setAndContinue(result ? optionA : optionB);
        });
    }

    /**
     * Generic {@link DefaultAnimations#LIVING living} controller.<br>
     * Continuously plays the living animation
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericLivingController(T animatable) {
        return new AnimationController<>(animatable, "Living", 10, state -> state.setAndContinue(LIVING));
    }

    /**
     * Generic {@link DefaultAnimations#IDLE idle} controller.<br>
     * Continuously plays the idle animation
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericIdleController(T animatable) {
        return new AnimationController<T>(animatable, "Idle", 10, state -> state.setAndContinue(IDLE));
    }

    /**
     * Generic {@link DefaultAnimations#SPAWN spawn} controller.<br>
     * Plays the spawn animation as long as the current {@link GeoAnimatable#getTick tick} of the animatable is less
     * than or equal to the value provided in {@code ticks}.<br>
     * For the {@code objectSupplier}, provide the relevant object for the animatable being animated. Recommended:
     * <ul>
     * <li>{@link GeoEntity GeoEntity}: state -> animatable</li>
     * <li>{@link GeoBlockEntity GeoBlockEntity}: state -> animatable</li>
     * <li>{@link GeoReplacedEntity GeoReplacedEntity}: state -> state.getData(DataTickets.ENTITY)</li>
     * <li>{@link GeoItem GeoItem}: state -> state.getData(DataTickets.ITEMSTACK)</li>
     * <li>{@code GeoArmor}: state -> state.getData(DataTickets.ENTITY)</li>
     * </ul>
     *
     * @param animatable     The animatable the animation is for
     * @param objectSupplier The supplier of the associated object for the {@link GeoAnimatable#getTick} call
     * @param ticks          The number of ticks the animation should run for. After this value is surpassed, the
     *                       animation will no longer play
     */
    public static <T extends GeoAnimatable> AnimationController<T> getSpawnController(
            T animatable,
            Function<AnimationState<T>, Object> objectSupplier,
            int ticks
    ) {
        return new AnimationController<>(animatable, "Spawn", 0, state -> {
            if (animatable.getTick(objectSupplier.apply(state)) <= ticks)
                return state.setAndContinue(DefaultAnimations.SPAWN);

            return PlayState.STOP;
        });
    }

    /**
     * Generic {@link DefaultAnimations#WALK walk} controller.<br>
     * Will play the walk animation if the animatable is considered moving, or stop if not.
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericWalkController(T animatable) {
        return new AnimationController<>(animatable, "Walk", 0, state -> {
            if (state.isMoving())
                return state.setAndContinue(WALK);

            return PlayState.STOP;
        });
    }

    /**
     * Generic attack controller.<br>
     * Plays an attack animation if the animatable is {@link net.minecraft.world.entity.LivingEntity#swinging}.<br>
     * Resets the animation each time it stops, ready for the next swing
     *
     * @param animatable      The entity that should swing
     * @param attackAnimation The attack animation to play (E.G. swipe, strike, stomp, swing, etc)
     * @return A new {@link AnimationController} instance to use
     */
    public static <T extends LivingEntity & GeoAnimatable> AnimationController<T> genericAttackAnimation(
            T animatable,
            RawAnimation attackAnimation
    ) {
        return new AnimationController<>(animatable, "Attack", 5, state -> {
            if (animatable.swinging)
                return state.setAndContinue(attackAnimation);

            state.getController().forceAnimationReset();

            return PlayState.STOP;
        });
    }

    /**
     * Generic {@link DefaultAnimations#WALK walk} + {@link DefaultAnimations#IDLE idle} controller.<br>
     * Will play the walk animation if the animatable is considered moving, or idle if not
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericWalkIdleController(T animatable) {
        return new AnimationController<>(
                animatable,
                "Walk/Idle",
                0,
                state -> state.setAndContinue(state.isMoving() ? WALK : IDLE)
        );
    }

    /**
     * Generic {@link DefaultAnimations#SWIM swim} controller.<br>
     * Will play the swim animation if the animatable is considered moving, or stop if not.
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericSwimController(T entity) {
        return new AnimationController<>(entity, "Swim", 0, state -> {
            if (state.isMoving())
                return state.setAndContinue(SWIM);

            return PlayState.STOP;
        });
    }

    /**
     * Generic {@link DefaultAnimations#SWIM swim} + {@link DefaultAnimations#IDLE idle} controller.<br>
     * Will play the swim animation if the animatable is considered moving, or idle if not
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericSwimIdleController(T animatable) {
        return new AnimationController<>(
                animatable,
                "Swim/Idle",
                0,
                state -> state.setAndContinue(state.isMoving() ? SWIM : IDLE)
        );
    }

    /**
     * Generic {@link DefaultAnimations#FLY walk} controller.<br>
     * Will play the fly animation if the animatable is considered moving, or stop if not.
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericFlyController(T animatable) {
        return new AnimationController<>(animatable, "Fly", 0, state -> state.setAndContinue(FLY));
    }

    /**
     * Generic {@link DefaultAnimations#FLY walk} + {@link DefaultAnimations#IDLE idle} controller.<br>
     * Will play the walk animation if the animatable is considered moving, or idle if not
     */
    public static <T extends GeoAnimatable> AnimationController<T> genericFlyIdleController(T animatable) {
        return new AnimationController<>(
                animatable,
                "Fly/Idle",
                0,
                state -> state.setAndContinue(state.isMoving() ? FLY : IDLE)
        );
    }

    /**
     * Generic {@link DefaultAnimations#WALK walk} + {@link DefaultAnimations#RUN run} + {@link DefaultAnimations#IDLE
     * idle} controller.<br>
     * If the entity is considered moving, will either walk or run depending on the {@link Entity#isSprinting()} method,
     * otherwise it will idle
     */
    public static <T extends Entity & GeoAnimatable> AnimationController<T> genericWalkRunIdleController(T entity) {
        return new AnimationController<>(entity, "Walk/Run/Idle", 0, state -> {
            if (state.isMoving()) {
                return state.setAndContinue(entity.isSprinting() ? RUN : WALK);
            } else {
                return state.setAndContinue(IDLE);
            }
        });
    }
}
