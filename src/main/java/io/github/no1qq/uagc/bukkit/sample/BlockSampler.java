package io.github.no1qq.uagc.bukkit.sample;

import io.github.no1qq.uagc.engine.movement.SurfaceSample;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.BoundingBox;

public final class BlockSampler {

    private static final double GROUND_EPSILON = 0.03D;
    private static final double PLAYER_HALF_WIDTH = 0.3D;
    private static final int MAX_GROUND_SCAN = 6;

    private BlockSampler() {
    }

    public static SurfaceSample sample(Player player, Location to, double scale) {
        World world = to.getWorld();
        if (world == null) {
            return SurfaceSample.unknown();
        }
        int blockX = to.getBlockX();
        int blockZ = to.getBlockZ();
        if (!world.isChunkLoaded(blockX >> 4, blockZ >> 4)) {
            return SurfaceSample.unknown();
        }

        double half = PLAYER_HALF_WIDTH * Math.max(0.1D, scale);
        double x = to.getX();
        double y = to.getY();
        double z = to.getZ();

        BoundingBox feet = new BoundingBox(x - half, y - GROUND_EPSILON, z - half, x + half, y + 0.001D, z + half);
        boolean solidBelow = intersectsSolid(world, feet);

        BoundingBox nearFeet = new BoundingBox(x - half, y - 0.5D, z - half, x + half, y + 0.001D, z + half);
        boolean nearGround = solidBelow || intersectsSolid(world, nearFeet);

        double distanceToGround = solidBelow ? 0.0D : distanceToGround(world, x, y, z, half);

        BoundingBox body = new BoundingBox(x - half, y + 0.1D, z - half, x + half, y + 1.7D * scale, z + half);
        boolean insideSolid = intersectsSolid(world, body);

        BoundingBox sides = new BoundingBox(x - half - 0.05D, y + 0.1D, z - half - 0.05D,
                x + half + 0.05D, y + 1.5D * scale, z + half + 0.05D);
        boolean collidingHorizontally = !insideSolid && intersectsSolid(world, sides);

        Block below = world.getBlockAt(blockX, (int) Math.floor(y - 0.1D), blockZ);
        Block at = world.getBlockAt(blockX, (int) Math.floor(y + 0.1D), blockZ);
        Block head = world.getBlockAt(blockX, (int) Math.floor(y + 1.5D * scale), blockZ);

        Material belowType = below.getType();
        Material atType = at.getType();

        double friction = frictionOf(belowType);

        return new SurfaceSample(
                solidBelow,
                nearGround,
                distanceToGround,
                collidingHorizontally,
                insideSolid,
                player.isInWater(),
                player.isInLava(),
                head.getType() == Material.WATER,
                atType == Material.BUBBLE_COLUMN || belowType == Material.BUBBLE_COLUMN,
                player.isClimbing() || Tag.CLIMBABLE.isTagged(atType),
                belowType == Material.SLIME_BLOCK,
                belowType == Material.HONEY_BLOCK || atType == Material.HONEY_BLOCK,
                Tag.BEDS.isTagged(belowType),
                atType == Material.SCAFFOLDING || belowType == Material.SCAFFOLDING,
                atType == Material.POWDER_SNOW || belowType == Material.POWDER_SNOW,
                atType == Material.COBWEB,
                atType == Material.SWEET_BERRY_BUSH,
                Tag.SLABS.isTagged(belowType) || Tag.STAIRS.isTagged(belowType),
                friction,
                true,
                belowType.getKey().getKey());
    }

    public static double frictionOf(Material material) {
        return switch (material) {
            case ICE, PACKED_ICE, FROSTED_ICE -> 0.98D;
            case BLUE_ICE -> 0.989D;
            case SLIME_BLOCK -> 0.8D;
            default -> SurfaceSample.DEFAULT_FRICTION;
        };
    }

    private static boolean intersectsSolid(World world, BoundingBox box) {
        int minX = (int) Math.floor(box.getMinX());
        int maxX = (int) Math.floor(box.getMaxX());
        int minY = (int) Math.floor(box.getMinY());
        int maxY = (int) Math.floor(box.getMaxY());
        int minZ = (int) Math.floor(box.getMinZ());
        int maxZ = (int) Math.floor(box.getMaxZ());

        for (int bx = minX; bx <= maxX; bx++) {
            for (int bz = minZ; bz <= maxZ; bz++) {
                if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
                    continue;
                }
                for (int by = minY; by <= maxY; by++) {
                    Block block = world.getBlockAt(bx, by, bz);
                    if (block.isPassable() || block.isLiquid()) {
                        continue;
                    }
                    if (block.getBoundingBox().overlaps(box)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static double distanceToGround(World world, double x, double y, double z, double half) {
        for (int offset = 0; offset < MAX_GROUND_SCAN; offset++) {
            double top = y - offset;
            BoundingBox slab = new BoundingBox(x - half, top - 1.0D, z - half, x + half, top, z + half);
            if (!intersectsSolid(world, slab)) {
                continue;
            }
            double best = Double.MAX_VALUE;
            int minX = (int) Math.floor(slab.getMinX());
            int maxX = (int) Math.floor(slab.getMaxX());
            int minZ = (int) Math.floor(slab.getMinZ());
            int maxZ = (int) Math.floor(slab.getMaxZ());
            int minY = (int) Math.floor(slab.getMinY());
            int maxY = (int) Math.floor(slab.getMaxY());
            for (int bx = minX; bx <= maxX; bx++) {
                for (int bz = minZ; bz <= maxZ; bz++) {
                    if (!world.isChunkLoaded(bx >> 4, bz >> 4)) {
                        continue;
                    }
                    for (int by = maxY; by >= minY; by--) {
                        Block block = world.getBlockAt(bx, by, bz);
                        if (block.isPassable() || block.isLiquid()) {
                            continue;
                        }
                        BoundingBox blockBox = block.getBoundingBox();
                        if (blockBox.getMaxY() <= y) {
                            best = Math.min(best, y - blockBox.getMaxY());
                        }
                    }
                }
            }
            if (best != Double.MAX_VALUE) {
                return best;
            }
        }
        return MAX_GROUND_SCAN;
    }
}
