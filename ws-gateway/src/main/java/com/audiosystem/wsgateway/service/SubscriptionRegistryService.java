package com.audiosystem.wsgateway.service;

import com.audiosystem.wsgateway.dto.SubscribedJobInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

@Service
@RequiredArgsConstructor
@Log4j2
public class SubscriptionRegistryService {

    private final Map<String, Map<String, String>> sessionSubscriptionJobRegistry = new ConcurrentHashMap<>();
    private final Map<String, SubscribedJobInfo> subscribedJobInfoRegistry = new ConcurrentHashMap<>();

    public Set<String> getAllSubscribedJobs() {
        return Set.copyOf(subscribedJobInfoRegistry.keySet());
    }

    public OptionalLong updateQueuePosition(String jobId, long position) {
        if (!hasText(jobId, "jobId")) return OptionalLong.empty();
        final SubscribedJobInfo info = subscribedJobInfoRegistry.computeIfPresent(
            jobId, (k, v) -> new SubscribedJobInfo(v.count(), Math.min(position, v.lastQueuePosition()))
        );
        return info == null ? OptionalLong.empty() : OptionalLong.of(info.lastQueuePosition());
    }

    public OptionalLong getQueuePosition(String jobId) {
        if (!hasText(jobId, "jobId")) return OptionalLong.empty();
        final SubscribedJobInfo info = subscribedJobInfoRegistry.get(jobId);
        return info == null || info.lastQueuePosition() == Long.MAX_VALUE
            ? OptionalLong.empty() : OptionalLong.of(info.lastQueuePosition());
    }

    public void subscribe(String sessionId, String subscriptionId, String jobId) {
        if (!hasText(sessionId, "sessionId")
            || !hasText(subscriptionId, "subscriptionId")
            || !hasText(jobId, "jobId")
        ) {
            return;
        }

        sessionSubscriptionJobRegistry.compute(
            sessionId, (sess, map) -> {
                map = map == null ? new ConcurrentHashMap<>() : map;

                if (map.putIfAbsent(subscriptionId, jobId) == null) {
                    subscribedJobInfoRegistry.compute(
                        jobId, (k, v) -> v == null
                            ? new SubscribedJobInfo(1, Long.MAX_VALUE)
                            : new SubscribedJobInfo(v.count() + 1, v.lastQueuePosition())
                    );
                }

                return map;
            }
        );
    }

    public void unsubscribe(String sessionId, String subscriptionId) {
        if (!hasText(sessionId, "sessionId") || !hasText(subscriptionId, "subscriptionId")) {
            return;
        }

        sessionSubscriptionJobRegistry.computeIfPresent(
            sessionId, (sess, map) -> {
                final String jobId = map.remove(subscriptionId);
                if (jobId != null) {
                    subscribedJobInfoRegistry.compute(jobId, decrementFunction());
                }
                return map.isEmpty() ? null : map;
            }
        );
    }

    public void disconnect(String sessionId) {
        if (!hasText(sessionId, "sessionId")) {
            return;
        }

        sessionSubscriptionJobRegistry.compute(
            sessionId, (sess, map) -> {
                if (map != null) {
                    map.forEach((sub, job) -> subscribedJobInfoRegistry.compute(job, decrementFunction()));
                }
                return null;
            }
        );
    }

    /**
     * for testing only
     */
    OptionalInt getSubscriptionCount(String jobId) {
        final SubscribedJobInfo info = subscribedJobInfoRegistry.get(jobId);
        return info == null ? OptionalInt.empty() : OptionalInt.of(info.count());
    }

    private BiFunction<String, SubscribedJobInfo, SubscribedJobInfo> decrementFunction() {
        return (k, v) -> v == null || v.count() == 1
            ? null
            : new SubscribedJobInfo(v.count() - 1, v.lastQueuePosition());
    }

    private boolean hasText(String value, String fieldName) {
        if (StringUtils.hasText(value)) {
            return true;
        }
        log.debug("{} is empty. Skip", fieldName);
        return false;
    }
}
