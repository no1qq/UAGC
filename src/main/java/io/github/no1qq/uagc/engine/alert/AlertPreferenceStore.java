package io.github.no1qq.uagc.engine.alert;

public interface AlertPreferenceStore {

    AlertPreferenceStore MEMORY_ONLY = new AlertPreferenceStore() {
        @Override
        public AlertPreferences load() {
            return AlertPreferences.empty();
        }

        @Override
        public void save(AlertPreferences preferences) {
        }
    };

    AlertPreferences load();

    void save(AlertPreferences preferences);
}
