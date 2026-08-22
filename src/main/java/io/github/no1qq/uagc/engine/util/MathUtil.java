package io.github.no1qq.uagc.engine.util;

public final class MathUtil {

    private MathUtil() {
    }

    public static double clamp(double value, double min, double max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static double clamp01(double value) {
        return clamp(value, 0.0D, 1.0D);
    }

    public static int clamp(int value, int min, int max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static long clamp(long value, long min, long max) {
        if (value < min) {
            return min;
        }
        return Math.min(value, max);
    }

    public static boolean isFinite(double value) {
        return !Double.isNaN(value) && !Double.isInfinite(value);
    }

    public static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    public static double sanitize(double value, double fallback) {
        return isFinite(value) ? value : fallback;
    }

    public static double lerp(double from, double to, double progress) {
        return from + (to - from) * clamp01(progress);
    }

    public static double ratio(double value, double reference) {
        if (reference <= 0.0D || !isFinite(reference) || !isFinite(value)) {
            return 0.0D;
        }
        return value / reference;
    }

    public static double excessRatio(double observed, double allowed) {
        if (allowed <= 0.0D) {
            return observed > 0.0D ? 1.0D : 0.0D;
        }
        return (observed - allowed) / allowed;
    }

    public static float wrapDegrees(float degrees) {
        float wrapped = degrees % 360.0F;
        if (wrapped >= 180.0F) {
            wrapped -= 360.0F;
        }
        if (wrapped < -180.0F) {
            wrapped += 360.0F;
        }
        return wrapped;
    }

    public static float angleDifference(float from, float to) {
        return Math.abs(wrapDegrees(to - from));
    }

    public static double mean(double[] values, int count) {
        if (count <= 0) {
            return 0.0D;
        }
        double sum = 0.0D;
        for (int i = 0; i < count; i++) {
            sum += values[i];
        }
        return sum / count;
    }

    public static double standardDeviation(double[] values, int count) {
        if (count <= 1) {
            return 0.0D;
        }
        double mean = mean(values, count);
        double sum = 0.0D;
        for (int i = 0; i < count; i++) {
            double diff = values[i] - mean;
            sum += diff * diff;
        }
        return Math.sqrt(sum / (count - 1));
    }
}
