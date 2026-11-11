package io.draft2done.app;

import java.io.InterruptedIOException;
import java.net.UnknownHostException;

/** Only DNS failures are replayable: no HTTP request reached the remote service. */
final class DnsRetry {
    interface Attempt<T> { T run() throws Exception; }
    interface Cancellation { void check() throws Exception; }
    interface Pause { void sleep(long milliseconds) throws InterruptedException; }
    private DnsRetry() {}

    static <T> T run(Attempt<T> attempt, Cancellation cancellation) throws Exception {
        return run(attempt, cancellation, Thread::sleep);
    }
    static <T> T run(Attempt<T> attempt, Cancellation cancellation, Pause pause) throws Exception {
        long[] delays = {300, 900};
        for (int index = 0; ; index++) {
            cancellation.check();
            try { return attempt.run(); }
            catch (UnknownHostException error) {
                if (index == delays.length) throw error;
                cancellation.check();
                try { pause.sleep(delays[index]); }
                catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    InterruptedIOException cancelled = new InterruptedIOException("Cancelled.");
                    cancelled.initCause(interrupted); throw cancelled;
                }
            }
        }
    }
}
