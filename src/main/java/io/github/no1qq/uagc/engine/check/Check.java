package io.github.no1qq.uagc.engine.check;

public interface Check<E extends CheckEvent, S> {

    CheckDefinition definition();

    Class<E> eventType();

    CheckResult inspect(CheckContext context, E event, S state);

    default S createState() {
        return null;
    }

    default boolean ignoresExemptions() {
        return false;
    }
}
