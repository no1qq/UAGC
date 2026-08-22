package io.github.no1qq.uagc.engine.platform;

import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.punishment.PunishmentRecord;

import java.time.Duration;
import java.util.UUID;

public interface EnforcementGateway {

    void kick(UUID playerId, PunishmentRecord record);

    void ban(UUID playerId, PunishmentRecord record, Duration duration);

    boolean unban(String playerName);

    void runConsoleCommand(String command);

    void setback(UUID playerId, Vec3 position);

    void applyFreeze(UUID playerId, boolean frozen);
}
