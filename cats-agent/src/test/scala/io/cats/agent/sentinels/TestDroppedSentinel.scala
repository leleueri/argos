package io.cats.agent.sentinels

import akka.actor.{ActorRef, Props, ActorSystem}
import akka.testkit.{ImplicitSender, TestProbe, TestKit}
import com.typesafe.config.ConfigFactory
import io.cats.agent.Constants._
import io.cats.agent.Messages
import io.cats.agent.bean.ActorProtocol._
import io.cats.agent.bean._
import org.scalatest._
import org.scalatest.words.ShouldVerb

/**
 * Created by eric on 05/07/16.
 */
class TestDroppedSentinel extends TestKit(ActorSystem("TestDroppedSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val droppedCounterActor = system.actorOf(Props(classOf[DroppedCounterSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER)), name = "DroppedCounterSentinel")
  val droppedMutationActor = system.actorOf(Props(classOf[DroppedMutationSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION)), name = "DroppedMutationSentinel")
  val droppedReadActor = system.actorOf(Props(classOf[DroppedReadSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ)), name = "DroppedReadSentinel")
  val droppedReadRepairActor = system.actorOf(Props(classOf[DroppedReadRepairSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR)), name = "DroppedReadRepairSentinel")
  val droppedPageActor = system.actorOf(Props(classOf[DroppedPageRangeSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE)), name = "DroppedPageRangeSentinel")
  val droppedRangeActor = system.actorOf(Props(classOf[DroppedRangeSliceSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE)), name = "DroppedRangeSliceSentinel")
  val droppedReqRespActor = system.actorOf(Props(classOf[DroppedRequestResponseSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP)), name = "DroppedRequestResponseSentinel")

  override def afterAll() {
    system.terminate()
  }

  "A notification " should "be sent on COUNTER_MUTATION dropped message" in {
    executeTest(droppedCounterActor, Messages.DROPPED_MESSAGE_COUNTER_MUTATION)
  }

  "A notification " should "be sent on MUTATION dropped message" in {
    executeTest(droppedMutationActor, Messages.DROPPED_MESSAGE_MUTATION)
  }

  "A notification " should "be sent on READ dropped message" in {
    executeTest(droppedReadActor, Messages.DROPPED_MESSAGE_READ)
  }

  "A notification " should "be sent on READ_REPAIR dropped message" in {
    executeTest(droppedReadRepairActor, Messages.DROPPED_MESSAGE_READ_REPAIR)
  }

  "A notification " should "be sent on PAGE_RANGE dropped message" in {
    executeTest(droppedPageActor, Messages.DROPPED_MESSAGE_PAGED_RANGE)
  }

  "A notification " should "be sent on RANGE_SLICE dropped message" in {
    executeTest(droppedRangeActor, Messages.DROPPED_MESSAGE_RANGE_SLICE)
  }

  "A notification " should "be sent on REQUEST_RESPONSE dropped message" in {
    executeTest(droppedReqRespActor, Messages.DROPPED_MESSAGE_REQUEST_RESPONSE)
  }

  private def executeTest(testableActorRef : ActorRef, dmType: String): Unit = {
    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, dmType))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_DROPPED_MESSAGES, Some(DroppedMessageStats(dmType, 0, 0, 0, 0, 0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_DROPPED_MESSAGES, dmType))
    // non zero one minute rate should trigger a notification
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_DROPPED_MESSAGES, Some(DroppedMessageStats(dmType, 1, 0.0, 0.0, 0.0, 0.1))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains(dmType))
  }
}
