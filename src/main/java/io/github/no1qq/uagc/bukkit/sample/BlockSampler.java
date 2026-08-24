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
    private static final double PLAYER_HEIGHT = 1.8D;
    private static final int MAX_GROUND_SCAN = 6;

    private BlockSampler() {
    }

    private static final class BodyScan {
        boolean cobweb;
        boolean powderSnow;
        boolean berryBush;
        boolean honey;
        boolean bubbleColumn;
        boolean scaffolding;
        boolean climbable;
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
        double height = PLAYER_HEIGHT * Math.max(0.1D, scale);
        double x = to.getX();
        double y = to.getY();
        double z = to.getZ();

        BoundingBox feet = new BoundingBox(x - half, y - GROUND_EPSILON, z - half, x + half, y + 0.001D, z + half);
        boolean solidBelow = intersectsSolid(world, feet);

        BoundingBox nearFeet = new BoundingBox(x - half, y - 0.5D, z - half, x + half, y + 0.001D, z + half);
        boolean nearGround = solidBelow || intersectsSolid(world, nearFeet);

        double distanceToGround = solidBelow ? 0.0D : distanceToGround(world, x, y, z, half);

        BoundingBox body = new BoundingBox(x - half, y + 0.1D, z - half, x + half, y + height * 0.95D, z + half);
        boolean insideSolid = intersectsSolid(world, body);

        BoundingBox sides = new BoundingBox(x - half - 0.05D, y + 0.1D, z - half - 0.05D,
                x + half + 0.05D, y + height * 0.85D, z + half + 0.05D);
        boolean collidingHorizontally = !insideSolid && intersectsSolid(world, sides);

        BodyScan scan = scanBody(world, new BoundingBox(x - half, y, z - half, x + half, y + height, z + half));

        Block below = world.getBlockAt(blockX, (int) Math.floor(y - 0.1D), blockZ);
        Material belowType = below.getType();
        double friction = frictionOf(belowType);

        return new SurfaceSample(
                solidBelow,
                nearGround,
                distanceToGround,
                collidingHorizontally,
                insideSolid,
                player.isInWater(),
                player.isInLava(),
                world.getBlockAt(blockX, (int) Math.floor(y + height * 0.85D), blockZ).getType() == Material.WATER,
                scan.bubbleColumn || belowType == Material.BUBBLE_COLUMN,
                player.isClimbing() || scan.climbable,
                belowType == Material.SLIME_BLOCK,
                scan.honey || belowType == Material.HONEY_BLOCK,
                Tag.BEDS.isTagged(belowType),
                scan.scaffolding || belowType == Material.SCAFFOLDING,
                scan.powderSnow || belowType == Material.POWDER_SNOW,
                scan.cobweb,
                scan.berryBush,
                Tag.SLABS.isTagged(belowType) || Tag.STAIRS.isTagged(belowType),
                friction,
                true,
                belowType.getKey().getKey());
    }

    private static BodyScan scanBody(World world, BoundingBox box) {
        BodyScan scan = new BodyScan();
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
                    Material type = world.getBlockAt(bx, by, bz).getType();
                    switch (type) {
                        case COBWEB -> scan.cobweb = true;
                        case POWDER_SNOW -> scan.powderSnow = true;
                        case SWEET_BERRY_BUSH -> scan.berryBush = true;
                        case HONEY_BLOCK -> scan.honey = true;
                        case BUBBLE_COLUMN -> scan.bubbleColumn = true;
                        case SCAFFOLDING -> scan.scaffolding = true;
                        default -> {
                            if (Tag.CLIMBABLE.isTagged(type)) {
                                scan.climbable = true;
                            }
                        }
                    }
                }
            }
        }
        return scan;
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
