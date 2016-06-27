package io.cats.agent.util

import akka.actor.ActorSystem
import akka.event.{Logging, LoggingAdapter}
import com.typesafe.config.ConfigFactory


class CommonLogger(logger: LoggingAdapter){

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


  def error(caller: AnyRef, message: String, args:String*) : Unit = {
    logger.error("["+ caller.getClass.getName + "]"+ message, args.toArray)
  }
  def error(caller: AnyRef, exception: Throwable, message: String, args:String*) : Unit = {
    logger.error(exception, "["+ caller.getClass.getName + "]"+ message, args.toArray)
  }
}

object CommonLoggerFactory {

  val system = ActorSystem("logger-system", ConfigFactory.load.getConfig("logger-system"))
  val commonLogger = new CommonLogger(Logging.getLogger(system, this))

}