package io.github.no1qq.uagc.engine.evidence;

import io.github.no1qq.uagc.engine.util.RingBuffer;
import io.github.no1qq.uagc.engine.violation.Violation;

import java.util.ArrayList;
import java.util.List;

public final class EvidenceLog {

    private final RingBuffer<EvidenceEntry> entries;
    private final RingBuffer<Violation> violations;

    public EvidenceLog(int entryCapacity, int violationCapacity) {
        this.entries = new RingBuffer<>(entryCapacity);
        this.violations = new RingBuffer<>(violationCapacity);
    }

    public void record(EvidenceEntry entry) {
        entries.add(entry);
    }

    public void recordViolation(Violation violation) {
        violations.add(violation);
    }

    public List<EvidenceEntry> recentEntries(int limit) {
        return entries.newestFirst(limit);
    }

    public List<Violation> recentViolations(int limit) {
        return violations.newestFirst(limit);
    }

    public List<Violation> recentViolations(int limit, String checkId) {
        List<Violation> matched = new ArrayList<>();
        for (int i = 0; i < violations.size() && matched.size() < limit; i++) {
            Violation violation = violations.fromEnd(i);
            if (violation.checkId().equalsIgnoreCase(checkId)) {
                matched.add(violation);
            }
        }
        return matched;
    }

    public int violationCount() {
        return violations.size();
    }

    public int entryCount() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
        violations.clear();
    }
}
