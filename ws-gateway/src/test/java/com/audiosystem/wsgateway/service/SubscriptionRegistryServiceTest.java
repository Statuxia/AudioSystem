package com.audiosystem.wsgateway.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class SubscriptionRegistryServiceTest {

    private final SubscriptionRegistryService service = new SubscriptionRegistryService();

    @Test
    void testGetAllSubscribedJobsImmutableSet() {
        service.subscribe("sess", "sub", "job");
        final Set<String> allSubscribedJobs = service.getAllSubscribedJobs();

        assertEquals(Set.of("job"), allSubscribedJobs);
        assertThrows(UnsupportedOperationException.class, () -> allSubscribedJobs.add("job2"));
    }

    @Test
    void testUpdateQueuePositionNotSubscribed() {
        service.subscribe("sess", "sub", "job2");
        final OptionalLong queuePosition = service.updateQueuePosition("job", 3);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testUpdateQueueEmptyJobArg() {
        service.subscribe("sess", "sub", "job");
        final OptionalLong queuePosition = service.updateQueuePosition("", 3);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testUpdateQueuePositionSubscribed() {
        service.subscribe("sess", "sub", "job");
        final OptionalLong queuePosition = service.updateQueuePosition("job", 3);
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUpdateQueuePositionIncreased() {
        service.subscribe("sess", "sub", "job");
        OptionalLong queuePosition = service.updateQueuePosition("job", 3);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        queuePosition = service.updateQueuePosition("job", 5);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUpdateQueuePositionDecreased() {
        service.subscribe("sess", "sub", "job");
        OptionalLong queuePosition = service.updateQueuePosition("job", 3);

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        queuePosition = service.updateQueuePosition("job", 2);

        assertTrue(queuePosition.isPresent());
        assertEquals(2L, queuePosition.getAsLong());
    }

    @ParameterizedTest(name = "{2}")
    @CsvSource({"job2,job,diff jobId", "job,job,position unknown", "job,,empty job"})
    void testGetQueuePositionNotSubscribed(String subscriptionJobId, String requestedJobId, String description) {
        service.subscribe("sess", "sub", subscriptionJobId);
        final OptionalLong queuePosition = service.getQueuePosition(requestedJobId);
        assertTrue(queuePosition.isEmpty());
    }

    @Test
    void testGetQueuePositionSubscribedAndPositionUpdated() {
        service.subscribe("sess", "sub", "job");
        service.updateQueuePosition("job", 3);

        final OptionalLong queuePosition = service.getQueuePosition("job");

        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @ParameterizedTest
    @CsvSource({"sess,sub,", "sess,,job", ",sub,job", ",,"})
    void testSubscribeNoText(String sess, String sub, String job) {
        service.subscribe(sess, sub, job);
        final Set<String> subscribedJobs = service.getAllSubscribedJobs();
        assertTrue(subscribedJobs.isEmpty());
    }

    @Test
    void testSubscribeSameSubscriptionOneCount() {
        service.subscribe("sess", "sub", "job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        OptionalLong queuePosition = service.updateQueuePosition("job", 3);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.subscribe("sess", "sub", "job");
        subscriptionCount = service.getSubscriptionCount("job");
        queuePosition = service.getQueuePosition("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testSubscribeDoubleSubscription() {
        service.subscribe("sess", "sub", "job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        OptionalLong queuePosition = service.updateQueuePosition("job", 3);

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.subscribe("sess", "sub2", "job");
        subscriptionCount = service.getSubscriptionCount("job");
        queuePosition = service.getQueuePosition("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testSubscribeWithDiffJobId() {
        service.subscribe("sess", "sub", "job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.subscribe("sess", "sub", "job2");
        subscriptionCount = service.getSubscriptionCount("job");
        final OptionalInt subscription2Count = service.getSubscriptionCount("job2");

        assertTrue(subscriptionCount.isPresent());
        assertTrue(subscription2Count.isEmpty());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testSubscribeWithDiffSubscriptionId() {
        service.subscribe("sess", "sub", "job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.subscribe("sess", "sub2", "job");
        subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
    }

    @Test
    void testSubscribeAfterUnsubscribe() {
        service.subscribe("sess", "sub", "job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");
        subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isEmpty());

        service.subscribe("sess", "sub", "job");
        subscriptionCount = service.getSubscriptionCount("job");

        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testUnsubscribeUnknown() {
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscriptionCount.isEmpty());

        service.unsubscribe("sess", "sub");

        subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscriptionCount.isEmpty());
    }

    @ParameterizedTest
    @CsvSource({"sess,", ",sub", ",,"})
    void testUnsubscribeNoText(String sess, String sub) {
        service.subscribe("sess", "sub", "job");
        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        assertFalse(subscribedJobs.isEmpty());

        service.unsubscribe(sess, sub);

        subscribedJobs = service.getAllSubscribedJobs();
        assertFalse(subscribedJobs.isEmpty());
    }

    @Test
    void testUnsubscribeAndRemoveJob() {
        service.subscribe("sess", "sub", "job");
        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isEmpty());
    }

    @Test
    void testUnsubscribeAndDecreaseCounter() {
        service.subscribe("sess", "sub", "job");
        service.subscribe("sess", "sub2", "job");
        service.updateQueuePosition("job", 3);

        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalLong queuePosition = service.getQueuePosition("job");
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        queuePosition = service.getQueuePosition("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
        assertTrue(queuePosition.isPresent());
        assertEquals(3L, queuePosition.getAsLong());
    }

    @Test
    void testUnsubscribeDifferentSubscription() {
        service.subscribe("sess", "sub2", "job");
        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.unsubscribe("sess", "sub");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @ParameterizedTest
    @NullAndEmptySource
    void testDisconnectNoText(String sess) {
        service.subscribe("sess", "sub2", "job");
        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());

        service.disconnect(sess);

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }

    @Test
    void testDisconnectUnknown() {
        service.subscribe("sess2", "sub", "job");

        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscriptionCount.isPresent());

        service.disconnect("sess");

        subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscriptionCount.isPresent());
    }

    @Test
    void testDisconnectAllSubscriptions() {
        service.subscribe("sess", "sub1", "job");
        service.subscribe("sess", "sub2", "job");
        service.subscribe("sess", "sub3", "job");

        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(3, subscriptionCount.getAsInt());

        service.disconnect("sess");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        assertTrue(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isEmpty());
    }

    @Test
    void testDisconnectDecreaseCounter() {
        service.subscribe("sess", "sub1", "job");
        service.subscribe("sess2", "sub2", "job");

        Set<String> subscribedJobs = service.getAllSubscribedJobs();
        OptionalInt subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(2, subscriptionCount.getAsInt());

        service.disconnect("sess");

        subscribedJobs = service.getAllSubscribedJobs();
        subscriptionCount = service.getSubscriptionCount("job");
        assertFalse(subscribedJobs.isEmpty());
        assertTrue(subscriptionCount.isPresent());
        assertEquals(1, subscriptionCount.getAsInt());
    }
}