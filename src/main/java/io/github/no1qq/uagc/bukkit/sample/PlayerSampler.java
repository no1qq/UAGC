package io.github.no1qq.uagc.bukkit.sample;

import io.github.no1qq.uagc.engine.movement.ActivitySample;
import io.github.no1qq.uagc.engine.movement.AttributeSample;
import io.github.no1qq.uagc.engine.movement.EffectSample;
import io.github.no1qq.uagc.engine.movement.GameModeType;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import io.github.no1qq.uagc.engine.movement.Vec3;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public final class PlayerSampler {

    private PlayerSampler() {
    }

    @SuppressWarnings("deprecation")
    public static MovementSnapshot sample(Player player, Location from, Location to, long tick, long millis) {
        AttributeSample attributes = sampleAttributes(player);
        ActivitySample activity = sampleActivity(player);
        EffectSample effects = sampleEffects(player);
        SurfaceSample surface = BlockSampler.sample(player, to, attributes.scale());

        return new MovementSnapshot(
                tick,
                millis,
                toVec(from),
                toVec(to),
                new Rotation(from.getYaw(), from.getPitch()),
                new Rotation(to.getYaw(), to.getPitch()),
                player.isOnGround(),
                player.getFallDistance(),
                Math.max(0, player.getPing()),
                surface,
                activity,
                attributes,
                effects);
    }

    public static Vec3 toVec(Location location) {
        return new Vec3(location.getX(), location.getY(), location.getZ());
    }

    public static AttributeSample sampleAttributes(Player player) {
        return new AttributeSample(
                player.getWalkSpeed(),
                player.getFlySpeed(),
                attribute(player, Attribute.MOVEMENT_SPEED, AttributeSample.VANILLA_MOVEMENT_SPEED),
                attribute(player, Attribute.JUMP_STRENGTH, AttributeSample.VANILLA_JUMP_STRENGTH),
                attribute(player, Attribute.GRAVITY, AttributeSample.VANILLA_GRAVITY),
                attribute(player, Attribute.STEP_HEIGHT, AttributeSample.VANILLA_STEP_HEIGHT),
                attribute(player, Attribute.SCALE, 1.0D),
                attribute(player, Attribute.SAFE_FALL_DISTANCE, 3.0D),
                attribute(player, Attribute.FALL_DAMAGE_MULTIPLIER, 1.0D),
                attribute(player, Attribute.ENTITY_INTERACTION_RANGE, AttributeSample.VANILLA_ENTITY_INTERACTION_RANGE),
                attribute(player, Attribute.BLOCK_INTERACTION_RANGE, AttributeSample.VANILLA_BLOCK_INTERACTION_RANGE),
                attribute(player, Attribute.SNEAKING_SPEED, 0.3D),
                attribute(player, Attribute.MOVEMENT_EFFICIENCY, 0.0D),
                attribute(player, Attribute.WATER_MOVEMENT_EFFICIENCY, 0.0D));
    }

    public static ActivitySample sampleActivity(Player player) {
        return new ActivitySample(
                player.isSprinting(),
                player.isSneaking(),
                player.isSwimming(),
                player.isGliding(),
                player.isClimbing(),
                player.isRiptiding(),
                player.isFlying(),
                player.getAllowFlight(),
                player.isInsideVehicle(),
                player.isSleeping(),
                player.isDead(),
                gameMode(player),
                player.getVehicle() == null ? null : player.getVehicle().getType().name());
    }

    public static EffectSample sampleEffects(Player player) {
        return new EffectSample(
                amplifier(player, PotionEffectType.SPEED),
                amplifier(player, PotionEffectType.SLOWNESS),
                amplifier(player, PotionEffectType.JUMP_BOOST),
                amplifier(player, PotionEffectType.LEVITATION),
                player.hasPotionEffect(PotionEffectType.SLOW_FALLING),
                player.hasPotionEffect(PotionEffectType.DOLPHINS_GRACE),
                player.hasPotionEffect(PotionEffectType.BLINDNESS));
    }

    private static GameModeType gameMode(Player player) {
        return switch (player.getGameMode()) {
            case CREATIVE -> GameModeType.CREATIVE;
            case ADVENTURE -> GameModeType.ADVENTURE;
            case SPECTATOR -> GameModeType.SPECTATOR;
            default -> GameModeType.SURVIVAL;
        };
    }

    private static int amplifier(Player player, PotionEffectType type) {
        PotionEffect effect = player.getPotionEffect(type);
        return effect == null ? EffectSample.NONE : effect.getAmplifier();
    }

    private static double attribute(Player player, Attribute attribute, double fallback) {
        AttributeInstance instance = player.getAttribute(attribute);
        if (instance == null) {
            return fallback;
        }
        double value = instance.getValue();
        return Double.isFinite(value) ? value : fallback;
    }
}
