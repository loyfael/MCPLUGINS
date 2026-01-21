package loyfael.utils;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.UUID;

public class TransactionManager {

    private static final ConcurrentHashMap<UUID, ReentrantLock> playerLocks = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, ReentrantLock> itemLocks = new ConcurrentHashMap<>();

    private static ReentrantLock getPlayerLock(UUID playerId) {
        return playerLocks.computeIfAbsent(playerId, k -> new ReentrantLock());
    }

    private static ReentrantLock getItemLock(String itemId) {
        return itemLocks.computeIfAbsent(itemId, k -> new ReentrantLock());
    }

    public static void executeTransaction(UUID playerId, String itemId, Runnable transaction) {
        ReentrantLock playerLock = getPlayerLock(playerId);
        ReentrantLock itemLock = getItemLock(itemId);

        // Toujours acquérir les locks dans le même ordre pour éviter les deadlocks
        ReentrantLock firstLock = playerId.toString().compareTo(itemId) < 0 ? playerLock : itemLock;
        ReentrantLock secondLock = playerId.toString().compareTo(itemId) < 0 ? itemLock : playerLock;

        firstLock.lock();
        try {
            secondLock.lock();
            try {
                transaction.run();
            } finally {
                secondLock.unlock();
            }
        } finally {
            firstLock.unlock();
        }
    }
}
