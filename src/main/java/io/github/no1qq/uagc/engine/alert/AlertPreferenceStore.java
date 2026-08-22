package io.github.no1qq.uagc.engine.alert;

import java.util.Map;
import java.util.UUID;

public interface AlertPreferenceStore {

    AlertPreferenceStore MEMORY_ONLY = new AlertPreferenceStore() {
        @Override
        public Map<UUID, Boolean> load() {
            return Map.of();
        }

        @Override
        public void save(Map<UUID, Boolean> preferences) {
        }
    };

    Map<UUID, Boolean> load();

    void save(Map<UUID, Boolean> preferences);
}
