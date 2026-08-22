package io.github.no1qq.uagc.engine.movement;

public record ActivitySample(
        boolean sprinting,
        boolean sneaking,
        boolean swimming,
        boolean gliding,
        boolean climbing,
        boolean riptiding,
        boolean flying,
        boolean allowFlight,
        boolean insideVehicle,
        boolean sleeping,
        boolean dead,
        GameModeType gameMode,
        String vehicleType) {

    public static ActivitySample idle() {
        return new ActivitySample(false, false, false, false, false, false, false, false, false, false, false,
                GameModeType.SURVIVAL, null);
    }

    public boolean hasAlternateMovement() {
        return swimming || gliding || climbing || riptiding || flying || insideVehicle || sleeping || dead;
    }
}
