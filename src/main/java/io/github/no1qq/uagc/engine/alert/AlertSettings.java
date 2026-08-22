package io.github.no1qq.uagc.engine.alert;

import io.github.no1qq.uagc.engine.check.CheckCategory;

import java.util.EnumSet;
import java.util.Set;

public final class AlertSettings {

    private volatile boolean enabled;
    private volatile double minimumConfidence;
    private volatile double minimumViolationLevel;
    private final Set<CheckCategory> mutedCategories = EnumSet.noneOf(CheckCategory.class);
    private final Set<String> mutedChecks = java.util.concurrent.ConcurrentHashMap.newKeySet();
    private volatile boolean verbose;

    public AlertSettings(boolean enabled, double minimumConfidence, double minimumViolationLevel) {
        this.enabled = enabled;
        this.minimumConfidence = minimumConfidence;
        this.minimumViolationLevel = minimumViolationLevel;
    }

    public boolean enabled() {
        return enabled;
    }

    public void setEnabled(boolean value) {
        this.enabled = value;
    }

    public boolean toggle() {
        enabled = !enabled;
        return enabled;
    }

    public double minimumConfidence() {
        return minimumConfidence;
    }

    public void setMinimumConfidence(double value) {
        this.minimumConfidence = value;
    }

    public double minimumViolationLevel() {
        return minimumViolationLevel;
    }

    public void setMinimumViolationLevel(double value) {
        this.minimumViolationLevel = value;
    }

    public boolean verbose() {
        return verbose;
    }

    public void setVerbose(boolean value) {
        this.verbose = value;
    }

    public boolean isMuted(CheckCategory category, String checkId) {
        synchronized (mutedCategories) {
            if (mutedCategories.contains(category)) {
                return true;
            }
        }
        return mutedChecks.contains(checkId);
    }

    public boolean toggleCategory(CheckCategory category) {
        synchronized (mutedCategories) {
            if (mutedCategories.contains(category)) {
                mutedCategories.remove(category);
                return true;
            }
            mutedCategories.add(category);
            return false;
        }
    }

    public boolean toggleCheck(String checkId) {
        if (mutedChecks.contains(checkId)) {
            mutedChecks.remove(checkId);
            return true;
        }
        mutedChecks.add(checkId);
        return false;
    }

    public Set<CheckCategory> mutedCategories() {
        synchronized (mutedCategories) {
            return EnumSet.copyOf(mutedCategories.isEmpty() ? EnumSet.noneOf(CheckCategory.class) : mutedCategories);
        }
    }

    public Set<String> mutedChecks() {
        return Set.copyOf(mutedChecks);
    }
}
