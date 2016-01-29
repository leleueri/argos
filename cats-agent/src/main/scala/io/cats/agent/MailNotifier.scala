package io.cats.agent

import akka.actor.Actor
import io.cats.agent.bean.Notification
import java.util.{Date, Properties}
import javax.mail._
import javax.mail.internet.{InternetAddress, MimeMessage}

class MailNotifier extends Actor {

  // TODO configure all these stuffs
  // Recipient's email ID needs to be mentioned.
  var to = "eric.leleu@domain.com";
  // Sender's email ID needs to be mentioned
  var from = "cats-agent@no-reply";
  // Assuming you are sending email from localhost
  var host = "mailhost.domain";

  val props = new Properties();
  props.put("mail.smtp.host", host);
  val session = Session.getInstance(props, null);

  override def receive = {
    case Notification(level, msg) => sendMessage(level, msg)
  }

  def sendMessage(level: String, msg: String) : Unit = {
    try {
      val message = new MimeMessage(session);
      message.setFrom(new InternetAddress(from));
      message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
      message.setSentDate(new Date());
      message.setSubject(s"[${level}] Cassandra Agent");
      message.setText(msg);

      Transport.send(message);
    } catch {
      case mex : MessagingException =>  mex.printStackTrace();
    }
  }
}
