package io.github.no1qq.uagc.engine.player;

import io.github.no1qq.uagc.engine.alert.AlertSettings;
import io.github.no1qq.uagc.engine.bypass.BypassState;
import io.github.no1qq.uagc.engine.evidence.EvidenceEntry;
import io.github.no1qq.uagc.engine.evidence.EvidenceLog;
import io.github.no1qq.uagc.engine.exemption.ExemptionState;
import io.github.no1qq.uagc.engine.exemption.ExemptionType;
import io.github.no1qq.uagc.engine.movement.Vec3;
import io.github.no1qq.uagc.engine.platform.UagcClock;
import io.github.no1qq.uagc.engine.violation.ViolationTracker;

import java.util.UUID;

public final class PlayerData {

    private final UUID uuid;
    private final UagcClock clock;
    private final ExemptionState exemptions;
    private final BypassState bypass = new BypassState();
    private final MovementTracker movement;
    private final LatencyTracker latency;
    private final CombatState combat;
    private final InteractionState interaction = new InteractionState();
    private final VelocityState velocity = new VelocityState();
    private final EvidenceLog evidence;
    private final ViolationTracker[] violationTrackers;
    private final Object[] checkStates;
    private final AlertSettings alertSettings;
    private final long joinTick;
    private final long joinMillis;

    private volatile String name;
    private volatile Vec3 lastSafePosition;
    private volatile long lastSetbackTick = Long.MIN_VALUE;
    private volatile int setbackCount;
    private volatile boolean debugTarget;

    public PlayerData(UUID uuid,
                      String name,
                      UagcClock clock,
                      int checkCount,
                      PlayerDataSettings settings) {
        this.uuid = uuid;
        this.name = name;
        this.clock = clock;
        this.exemptions = new ExemptionState(clock);
        this.movement = new MovementTracker(settings.movementHistorySize());
        this.latency = new LatencyTracker(settings.latencySampleSize());
        this.combat = new CombatState(settings.clickSampleSize());
        this.evidence = new EvidenceLog(settings.evidenceEntryCapacity(), settings.evidenceViolationCapacity());
        this.violationTrackers = new ViolationTracker[checkCount];
        this.checkStates = new Object[checkCount];
        this.alertSettings = new AlertSettings(settings.alertsEnabledByDefault(),
                settings.defaultAlertConfidence(), settings.defaultAlertViolationLevel());
        this.joinTick = clock.currentTick();
        this.joinMillis = clock.currentTimeMillis();
    }

    public UUID uuid() {
        return uuid;
    }

    public String name() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public UagcClock clock() {
        return clock;
    }

    public ExemptionState exemptions() {
        return exemptions;
    }

    public BypassState bypass() {
        return bypass;
    }

    public MovementTracker movement() {
        return movement;
    }

    public LatencyTracker latency() {
        return latency;
    }

    public CombatState combat() {
        return combat;
    }

    public InteractionState interaction() {
        return interaction;
    }

    public VelocityState velocity() {
        return velocity;
    }

    public EvidenceLog evidence() {
        return evidence;
    }

    public AlertSettings alertSettings() {
        return alertSettings;
    }

    public long joinTick() {
        return joinTick;
    }

    public long joinMillis() {
        return joinMillis;
    }

    public long ticksOnline() {
        return clock.currentTick() - joinTick;
    }

    public ViolationTracker violations(int checkIndex, String checkId) {
        ViolationTracker tracker = violationTrackers[checkIndex];
        if (tracker == null) {
            tracker = new ViolationTracker(checkId);
            violationTrackers[checkIndex] = tracker;
        }
        return tracker;
    }

    public ViolationTracker violationsIfPresent(int checkIndex) {
        return checkIndex >= 0 && checkIndex < violationTrackers.length ? violationTrackers[checkIndex] : null;
    }

    public int violationTrackerCount() {
        return violationTrackers.length;
    }

    @SuppressWarnings("unchecked")
    public <S> S checkState(int checkIndex) {
        return (S) checkStates[checkIndex];
    }

    public void setCheckState(int checkIndex, Object state) {
        checkStates[checkIndex] = state;
    }

    public Vec3 lastSafePosition() {
        return lastSafePosition;
    }

    public void setLastSafePosition(Vec3 position) {
        this.lastSafePosition = position;
    }

    public long lastSetbackTick() {
        return lastSetbackTick;
    }

    public int setbackCount() {
        return setbackCount;
    }

    public void recordSetback(long tick) {
        this.lastSetbackTick = tick;
        this.setbackCount++;
        exemptions.grant(ExemptionType.SETBACK);
    }

    public boolean isDebugTarget() {
        return debugTarget;
    }

    public void setDebugTarget(boolean value) {
        this.debugTarget = value;
    }

    public void recordEvidence(EvidenceEntry.Builder builder) {
        evidence.record(builder.build(clock.currentTick(), clock.currentTimeMillis()));
    }

    public void resetTransientState() {
        movement.reset();
        combat.reset();
        interaction.reset();
        velocity.reset();
        for (ViolationTracker tracker : violationTrackers) {
            if (tracker != null) {
                tracker.reset();
            }
        }
        java.util.Arrays.fill(checkStates, null);
    }

    public void resetViolations() {
        for (ViolationTracker tracker : violationTrackers) {
            if (tracker != null) {
                tracker.resetCompletely();
            }
        }
    }
}
