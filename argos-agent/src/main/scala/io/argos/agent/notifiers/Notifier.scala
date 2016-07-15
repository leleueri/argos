package io.argos.agent.notifiers

import akka.actor.{ActorLogging, Actor, Props}
import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants
import io.argos.agent.bean.Notification
import Constants._

/**
 * Base class for each Notifier actor.
 *
 * Each Notifier must have a configuration section into the "cats.notifiers" block
 * and the minimal configuration is the key 'providerClass' that specifies the class
 * able to provide the Props object for the actor.
 */
abstract class Notifier extends Actor with ActorLogging {
  /**
   * The notifier identifier used a key in the 'notifiers' section of the configuration.
   * The value provided by this method is used to locate the entry of the provider configuration
   * into the "cats.notifiers" section.
   *
   * @return
   */
  def notifierId : String

  final def getNotifierConfig() = ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_NOTIFIERS + "." + notifierId)

  /**
   * This method returns an array of CaseClasses used as a Channel of the EventStream.
   * The Notifier will subscribe to these channels
   *
   * @return
   */
  def notificationChannels() : Array[Class[_]] = Array(classOf[Notification])

  // when the actor is created, it subscribes to the channels
  notificationChannels().foreach{ channel =>
    this.context.system.eventStream.subscribe(this.self, channel)
  }
}

/**
 * Each Notifier is linked to a NotifierProvider.
 * This provider gives the Props object required
 * to create and configure the Notifier Actor.
 */
trait NotifierProvider {
  /**
   * Name of the Actor for this notifier
   * @return
   */
  def actorName() : String
  /**
   * @return configuration object to specify options for the creation of the actor Notifier
   */
  def props(): Props
}