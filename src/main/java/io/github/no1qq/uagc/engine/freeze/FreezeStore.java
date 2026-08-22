package io.github.no1qq.uagc.engine.freeze;

import java.util.Collection;
import java.util.List;

public interface FreezeStore {

    FreezeStore MEMORY_ONLY = new FreezeStore() {
        @Override
        public List<FreezeRecord> load() {
            return List.of();
        }

        @Override
        public void save(Collection<FreezeRecord> records) {
        }
    };

    List<FreezeRecord> load();

    void save(Collection<FreezeRecord> records);
}
