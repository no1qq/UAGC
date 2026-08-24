package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceType;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;
import io.papermc.paper.event.entity.EntityKnockbackEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.event.entity.EntityToggleGlideEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.event.player.PlayerVelocityEvent;
import org.bukkit.util.Vector;

public final class StateListener implements Listener {

    private final UagcRuntime runtime;

    public StateListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    private PlayerData data(Player player) {
        return runtime.players().get(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onTeleport(PlayerTeleportEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        ExemptionType type = switch (event.getCause()) {
            case NETHER_PORTAL, END_PORTAL, END_GATEWAY -> ExemptionType.PORTAL;
            case PLUGIN, COMMAND, SPECTATE -> ExemptionType.PLUGIN_TELEPORT;
            case UNKNOWN -> ExemptionType.SERVER_CORRECTION;
            default -> ExemptionType.TELEPORT;
        };
        data.exemptions().grant(type, 0, "server", event.getCause().name());
        if (type != ExemptionType.SERVER_CORRECTION) {
            data.exemptions().grant(ExemptionType.TELEPORT);
        }
        data.movement().breakContinuity();
        if (event.getTo() != null) {
            data.setLastSafePosition(PlayerSampler.toVec(event.getTo()));
        }
        data.recordEvidence(EvidenceEntry.of(EvidenceType.TELEPORT, "teleported by " + event.getCause().name())
                .with("cause", event.getCause().name())
                .with("to", event.getTo() == null ? "unknown" : PlayerSampler.toVec(event.getTo()).toString()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVelocity(PlayerVelocityEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        Vector velocity = event.getVelocity();
        Vec3 applied = new Vec3(velocity.getX(), velocity.getY(), velocity.getZ());
        data.velocity().record(applied, runtime.server().currentTick(), "velocity_event");
        data.exemptions().grant(ExemptionType.VELOCITY);
        data.recordEvidence(EvidenceEntry.of(EvidenceType.VELOCITY, "server applied velocity")
                .with("vector", applied.toString())
                .with("horizontal", applied.horizontalLength()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onKnockback(EntityKnockbackEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PlayerData data = data(player);
        if (data == null) {
            return;
        }
        Vector knockback = event.getKnockback();
        Vec3 applied = new Vec3(knockback.getX(), knockback.getY(), knockback.getZ());
        data.velocity().record(applied, runtime.server().currentTick(), event.getCause().name());
        data.exemptions().grant(ExemptionType.KNOCKBACK);
        if (event.getCause() == EntityKnockbackEvent.Cause.EXPLOSION) {
            data.exemptions().grant(ExemptionType.EXPLOSION);
        }
        data.recordEvidence(EvidenceEntry.of(EvidenceType.KNOCKBACK, "knockback " + event.getCause().name())
                .with("vector", applied.toString())
                .with("horizontal", applied.horizontalLength()));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onRespawn(PlayerRespawnEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.RESPAWN);
        data.movement().reset();
        data.setLastSafePosition(PlayerSampler.toVec(event.getRespawnLocation()));
        data.recordEvidence(EvidenceEntry.of(EvidenceType.LIFECYCLE, "respawned"));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onWorldChange(PlayerChangedWorldEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.WORLD_CHANGE);
        data.movement().reset();
        data.setLastSafePosition(PlayerSampler.toVec(event.getPlayer().getLocation()));
        data.recordEvidence(EvidenceEntry.of(EvidenceType.LIFECYCLE, "changed world")
                .with("world", event.getPlayer().getWorld().getName()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onGameModeChange(PlayerGameModeChangeEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.GAMEMODE_CHANGE);
        data.recordEvidence(EvidenceEntry.of(EvidenceType.STATE_CHANGE, "game mode changed")
                .with("mode", event.getNewGameMode().name()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleFlight(PlayerToggleFlightEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.FLIGHT_TOGGLE);
        data.movement().breakContinuity();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onToggleGlide(EntityToggleGlideEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PlayerData data = data(player);
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.ELYTRA);
        data.movement().breakContinuity();
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPotionEffect(EntityPotionEffectEvent event) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PlayerData data = data(player);
        if (data == null) {
            return;
        }
        data.exemptions().grant(ExemptionType.EFFECT_CHANGE);
        data.recordEvidence(EvidenceEntry.of(EvidenceType.STATE_CHANGE, "potion effect changed")
                .with("effect", event.getModifiedType() == null ? "unknown" : event.getModifiedType().getKey().getKey())
                .with("action", event.getAction().name())
                .with("cause", event.getCause().name()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJump(com.destroystokyo.paper.event.player.PlayerJumpEvent event) {
        PlayerData data = data(event.getPlayer());
        if (data != null) {
            data.velocity().recordJump(runtime.server().currentTick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleEnter(org.bukkit.event.vehicle.VehicleEnterEvent event) {
        if (event.getEntered() instanceof Player player) {
            PlayerData data = data(player);
            if (data != null) {
                data.exemptions().grant(ExemptionType.VEHICLE);
                data.movement().breakContinuity();
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onVehicleExit(org.bukkit.event.vehicle.VehicleExitEvent event) {
        if (event.getExited() instanceof Player player) {
            PlayerData data = data(player);
            if (data != null) {
                data.exemptions().grant(ExemptionType.VEHICLE_EXIT);
                data.movement().breakContinuity();
            }
        }
    }
}
