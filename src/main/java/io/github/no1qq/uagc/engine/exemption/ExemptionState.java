package io.github.no1qq.uagc.engine.exemption;

import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.platform.UagcClock;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReferenceArray;

public final class ExemptionState {

    private static final ExemptionType[] TYPES = ExemptionType.values();

    private final AtomicReferenceArray<ExemptionGrant> grants = new AtomicReferenceArray<>(TYPES.length);
    private final UagcClock clock;

    public ExemptionState(UagcClock clock) {
        this.clock = clock;
    }

    public ExemptionGrant grant(ExemptionType type, int durationTicks, String source, String reason) {
        if (type == null) {
            return null;
        }
        int ticks = durationTicks > 0 ? durationTicks : type.defaultDurationTicks();
        if (ticks <= 0) {
            return null;
        }
        long tick = clock.currentTick();
        ExemptionGrant fresh = new ExemptionGrant(type, tick, tick + ticks, clock.currentTimeMillis(), source, reason);
        int index = type.ordinal();
        while (true) {
            ExemptionGrant existing = grants.get(index);
            if (existing != null && existing.expiresTick() >= fresh.expiresTick() && existing.isActive(tick)) {
                return existing;
            }
            if (grants.compareAndSet(index, existing, fresh)) {
                return fresh;
            }
        }
    }

    public ExemptionGrant grant(ExemptionType type) {
        return grant(type, type.defaultDurationTicks(), "uagc", "");
    }

    public ExemptionGrant grant(ExemptionType type, int durationTicks) {
        return grant(type, durationTicks, "uagc", "");
    }

    public void revoke(ExemptionType type) {
        if (type != null) {
            grants.set(type.ordinal(), null);
        }
    }

    public void revokeAll() {
        for (int i = 0; i < grants.length(); i++) {
            grants.set(i, null);
        }
    }

    public boolean isExempt(ExemptionType type) {
        if (type == null) {
            return false;
        }
        ExemptionGrant grant = grants.get(type.ordinal());
        return grant != null && grant.isActive(clock.currentTick());
    }

    public boolean isExemptFromAny(ExemptionType... types) {
        long tick = clock.currentTick();
        for (ExemptionType type : types) {
            ExemptionGrant grant = grants.get(type.ordinal());
            if (grant != null && grant.isActive(tick)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCategoryExempt(CheckCategory category) {
        long tick = clock.currentTick();
        for (int i = 0; i < TYPES.length; i++) {
            ExemptionGrant grant = grants.get(i);
            if (grant != null && grant.isActive(tick) && TYPES[i].affects(category)) {
                return true;
            }
        }
        return false;
    }

    public ExemptionGrant grantOf(ExemptionType type) {
        if (type == null) {
            return null;
        }
        ExemptionGrant grant = grants.get(type.ordinal());
        return grant != null && grant.isActive(clock.currentTick()) ? grant : null;
    }

    public long ticksSinceExpiry(ExemptionType type) {
        ExemptionGrant grant = grants.get(type.ordinal());
        if (grant == null) {
            return Long.MAX_VALUE;
        }
        long tick = clock.currentTick();
        return tick < grant.expiresTick() ? 0L : tick - grant.expiresTick();
    }

    public List<ExemptionGrant> active() {
        long tick = clock.currentTick();
        List<ExemptionGrant> active = new ArrayList<>();
        for (int i = 0; i < TYPES.length; i++) {
            ExemptionGrant grant = grants.get(i);
            if (grant != null && grant.isActive(tick)) {
                active.add(grant);
            }
        }
        return active;
    }

    public Map<CheckCategory, Integer> activeByCategory() {
        Map<CheckCategory, Integer> counts = new EnumMap<>(CheckCategory.class);
        for (ExemptionGrant grant : active()) {
            for (CheckCategory category : grant.type().categories()) {
                counts.merge(category, 1, Integer::sum);
            }
        }
        return counts;
    }
}
