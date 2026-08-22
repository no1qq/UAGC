package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.platform.EnforcementGateway;
import io.github.no1qq.uagc.engine.punishment.PunishmentRecord;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class RecordingEnforcementGateway implements EnforcementGateway {

    public record BanCall(UUID playerId, PunishmentRecord record, Duration duration) {
    }

    private final List<PunishmentRecord> kicks = new ArrayList<>();
    private final List<BanCall> bans = new ArrayList<>();
    private final List<String> commands = new ArrayList<>();
    private final List<Vec3> setbacks = new ArrayList<>();
    private final List<UUID> freezeCalls = new ArrayList<>();

    @Override
    public void kick(UUID playerId, PunishmentRecord record) {
        kicks.add(record);
    }

    @Override
    public void ban(UUID playerId, PunishmentRecord record, Duration duration) {
        bans.add(new BanCall(playerId, record, duration));
    }

    @Override
    public boolean unban(String playerName) {
        commands.add("unban:" + playerName);
        return true;
    }

    @Override
    public void runConsoleCommand(String command) {
        commands.add(command);
    }

    @Override
    public void setback(UUID playerId, Vec3 position) {
        setbacks.add(position);
    }

    @Override
    public void applyFreeze(UUID playerId, boolean frozen) {
        freezeCalls.add(playerId);
    }

    public List<PunishmentRecord> kicks() {
        return kicks;
    }

    public List<BanCall> bans() {
        return bans;
    }

    public List<String> commands() {
        return commands;
    }

    public List<Vec3> setbacks() {
        return setbacks;
    }

    public List<UUID> freezeCalls() {
        return freezeCalls;
    }
}
