package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.{Messages, Constants}
import io.argos.agent.bean._
import Constants._
import ActorProtocol._
import io.argos.agent.bean._
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestBlockedSentinel extends TestKit(ActorSystem("TestBlockedSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val blockedCounterActor = system.actorOf(Props(classOf[CounterMutationBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_COUNTER_MUTATION)))
  val blockedGossipActor = system.actorOf(Props(classOf[GossipBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_GOSSIP)))
  val blockedMutationActor = system.actorOf(Props(classOf[MutationBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_MUTATION)))
  val blockedReadActor = system.actorOf(Props(classOf[ReadBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_READ)))
  val blockedReadRepairActor = system.actorOf(Props(classOf[ReadRepairBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_READ_REPAIR)))
  val blockedInternalActor = system.actorOf(Props(classOf[InternalResponseBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_INTERNAL)))
  val blockedMemtableActor = system.actorOf(Props(classOf[MemtableFlusherBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_MEMTABLE)))
  val blockedCompatcActor = system.actorOf(Props(classOf[CompactionExecBlockedSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_COMPACTION)))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should "be sent on COUNTER_MUTATION blocked message" in  {
    executeTest(blockedCounterActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_COUNTER_MUTATION)
  }

  "A notification " should "be sent on MUTATION blocked message" in  {
    executeTest(blockedMutationActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_MUTATION)
  }

  "A notification " should "be sent on READ blocked message" in  {
    executeTest(blockedReadActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ)
  }

  "A notification " should "be sent on READ_REPAIR blocked message" in  {
    executeTest(blockedReadRepairActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ_REPAIR)
  }

  "A notification " should "be sent on COMPACTION_EXEC blocked message" in  {
    executeTest(blockedCompatcActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_COMPACTION_EXEC)
  }

  "A notification " should "be sent on GOSSIP blocked message" in  {
    executeTest(blockedGossipActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_GOSSIP)
  }

  "A notification " should "be sent on INTERNAL_RESPONSE blocked message" in  {
    executeTest(blockedInternalActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_INTERNAL_RESPONSE)
  }

  "A notification " should "be sent on MEMTABLE_FLUSHER blocked message" in  {
    executeTest(blockedMemtableActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_MEMTABLE_FLUSHER)
  }

  private def executeTest(testableActorRef : ActorRef, stype: String, dmType: String): Unit = {
    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(stype, dmType))
    metricsProviderProbe.reply(MetricsResponse(stype, Some(ThreadPoolStats(dmType, 0, 0, 0, 0, 0, 0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(stype, dmType))
    // non zero one minute rate should trigger a notification
    metricsProviderProbe.reply(MetricsResponse(stype, Some(ThreadPoolStats(dmType, 1, 0, 2, 0, 0, 0))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains(dmType))
  }
}
