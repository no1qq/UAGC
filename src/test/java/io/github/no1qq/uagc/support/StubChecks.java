package io.github.no1qq.uagc.support;

import io.github.no1qq.uagc.engine.check.Check;
import io.github.no1qq.uagc.engine.check.CheckCategory;
import io.github.no1qq.uagc.engine.check.CheckContext;
import io.github.no1qq.uagc.engine.check.CheckDefinition;
import io.github.no1qq.uagc.engine.check.CheckResult;
import io.github.no1qq.uagc.engine.check.event.MovementEvent;

public final class StubChecks {

    private StubChecks() {
    }

    public static final class AlwaysFlags implements Check<MovementEvent, Void> {

        private final CheckDefinition definition;
        private final double severity;
        private final boolean setback;

        public AlwaysFlags(String id, double severity, boolean setback) {
            this.definition = CheckDefinition.builder(id, id, CheckCategory.MOVEMENT).build();
            this.severity = severity;
            this.setback = setback;
        }

        @Override
        public CheckDefinition definition() {
            return definition;
        }

        @Override
        public Class<MovementEvent> eventType() {
            return MovementEvent.class;
        }

        @Override
        public CheckResult inspect(CheckContext context, MovementEvent event, Void state) {
            CheckResult.Builder builder = CheckResult.flag(severity, "stub detection")
                    .with("severity", severity);
            if (setback) {
                builder.setback();
            }
            return builder.build();
        }
    }

    public static final class AlwaysThrows implements Check<MovementEvent, Void> {

        private final CheckDefinition definition;
        private int invocations;

        public AlwaysThrows(String id) {
            this.definition = CheckDefinition.builder(id, id, CheckCategory.MOVEMENT).build();
        }

        @Override
        public CheckDefinition definition() {
            return definition;
        }

        @Override
        public Class<MovementEvent> eventType() {
            return MovementEvent.class;
        }

        @Override
        public CheckResult inspect(CheckContext context, MovementEvent event, Void state) {
            invocations++;
            throw new IllegalStateException("intentional failure");
        }

        public int invocations() {
            return invocations;
        }
    }

    public static final class NeverFlags implements Check<MovementEvent, Void> {

        private final CheckDefinition definition;
        private int invocations;

        public NeverFlags(String id) {
            this.definition = CheckDefinition.builder(id, id, CheckCategory.MOVEMENT).build();
        }

        @Override
        public CheckDefinition definition() {
            return definition;
        }

        @Override
        public Class<MovementEvent> eventType() {
            return MovementEvent.class;
        }

        @Override
        public CheckResult inspect(CheckContext context, MovementEvent event, Void state) {
            invocations++;
            return CheckResult.passed();
        }

        public int invocations() {
            return invocations;
        }
    }
}
