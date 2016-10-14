package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants._
import io.argos.agent.bean._
import io.argos.agent.{Constants, Messages}
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestPendingSentinel extends TestKit(ActorSystem("TestPendingSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val pendingCounterActor = system.actorOf(Props(classOf[CounterMutationPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_COUNTER_MUTATION)))
  val pendingGossipActor = system.actorOf(Props(classOf[GossipPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_GOSSIP)))
  val pendingMutationActor = system.actorOf(Props(classOf[MutationPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_MUTATION)))
  val pendingReadActor = system.actorOf(Props(classOf[ReadPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_READ)))
  val pendingReadRepairActor = system.actorOf(Props(classOf[ReadRepairPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_READ_REPAIR)))
  val pendingInternalActor = system.actorOf(Props(classOf[InternalResponsePendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_INTERNAL)))
  val pendingMemtableActor = system.actorOf(Props(classOf[MemtableFlusherPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_MEMTABLE)))
  val pendingCompatcActor = system.actorOf(Props(classOf[CompactionExecPendingSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_COMPACTION)))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should "be sent on COUNTER_MUTATION pending message" in  {
    executeTest(pendingCounterActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_COUNTER_MUTATION)
  }

  "A notification " should "be sent on MUTATION pending message" in  {
    executeTest(pendingMutationActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_MUTATION)
  }

  "A notification " should "be sent on READ pending message" in  {
    executeTest(pendingReadActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ)
  }

  "A notification " should "be sent on READ_REPAIR pending message" in  {
    executeTest(pendingReadRepairActor, ActorProtocol.ACTION_CHECK_STAGE, Messages.STAGE_READ_REPAIR)
  }

  "A notification " should "be sent on COMPACTION_EXEC pending message" in  {
    executeTest(pendingCompatcActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_COMPACTION_EXEC)
  }

  "A notification " should "be sent on GOSSIP pending message" in  {
    executeTest(pendingGossipActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_GOSSIP)
  }

  "A notification " should "be sent on INTERNAL_RESPONSE pending message" in  {
    executeTest(pendingInternalActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_INTERNAL_RESPONSE)
  }

  "A notification " should "be sent on MEMTABLE_FLUSHER pending message" in  {
    executeTest(pendingMemtableActor, ActorProtocol.ACTION_CHECK_INTERNAL_STAGE, Messages.INTERNAL_STAGE_MEMTABLE_FLUSHER)
  }

  private def executeTest(testableActorRef : ActorRef, stype: String, dmType: String): Unit = {
    // under the threshold, no notification
    (1 to 5).foreach { i =>

      testableActorRef ! CheckMetrics()

      metricsProviderProbe.expectMsg(MetricsRequest(stype, dmType))
      metricsProviderProbe.reply(MetricsResponse(stype, Some(ThreadPoolStats(dmType, 0, 0, 0, 0, 1, 0))))

      notificationProbe.expectNoMsg()
    }

    // notification send only if all ThreadPool element exceed the limit
    (1 to 4).foreach { i =>

      testableActorRef ! CheckMetrics()

      metricsProviderProbe.expectMsg(MetricsRequest(stype, dmType))
      metricsProviderProbe.reply(MetricsResponse(stype, Some(ThreadPoolStats(dmType, 0, 0, 0, 0, 25, 0))))

      notificationProbe.expectNoMsg()
    }


    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(stype, dmType))
    metricsProviderProbe.reply(MetricsResponse(stype, Some(ThreadPoolStats(dmType, 0, 0, 0, 0, 25, 0))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains(dmType))
  }
}
