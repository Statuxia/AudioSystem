package com.audiosystem.wsgateway.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRegistryServiceTest {

    private final SubscriptionRegistryService service = new SubscriptionRegistryService();

    @Test
    void testGetAllSubscribedJobsImmutableSet() {
        final UUID jobId = new UUID(0, 0);
        final UUID jobId2 = new UUID(0, 1);

        service.subscribe("sess", "sub", jobId);
        final Set<UUID> allSubscribedJobs = service.getAllSubscribedJobs();

        assertEquals(Set.of(jobId), allSubscribedJobs);
        assertThrows(UnsupportedOperationException.class, () -> allSubscribedJobs.add(jobId2));
    }

    @Test
    void testUpdateQueuePositionNotSubscribed() {
        final UUID jobId = new UUID(0, 0);
        final UUID jobId2 = new UUID(0, 1);

        service.subscribe("sess", "sub", jobId2);
        final OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testUpdateQueueEmptyJobArg() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        final OptionalLong queuePosition = service.updateQueuePosition(null, 3);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testUpdateQueuePositionSubscribed() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        final OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUpdateQueuePositionIncreased() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        queuePosition = service.updateQueuePosition(jobId, 5);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUpdateQueuePositionDecreased() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        queuePosition = service.updateQueuePosition(jobId, 2);

        assertTrue(queuePosition.isPresent());
        assertEquals(2L, queuePosition.getAsLong());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({
        "00000000-0000-0000-0000-000000000001,00000000-0000-0000-0000-000000000000,diff jobId",
        "00000000-0000-0000-0000-000000000000,00000000-0000-0000-0000-000000000000,position unknown",
        "00000000-0000-0000-0000-000000000000,,empty job"
    })
    void testGetQueuePositionNotSubscribed(UUID subscriptionJobId, UUID requestedJobId, String description) {
        service.subscribe("sess", "sub", subscriptionJobId);
        final OptionalLong queuePosition = service.getQueuePosition(requestedJobId);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testGetQueuePositionSubscribedAndPositionUpdated() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        service.updateQueuePosition(jobId, 3);

        final OptionalLong queuePosition = service.getQueuePosition(jobId);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @ParameterizedTest
    @CsvSource({"sess,sub,", "sess,,00000000-0000-0000-0000-000000000000", ",sub,00000000-0000-0000-0000-000000000000", ",,"})
    void testSubscribeNoText(String sess, String sub, UUID job) {
        service.subscribe(sess, sub, job);
        final Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        assertTrue(subscribedJobs.isEmpty());
    }

    @Test
    void testSubscribeSameSubscriptionOneCount() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.subscribe("sess", "sub", jobId);
        subscriptionCount = service.getSubscriptionCount(jobId);
        queuePosition = service.getQueuePosition(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testSubscribeDoubleSubscription() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        OptionalLong queuePosition = service.updateQueuePosition(jobId, 3);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.subscribe("sess", "sub2", jobId);
        subscriptionCount = service.getSubscriptionCount(jobId);
        queuePosition = service.getQueuePosition(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testSubscribeWithDiffJobId() {
        final UUID jobId = new UUID(0, 0);
        final UUID jobId2 = new UUID(0, 1);

        service.subscribe("sess", "sub", jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.subscribe("sess", "sub", jobId2);
        subscriptionCount = service.getSubscriptionCount(jobId);
        final OptionalInt subscription2Count = service.getSubscriptionCount(jobId2);

        assertTrue(subscriptionCount.isPresent());
        assertTrue(subscription2Count.isEmpty());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testSubscribeWithDiffSubscriptionId() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.subscribe("sess", "sub2", jobId);
        subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
    }

    @Test
    void testSubscribeAfterUnsubscribe() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");
        subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isEmpty());

        service.subscribe("sess", "sub", jobId);
        subscriptionCount = service.getSubscriptionCount(jobId);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testUnsubscribeUnknown() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess2", "sub", jobId);

        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");

        subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @ParameterizedTest
    @CsvSource({"sess,", ",sub", ",,"})
    void testUnsubscribeNoText(String sess, String sub) {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        assertFalse(subscribedJobs.isEmpty());

        service.unsubscribe(sess, sub);

        subscribedJobs = service.getAllSubscribedJobs();
        assertFalse(subscribedJobs.isEmpty());
    }

    @Test
    void testUnsubscribeAndRemoveJob() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isEmpty());
    }

    @Test
    void testUnsubscribeAndDecreaseCounter() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub", jobId);
        service.subscribe("sess", "sub2", jobId);
        service.updateQueuePosition(jobId, 3);

        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalLong queuePosition = service.getQueuePosition(jobId);
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        queuePosition = service.getQueuePosition(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUnsubscribeDifferentSubscription() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub2", jobId);
        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testDisconnectNoText(String sess) {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub2", jobId);
        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.disconnect(sess);

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testDisconnectUnknown() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess2", "sub", jobId);

        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.disconnect("sess");

        subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testDisconnectAllSubscriptions() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub1", jobId);
        service.subscribe("sess", "sub2", jobId);
        service.subscribe("sess", "sub3", jobId);

        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(3, subscriptionCount.getAsInt());

        service.disconnect("sess");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        assertTrue(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isEmpty());
    }

    @Test
    void testDisconnectDecreaseCounter() {
        final UUID jobId = new UUID(0, 0);

        service.subscribe("sess", "sub1", jobId);
        service.subscribe("sess2", "sub2", jobId);

        Set<UUID> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());

        service.disconnect("sess");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount(jobId);
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }
}