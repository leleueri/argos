package io.cats.agent

/**
 * Created by eric on 26/01/16.
 */
object Constants {

  val ACTOR_SYSTEM = "Cats"

  val CONF_OBJECT_ENTRY_SENTINEL_ORCHESTRATOR = "cats.sentinel.manager"
  val CONF_ORCHESTRATOR_INTERVAL = "scheduler-interval"
  val CONF_ORCHESTRATOR_JMX_HOST = "jmx-host"
  val CONF_ORCHESTRATOR_JMX_PORT = "jmx-port"


  val CONF_OBJECT_ENTRY_SENTINEL = "cats.sentinel"
  val CONF_OBJECT_ENTRY_SENTINEL_LOADAVG = "cats.sentinel.load-avg"
  val CONF_OBJECT_ENTRY_SENTINEL_HINTS = "cats.sentinel.hints"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER = "cats.sentinel.dropped-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION = "cats.sentinel.dropped-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ = "cats.sentinel.dropped-read"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR = "cats.sentinel.dropped-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE = "cats.sentinel.dropped-range-slice"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP = "cats.sentinel.dropped-request-response"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE = "cats.sentinel.dropped-page-range"

  val CONF_THRESHOLD = "threshold"
  val CONF_ENABLED = "enabled"
  val CONF_LEVEL = "level"
  val CONF_LABEL = "label"

  val CONF_OBJECT_ENTRY_MAIL_NOTIFIER = "cats.notifiers.mail"
  val CONF_MAIL_NOTIFIER_SMTP = "smtp-host"
  val CONF_MAIL_NOTIFIER_RECIPIENTS = "recipients"
  val CONF_MAIL_NOTIFIER_FROM = "from"
}

object Messages {
  val CHECK_METRICS = "checkStats"
}