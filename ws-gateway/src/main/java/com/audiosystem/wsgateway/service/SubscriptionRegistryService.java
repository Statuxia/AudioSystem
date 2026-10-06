package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.SubscribedJobInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Log4j2
public class SubscriptionRegistryService {

    private final Map<String, Map<String, UUID>> sessionSubscriptionJobRegistry = new ConcurrentHashMap<>();
    private final Map<UUID, SubscribedJobInfo> subscribedJobInfoRegistry = new ConcurrentHashMap<>();

    public Set<UUID> getAllSubscribedJobs() {
        return Set.copyOf(subscribedJobInfoRegistry.keySet());
    }

    public OptionalLong updateQueuePosition(UUID jobId, long position) {
        if (noJobId(jobId)) return OptionalLong.empty();
        final SubscribedJobInfo info = subscribedJobInfoRegistry.computeIfPresent(
            jobId, (_, v) -> new SubscribedJobInfo(v.count(), Math.min(position, v.lastQueuePosition()))
        );
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.lastQueuePosition());
    }

    public OptionalLong decreaseQueuePosition(UUID jobId, long position) {
        if (noJobId(jobId)) return OptionalLong.empty();

        final AtomicBoolean decreased = new AtomicBoolean();
        final SubscribedJobInfo info = subscribedJobInfoRegistry.computeIfPresent(
            jobId, (_, v) -> {
                if (position >= v.lastQueuePosition()) {
                    return v;
                }
                decreased.set(true);
                return new SubscribedJobInfo(v.count(), position);
            }
        );
        return info == null || !decreased.get() ? OptionalLong.empty() : OptionalLong.of(info.lastQueuePosition());
    }

    public OptionalLong getQueuePosition(UUID jobId) {
        if (noJobId(jobId)) return OptionalLong.empty();
        final SubscribedJobInfo info = subscribedJobInfoRegistry.get(jobId);
        return info == null || info.lastQueuePosition() == Long.MAX_VALUE
            ? OptionalLong.empty() : OptionalLong.of(info.lastQueuePosition());
    }

    public void subscribe(String sessionId, String subscriptionId, UUID jobId) {
        if (noText(sessionId, "sessionId")
            || noText(subscriptionId, "subscriptionId")
            || noJobId(jobId)
        ) {
            return;
        }

        sessionSubscriptionJobRegistry.compute(
            sessionId, (_, map) -> {
                map = map == null ? new ConcurrentHashMap<>() : map;

                if (map.putIfAbsent(subscriptionId, jobId) == null) {
                    subscribedJobInfoRegistry.compute(
                        jobId, (_, v) -> v == null
                            ? new SubscribedJobInfo(1, Long.MAX_VALUE)
                            : new SubscribedJobInfo(v.count() + 1, v.lastQueuePosition())
                    );
                }

                return map;
            }
        );
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        if (noText(sessionId, "sessionId") || noText(subscriptionId, "subscriptionId")) {
            return;
        }

        sessionSubscriptionJobRegistry.computeIfPresent(
            sessionId, (_, map) -> {
                final UUID jobId = map.remove(subscriptionId);
                if (jobId != null) {
                    subscribedJobInfoRegistry.compute(jobId, decrementFunction());
                }
                return map.isEmpty() ? null : map;
            }
        );
    }

    public void disconnect(String sessionId) {
        if (noText(sessionId, "sessionId")) {
            return;
        }

        sessionSubscriptionJobRegistry.compute(
            sessionId, (_, map) -> {
                if (map != null) {
                    map.forEach((_, job) -> subscribedJobInfoRegistry.compute(job, decrementFunction()));
                }
                return null;
            }
        );
    }

    /**
     * for testing only
     */
    OptionalInt getSubscriptionCount(UUID jobId) {
        final SubscribedJobInfo info = subscribedJobInfoRegistry.get(jobId);
        return info == null ? OptionalInt.empty() : OptionalInt.of(info.count());
    }

    private BiFunction<UUID, SubscribedJobInfo, SubscribedJobInfo> decrementFunction() {
        return (_, v) -> v == null || v.count() == 1
            ? null
            : new SubscribedJobInfo(v.count() - 1, v.lastQueuePosition());
    }

    private boolean noText(String value, String fieldName) {
        if (StringUtils.hasText(value)) {
            return false;
        }
        log.debug("{} is empty. Skip", fieldName);
        return true;
    }

    private boolean noJobId(UUID jobId) {
        if (jobId == null) {
            log.debug("jobId is null. Skip");
            return true;
        }
        return false;
    }
}
