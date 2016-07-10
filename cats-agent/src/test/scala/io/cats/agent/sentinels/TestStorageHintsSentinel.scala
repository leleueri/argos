package io.cats.agent.sentinels

import akka.actor.{ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.cats.agent.Constants._
import io.cats.agent.bean.ActorProtocol._
import io.cats.agent.bean._
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestStorageHintsSentinel extends TestKit(ActorSystem("TestStorageHintsSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val testableActor = system.actorOf(Props(classOf[StorageHintsSentinel], metricsProviderProbe.ref, globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS)))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should "be sent on Storage Hints" in {
    testableActor ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_HINTS, ""))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_STORAGE_HINTS, Some(new java.lang.Long(11100), new java.lang.Long(211))))

    notificationProbe.expectNoMsg()

    testableActor ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_HINTS, ""))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_STORAGE_HINTS, Some(new java.lang.Long(0), new java.lang.Long(0))))

    notificationProbe.expectNoMsg()

    testableActor ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_HINTS, ""))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_STORAGE_HINTS, Some(new java.lang.Long(11100), new java.lang.Long(211))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains("11100"))
    assert(notif.message.contains("211"))
  }
}
