package io.cats.agent

import akka.actor.{ActorLogging, Props, Actor}
import com.typesafe.config.ConfigFactory
import io.cats.agent.bean.Notification
import java.util.{Date, Properties}
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import Constants._

import scala.collection.JavaConverters._

class MailNotifier extends Actor with ActorLogging {

  val configMail = ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_MAIL_NOTIFIER)

  val to = configMail.getStringList(CONF_MAIL_NOTIFIER_RECIPIENTS).asScala
  val from = configMail.getString(CONF_MAIL_NOTIFIER_FROM)
  val host = configMail.getString(CONF_MAIL_NOTIFIER_SMTP)
  val port = Option(configMail.getString(CONF_MAIL_NOTIFIER_SMTP_PORT)).getOrElse("25")

  val props = new Properties();
  props.put("mail.smtp.host", host);
  props.put("mail.smtp.port", port);
  val session = Session.getInstance(props, null);

  log.info("MailNotifier is running...")

  override def receive = {
    case Notification(title, msg) => sendMessage(title, msg)
  }

  def sendMessage(title: String, msg: String) : Unit = {
    try {

      log.debug("Send Message with title : {}", title)

      val message = new MimeMessage(session);
      message.setFrom(new InternetAddress(from));
      to.foreach {
        x => message.addRecipient(Message.RecipientType.TO, new InternetAddress(x))
      }
      message.setSentDate(new Date());
      message.setSubject(title);
      message.setText(msg);

      Transport.send(message);
    } catch {
      case mex : Exception =>  log.warning("Unable to send the notification message : {}", mex.getMessage, mex)
    }
  }
}


object MailNotifier {
  def props(): Props = Props[MailNotifier]
}