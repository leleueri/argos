package io.argos.agent

/**
 * Created by eric on 26/01/16.
 */
object Constants {

  val ACTOR_SYSTEM = "Argos"


  val CONF_ORCHESTRATOR_INTERVAL = "argos.scheduler-interval"

  val CONF_CASSANDRA_VERSION = "argos.cassandra-version"

  val CONF_OBJECT_ENTRY_METRICS = "argos.metrics"
  val CONF_ORCHESTRATOR_JMX_HOST = "jmx-host"
  val CONF_ORCHESTRATOR_JMX_PORT = "jmx-port"

  val CONF_OBJECT_ENTRY_SENTINEL = "argos.sentinel"
  val CONF_OBJECT_ENTRY_SENTINEL_LOADAVG = "argos.sentinel.load-avg"
  val CONF_OBJECT_ENTRY_SENTINEL_AVAILABLE = "argos.sentinel.consistency-level"
  val CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BLOCKING = "argos.sentinel.consistency-repaired-blocking"
  val CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BACKGROUND = "argos.sentinel.consistency-repaired-background"
  val CONF_OBJECT_ENTRY_SENTINEL_CONNECTION_TIMEOUTS = "argos.sentinel.connection-timeouts"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER = "argos.sentinel.dropped-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION = "argos.sentinel.dropped-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ = "argos.sentinel.dropped-read"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR = "argos.sentinel.dropped-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE = "argos.sentinel.dropped-range-slice"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP = "argos.sentinel.dropped-request-response"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE = "argos.sentinel.dropped-page-range"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE = "argos.sentinel.storage-space"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION = "argos.sentinel.storage-exception"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS = "argos.sentinel.storage-hints"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_COUNTER_MUTATION = "argos.sentinel.stage-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_GOSSIP = "argos.sentinel.stage-gossip"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_INTERNAL = "argos.sentinel.stage-internal"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_MEMTABLE = "argos.sentinel.stage-memtable"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_MUTATION = "argos.sentinel.stage-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_COMPACTION = "argos.sentinel.stage-compaction"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ = "argos.sentinel.stage-read"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_READ_REPAIR = "argos.sentinel.stage-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_STAGE_REQUEST_RESPONSE= "argos.sentinel.stage-request-response"
  val CONF_OBJECT_ENTRY_SENTINEL_JMX_NOTIFICATION= "argos.sentinel.notification-jmx"


  val CONF_THRESHOLD = "threshold"
  val CONF_ENABLED = "enabled"
  val CONF_LEVEL = "level"
  val CONF_LABEL = "label"
  val CONF_FREQUENCY = "period"
  val CONF_COMMIT_LOG_THRESHOLD = "commitlog-threshold"

  val CONF_KEYSPACES = "keyspaces"
  val CONF_KEYSPACE_NAME = "name"
  val CONF_CONSISTENCY_LEVEL = "cl"

  val CONF_OBJECT_ENTRY_NOTIFIERS = "argos.notifiers"
  val CONF_OBJECT_ENTRY_MAIL_NOTIFIER = "argos.notifiers.mail"
  val CONF_PROVIDER_CLASS_KEY = "providerClass"

  val CONF_MAIL_NOTIFIER_SMTP = "smtp-host"
  val CONF_MAIL_NOTIFIER_SMTP_PORT = "smtp-port"
  val CONF_MAIL_NOTIFIER_RECIPIENTS = "recipients"
  val CONF_MAIL_NOTIFIER_FROM = "from"
}

object Messages {
  val CHECK_METRICS = "checkStats"

  val OFFLINE_NODE = "OFFLINE"
  val ONLINE_NODE = "ONLINE"

  val READ_REPAIR_BACKGROUND = "RepairedBackground"
  val READ_REPAIR_BLOCKING = "RepairedBlocking"

  val CNX_TIMEOUT = "ConnectionTimeouts"

  val DROPPED_MESSAGE_COUNTER_MUTATION = "COUNTER_MUTATION"
  val DROPPED_MESSAGE_MUTATION = "MUTATION"
  val DROPPED_MESSAGE_PAGED_RANGE = "PAGED_RANGE"
  val DROPPED_MESSAGE_RANGE_SLICE = "RANGE_SLICE"
  val DROPPED_MESSAGE_READ_REPAIR = "READ_REPAIR"
  val DROPPED_MESSAGE_READ = "READ"
  val DROPPED_MESSAGE_REQUEST_RESPONSE = "REQUEST_RESPONSE"

  val STAGE_COUNTER_MUTATION = "CounterMutationStage"
  val STAGE_MUTATION = "MutationStage"
  val STAGE_READ_REPAIR = "ReadRepairStage"
  val STAGE_READ = "ReadStage"
  val STAGE_REQUEST_RESPONSE = "RequestResponseStage"

  val INTERNAL_STAGE_MEMTABLE_FLUSHER = "MemtableFlushWriter"
  val INTERNAL_STAGE_COMPACTION_EXEC = "CompactionExecutor"
  val INTERNAL_STAGE_GOSSIP = "GossipStage"
  val INTERNAL_STAGE_INTERNAL_RESPONSE = "InternalResponseStage"
}