package io.argos.agent.notifiers

import akka.actor.{Actor, ActorLogging, Props}
import io.argos.agent.bean.Notification

/**
 * Created by eric on 29/05/16.
 */
class ConsoleNotifier extends Notifier {
  log.info("Console notifier is running...")

  /**
   * The notifier identifier used a key in the 'notifiers' section of the configuration.
   * The value provided by this method is used to locate the entry of the provider configuration
   * into the "cats.notifiers" section.
   *
   * @return
   */
  override def notifierId: String = "console"

  override def onNotification(notif: Notification): Unit = println(notif.title, notif.message)

}

class ConsoleNotifierProvider extends NotifierProvider {
  def actorName() : String = "ConsoleNotifier"
  def props(): Props = Props[ConsoleNotifier]
}