package io.cats.agent.util

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import com.typesafe.config.ConfigFactory


abstract class CommonLogger(logger : LoggingAdapter)

class SentinelLogger(logger: LoggingAdapter) extends CommonLogger(logger) {

  def debug(caller: AnyRef, message: String, args:String*) : Unit = {
    logger.debug("["+ caller.getClass.getName + "]"+ message, args.toArray)
  }

  def isDebugEnabled = logger.isDebugEnabled

  def info(caller: AnyRef, message: String, args:String*) : Unit = {
    logger.info("["+ caller.getClass.getName + "]"+ message, args.toArray)
  }

  def isInfoEnabled = logger.isInfoEnabled

  def warn(caller: AnyRef, message: String, args:String*) : Unit = {
    logger.warning("["+ caller.getClass.getName + "]"+ message, args.toArray)
  }

  def isWarningEnabled = logger.isWarningEnabled
}

object CommonLoggerFactory {

  val system = ActorSystem("logger-system", ConfigFactory.load.getConfig("logger-system"))
  val sentinelLogger = new SentinelLogger(Logging.getLogger(system, this))

}