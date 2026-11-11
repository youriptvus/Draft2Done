package io.draft2done.app;

import java.util.HashSet;
import java.util.Set;

/** Tracks queued requests before workers run, so an early cancellation cannot be lost. */
public final class RequestRegistry {
    private final Set<String> active = new HashSet<>();
    private final Set<String> cancelled = new HashSet<>();
    public synchronized void enqueue(String id) {
        if (id == null || id.isEmpty() || id.length() > 120) throw new IllegalArgumentException("Invalid request ID.");
        if (active.size() >= 12) throw new IllegalStateException("Too many active ChatGPT requests.");
        if (!active.add(id)) throw new IllegalStateException("Duplicate request ID.");
    }
    public synchronized boolean cancel(String id) {
        if (!active.contains(id)) return false;
        cancelled.add(id);
        return true;
    }
    public synchronized boolean isCancelled(String id) { return cancelled.contains(id); }
    public synchronized Set<String> activeExcept(String keep) {
        Set<String> result = new HashSet<>(active);
        result.remove(keep);
        return result;
    }
    public synchronized void finish(String id) { active.remove(id); cancelled.remove(id); }
}
