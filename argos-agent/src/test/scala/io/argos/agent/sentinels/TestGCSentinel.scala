package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants._
import io.argos.agent.Messages
import io.argos.agent.bean.ActorProtocol._
import io.argos.agent.bean._
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestGCSentinel extends TestKit(ActorSystem("TestGCSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val gcInspectorActor = system.actorOf(Props(classOf[GCSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_GC)))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should " be sent on Connection Timeout " in {
    executeTest(gcInspectorActor)
  }


  private def executeTest(testableActorRef : ActorRef): Unit = {
    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_GC, ""))
    metricsProviderProbe.reply(MetricsResponse(ActorProtocol.ACTION_CHECK_GC, Some(GCState(0,0,0,0,0,0,0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_GC, ""))
    metricsProviderProbe.reply(MetricsResponse(ActorProtocol.ACTION_CHECK_GC, Some(GCState(0,100,100,0,0,1,0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_GC, ""))
    metricsProviderProbe.reply(MetricsResponse(ActorProtocol.ACTION_CHECK_GC, Some(GCState(0,150,350,0,0,3,0))))

    notificationProbe.expectNoMsg()

    testableActorRef ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_GC, ""))
    // non zero one minute rate should trigger a notification
    metricsProviderProbe.reply(MetricsResponse(ActorProtocol.ACTION_CHECK_GC, Some(GCState(0,200,300,0,0,2,0))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains("GC issue"))
  }
}
