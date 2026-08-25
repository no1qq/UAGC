package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.check.event.AttackEvent;
import io.github.no1qq.uagc.engine.check.event.TargetSample;
import io.github.no1qq.uagc.engine.movement.MovementSnapshot;
import io.github.no1qq.uagc.engine.movement.Rotation;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.github.no1qq.uagc.engine.util.RingBuffer;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.BoundingBox;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public final class CombatListener implements Listener {

    private static final int MAX_TARGET_SAMPLES = 10;

    private final UagcRuntime runtime;

    public CombatListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onDamage(EntityDamageByEntityEvent event) {
        long tick = runtime.server().currentTick();

        if (event.getEntity() instanceof Player victim) {
            PlayerData victimData = runtime.players().get(victim.getUniqueId());
            if (victimData != null) {
                victimData.combat().recordDamageTaken(tick);
            }
        }
        if (!(event.getDamager() instanceof Player attacker)) {
            return;
        }
        PlayerData data = runtime.players().get(attacker.getUniqueId());
        if (data == null) {
            return;
        }

        Entity target = event.getEntity();
        long millis = System.currentTimeMillis();
        data.combat().recordAttack(target.getUniqueId(), tick, millis);

        Location eye = attacker.getEyeLocation();
        Rotation rotation = new Rotation(eye.getYaw(), eye.getPitch());
        MovementSnapshot last = data.movement().last();
        Rotation previousRotation = last == null ? rotation : last.fromRotation();

        AttackEvent attackEvent = new AttackEvent(
                tick,
                millis,
                PlayerSampler.toVec(eye),
                rotation,
                previousRotation,
                buildTarget(target, attacker.getPing()),
                PlayerSampler.sampleAttributes(attacker),
                Math.max(0, attacker.getPing()),
                attacker.isSprinting(),
                attacker.isRiptiding(),
                attacker.isInsideVehicle());

        if (runtime.engine().process(data, attackEvent)) {
            event.setCancelled(true);
        }
    }

    private TargetSample buildTarget(Entity target, int attackerPing) {
        BoundingBox box = target.getBoundingBox();
        double width = box.getWidthX();
        double height = box.getHeight();
        Location location = target.getLocation();
        Vec3 position = new Vec3(location.getX(), box.getMinY(), location.getZ());
        Vector velocity = target.getVelocity();

        List<Vec3> recent = new ArrayList<>();
        if (target instanceof Player targetPlayer) {
            PlayerData targetData = runtime.players().get(targetPlayer.getUniqueId());
            if (targetData != null) {
                int samples = Math.min(MAX_TARGET_SAMPLES, Math.max(2, attackerPing / 50 + 2));
                RingBuffer<MovementSnapshot> history = targetData.movement().history();
                for (int i = 0; i < samples && i < history.size(); i++) {
                    recent.add(history.fromEnd(i).to());
                }
            }
        }

        return new TargetSample(
                target.getUniqueId(),
                target.getType().name(),
                target instanceof Player,
                position,
                new Vec3(velocity.getX(), velocity.getY(), velocity.getZ()),
                width,
                height,
                List.copyOf(recent));
    }
}
