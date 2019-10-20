package no.ssb.dc.application.health;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.ssb.dc.api.health.HealthRenderPriority;
import no.ssb.dc.api.health.HealthResource;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@HealthRenderPriority(priority = 20)
public class HealthThreadsResource implements HealthResource {

    @Override
    public String name() {
        return "thread-info";
    }

    @Override
    public boolean isList() {
        return false;
    }

    @Override
    public boolean canRender(Map<String, Deque<String>> queryParams) {
        return queryParams.containsKey("threads");
    }

    @Override
    public Object resource() {
        List<ThreadInfo> threadInfoList = new ArrayList<>();

        Set<Thread> threads = Thread.getAllStackTraces().keySet();

        for (Thread thread : threads) {
            String name = thread.getName();
            Thread.State state = thread.getState();
            int priority = thread.getPriority();
            String type = thread.isDaemon() ? "Daemon" : "Normal";
            threadInfoList.add(new ThreadInfo(
                    thread.getId(),
                    name,
                    state,
                    priority,
                    thread.isAlive(),
                    thread.isInterrupted(),
                    type
            ));
        }

        return new ThreadStatus(threadInfoList.size(), threadInfoList);
    }

    static class ThreadStatus {
        @JsonProperty("thread-count") public final Integer threadCount;
        @JsonProperty("threads") public final List<ThreadInfo> threadInfoList;

        public ThreadStatus(Integer threadCount, List<ThreadInfo> threadInfoList) {
            this.threadCount = threadCount;
            this.threadInfoList = threadInfoList;
        }
    }
    static class ThreadInfo {
        @JsonProperty public final Long id;
        @JsonProperty public final String name;
        @JsonProperty public final Thread.State state;
        @JsonProperty public final Integer priority;
        @JsonProperty public final Boolean alive;
        @JsonProperty public final Boolean interrupted;
        @JsonProperty public final String type;

        public ThreadInfo(Long id, String name, Thread.State state, Integer priority, Boolean alive, Boolean interrupted, String type) {
            this.id = id;
            this.name = name;
            this.state = state;
            this.priority = priority;
            this.alive = alive;
            this.interrupted = interrupted;
            this.type = type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ThreadInfo that = (ThreadInfo) o;
            return Objects.equals(id, that.id) &&
                    state == that.state &&
                    Objects.equals(priority, that.priority) &&
                    Objects.equals(alive, that.alive) &&
                    Objects.equals(interrupted, that.interrupted) &&
                    Objects.equals(type, that.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id, state, priority, alive, interrupted, type);
        }

        @Override
        public String toString() {
            return "ThreadInfo{" +
                    "id='" + id + '\'' +
                    ", state=" + state +
                    ", priority=" + priority +
                    ", alive=" + alive +
                    ", interrupted=" + interrupted +
                    ", type='" + type + '\'' +
                    '}';
        }
    }
}
