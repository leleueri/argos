package io.cats.agent

import akka.actor.{Props, Actor}
import com.typesafe.config.ConfigFactory
import io.cats.agent.bean.Notification
import java.util.{Date, Properties}
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}
import Constants._

import scala.collection.JavaConverters._

class MailNotifier extends Actor {

  val configMail = ConfigFactory.load().getConfig(CONF_OBJECT_ENTRY_MAIL_NOTIFIER)

  var to = configMail.getStringList(CONF_MAIL_NOTIFIER_RECIPIENTS).asScala
  var from = configMail.getString(CONF_MAIL_NOTIFIER_FROM)
  var host = configMail.getString(CONF_MAIL_NOTIFIER_SMTP)

  val props = new Properties();
  props.put("mail.smtp.host", host);
  val session = Session.getInstance(props, null);

  override def receive = {
    case Notification(title, msg) => sendMessage(title, msg)
  }

  def sendMessage(title: String, msg: String) : Unit = {
    try {
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
      case mex : MessagingException =>  mex.printStackTrace();// TODO use logger
      case ex =>  ex.printStackTrace();// TODO use logger
    }
  }
}


object MailNotifier {
  def props(): Props = Props[MailNotifier]
}