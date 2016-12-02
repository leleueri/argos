package io.argos.agent.sentinels

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.argos.agent.{Constants, Messages, SentinelConfiguration}
import io.argos.agent.bean._
import Constants._
import ActorProtocol._
import io.argos.agent.bean._
import org.scalatest._

/**
 * Created by eric on 05/07/16.
 */
class TestStorageExceptionSentinel extends TestKit(ActorSystem("TestStorageExceptionSentinel")) with FlatSpecLike with Matchers with BeforeAndAfterAll {

  val globalConfig = ConfigFactory.load()
  val configJmx = globalConfig.getConfig(CONF_OBJECT_ENTRY_METRICS)

  val metricsProviderProbe = TestProbe()

  val notificationProbe = TestProbe()

  system.eventStream.subscribe(
    notificationProbe.ref,
    classOf[Notification])

  val storageExceptionActor = system.actorOf(Props(classOf[StorageExceptionSentinel], metricsProviderProbe.ref, SentinelConfiguration("test", globalConfig.getConfig(CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION))))

  override def afterAll() {
    system.terminate()
  }

  "A notification " should "be sent on Storage Exception" in {
    storageExceptionActor ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_EXCEPTION, ""))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_STORAGE_EXCEPTION, Some(new java.lang.Long(0))))

    notificationProbe.expectNoMsg()

    storageExceptionActor ! CheckMetrics()

    metricsProviderProbe.expectMsg(MetricsRequest(ActorProtocol.ACTION_CHECK_STORAGE_EXCEPTION, ""))
    metricsProviderProbe.reply(MetricsResponse(ACTION_CHECK_STORAGE_EXCEPTION, Some(new java.lang.Long(111))))

    val notif = notificationProbe.expectMsgAnyClassOf(classOf[Notification])
    assert(notif.message.contains("111"))
  }
}
