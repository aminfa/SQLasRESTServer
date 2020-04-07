package ai.libs.sqlrest;

import ai.libs.jaicore.db.sql.SQLAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ISQLAdapterCyclicAccess implements ISQLAdapterAccess {

    private final Map<String, AtomicInteger> tokenSQLAdapterIndexMap = new ConcurrentHashMap<>();

    private final SQLAdapterManager adapterManager;

    public ISQLAdapterCyclicAccess(SQLAdapterManager adapterManager) {
        this.adapterManager = adapterManager;
    }

    @Override
    public SQLAdapter acquire(String token) throws SQLException {
        AtomicInteger atomicIndex
                = tokenSQLAdapterIndexMap.computeIfAbsent(token, t -> new AtomicInteger(0));
        int currentIndex = atomicIndex.getAndIncrement();
        int readIndex = currentIndex;
        List<SQLAdapter> adapters = adapterManager.getAdaptersFor(token);
        int numAdapters = adapters.size();
        if(currentIndex < 0) {
            throw new IllegalStateException("The current index is negative: " + currentIndex);
        }
        while(currentIndex >= adapters.size()) {
            currentIndex = currentIndex % adapters.size();
        }
        atomicIndex.compareAndSet(readIndex, currentIndex + 1);
        return adapters.get(currentIndex);
    }

    @Override
    public void release(SQLAdapter adapter, String token) {
        // releasing doesn't do anything
    }
}
