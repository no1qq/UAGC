package io.github.no1qq.uagc.bukkit.listener;

import io.github.no1qq.uagc.bukkit.UagcRuntime;
import io.github.no1qq.uagc.bukkit.sample.PlayerSampler;
import io.github.no1qq.uagc.engine.check.event.BlockBreakCheckEvent;
import io.github.no1qq.uagc.engine.check.event.BlockPlaceCheckEvent;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.player.PlayerData;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageAbortEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public final class InteractionListener implements Listener {

    private final UagcRuntime runtime;

    public InteractionListener(UagcRuntime runtime) {
        this.runtime = runtime;
    }

    private static long blockKey(Block block) {
        return ((long) block.getX() & 0x3FFFFFFL) << 38
                | ((long) block.getZ() & 0x3FFFFFFL) << 12
                | ((long) block.getY() & 0xFFFL);
    }

    private static Vec3 center(Block block) {
        return new Vec3(block.getX() + 0.5D, block.getY() + 0.5D, block.getZ() + 0.5D);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamage(BlockDamageEvent event) {
        PlayerData data = runtime.players().get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.interaction().beginBlockDamage(blockKey(event.getBlock()),
                    runtime.server().currentTick(), System.currentTimeMillis());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onDamageAbort(BlockDamageAbortEvent event) {
        PlayerData data = runtime.players().get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.interaction().abortBlockDamage();
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent event) {
        Player player = event.getPlayer();
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            return;
        }
        Block block = event.getBlock();
        long key = blockKey(block);
        long tick = runtime.server().currentTick();
        long start = data.interaction().damageStartTickFor(key);

        ItemStack tool = player.getInventory().getItemInMainHand();
        float speed;
        try {
            speed = block.getDestroySpeed(tool, true);
        } catch (RuntimeException exception) {
            speed = 0.0F;
        }

        BlockBreakCheckEvent checkEvent = new BlockBreakCheckEvent(
                tick,
                System.currentTimeMillis(),
                PlayerSampler.toVec(player.getEyeLocation()),
                center(block),
                block.getType().getKey().getKey(),
                speed,
                start,
                speed >= 1.0F,
                player.getGameMode() == GameMode.CREATIVE,
                PlayerSampler.sampleAttributes(player),
                Math.max(0, player.getPing()));

        runtime.engine().process(data, checkEvent);
        data.interaction().recordBreak(tick);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(org.bukkit.event.player.PlayerInteractEvent event) {
        PlayerData data = runtime.players().get(event.getPlayer().getUniqueId());
        if (data != null) {
            data.interaction().recordInteract(runtime.server().currentTick());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        PlayerData data = runtime.players().get(player.getUniqueId());
        if (data == null) {
            return;
        }
        Block placed = event.getBlockPlaced();
        Block against = event.getBlockAgainst();
        Location eye = player.getEyeLocation();
        long tick = runtime.server().currentTick();

        BlockPlaceCheckEvent checkEvent = new BlockPlaceCheckEvent(
                tick,
                System.currentTimeMillis(),
                PlayerSampler.toVec(eye),
                new io.github.no1qq.uagc.engine.movement.Rotation(eye.getYaw(), eye.getPitch()),
                center(placed),
                against == null ? null : center(against),
                placed.getType().getKey().getKey(),
                against == null ? "none" : placed.getFace(against) == null ? "unknown" : placed.getFace(against).name(),
                against != null && against.getType().isSolid(),
                PlayerSampler.sampleAttributes(player),
                Math.max(0, player.getPing()));

        runtime.engine().process(data, checkEvent);
        data.interaction().recordPlace(tick);
    }
}
