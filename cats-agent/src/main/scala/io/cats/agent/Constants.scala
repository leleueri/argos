package io.cats.agent

/**
 * Created by eric on 26/01/16.
 */
object Constants {

  val ACTOR_SYSTEM = "Cats"

}

object Messages {
  val CHECK_CRITICAL_METRICS = "checkCritical"
  val CHECK_WARNING_METRICS = "checkWarning"
  val CHECK_INFORMATIVE_METRICS = "checkInfo"

  val NOTIFICATION_LEVEL_CRITICAL = "CRITICAL"
  val NOTIFICATION_LEVEL_WARN = "WARNING"
  val NOTIFICATION_LEVEL_INFO = "INFO"
}