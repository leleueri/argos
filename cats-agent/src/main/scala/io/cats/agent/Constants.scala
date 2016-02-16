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
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER = "cats.sentinel.dropped-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION = "cats.sentinel.dropped-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ = "cats.sentinel.dropped-read"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR = "cats.sentinel.dropped-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE = "cats.sentinel.dropped-range-slice"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP = "cats.sentinel.dropped-request-response"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE = "cats.sentinel.dropped-page-range"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE = "cats.sentinel.storage-space"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION = "cats.sentinel.storage-exception"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS = "cats.sentinel.storage-hints"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_COUNTER_MUTATION = "cats.sentinel.stage-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_GOSSIP = "cats.sentinel.stage-gossip"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_INTERNAL = "cats.sentinel.stage-internal"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_MEMTABLE = "cats.sentinel.stage-memtable"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_MUTATION = "cats.sentinel.stage-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ = "cats.sentinel.stage-read"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ_REPAIR = "cats.sentinel.stage-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_REQUEST_RESPONSE= "cats.sentinel.stage-request-response"

  val CONF_THRESHOLD = "threshold"
  val CONF_ENABLED = "enabled"
  val CONF_LEVEL = "level"
  val CONF_LABEL = "label"
  val CONF_FREQUENCY = "period"
  val CONF_COMMIT_LOG_THRESHOLD = "commitlog-threshold"

  val CONF_OBJECT_ENTRY_MAIL_NOTIFIER = "cats.notifiers.mail"
  val CONF_MAIL_NOTIFIER_SMTP = "smtp-host"
  val CONF_MAIL_NOTIFIER_RECIPIENTS = "recipients"
  val CONF_MAIL_NOTIFIER_FROM = "from"
}

object Messages {
  val CHECK_METRICS = "checkStats"
}