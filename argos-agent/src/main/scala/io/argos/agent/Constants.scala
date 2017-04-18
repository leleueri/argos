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
  val CONF_ORCHESTRATOR_JMX_USER = "jmx-user"
  val CONF_ORCHESTRATOR_JMX_PWD = "jmx-pwd"
  val CONF_ORCHESTRATOR_DOWN_LABEL = "node-down-label"
  val CONF_ORCHESTRATOR_DOWN_LEVEL = "node-down-level"
  val CONF_ORCHESTRATOR_UP_LABEL = "node-up-label"
  val CONF_ORCHESTRATOR_UP_LEVEL = "node-up-level"

  val CONF_OBJECT_ENTRY_SENTINEL = "argos.sentinel"
  val CONF_OBJECT_ENTRY_SENTINEL_ENABLE = CONF_OBJECT_ENTRY_SENTINEL+".enable"
  val CONF_OBJECT_ENTRY_SENTINEL_LOADAVG = CONF_OBJECT_ENTRY_SENTINEL+".load-avg"
  val CONF_OBJECT_ENTRY_SENTINEL_AVAILABLE = CONF_OBJECT_ENTRY_SENTINEL+".consistency-level"
  val CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BLOCKING = CONF_OBJECT_ENTRY_SENTINEL+".consistency-repaired-blocking"
  val CONF_OBJECT_ENTRY_SENTINEL_CONSISTENCY_RR_BACKGROUND = CONF_OBJECT_ENTRY_SENTINEL+".consistency-repaired-background"
  val CONF_OBJECT_ENTRY_SENTINEL_CONNECTION_TIMEOUTS = CONF_OBJECT_ENTRY_SENTINEL+".connection-timeouts"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_COUNTER = CONF_OBJECT_ENTRY_SENTINEL+".dropped-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_MUTATION = CONF_OBJECT_ENTRY_SENTINEL+".dropped-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ = CONF_OBJECT_ENTRY_SENTINEL+".dropped-read"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_READ_REPAIR = CONF_OBJECT_ENTRY_SENTINEL+".dropped-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_RANGE = CONF_OBJECT_ENTRY_SENTINEL+".dropped-range-slice"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_REQ_RESP = CONF_OBJECT_ENTRY_SENTINEL+".dropped-request-response"
  val CONF_OBJECT_ENTRY_SENTINEL_DROPPED_PAGE = CONF_OBJECT_ENTRY_SENTINEL+".dropped-page-range"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_SPACE = CONF_OBJECT_ENTRY_SENTINEL+".storage-space"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_EXCEPTION = CONF_OBJECT_ENTRY_SENTINEL+".storage-exception"
  val CONF_OBJECT_ENTRY_SENTINEL_GC = CONF_OBJECT_ENTRY_SENTINEL+".gc-inspector"
  val CONF_OBJECT_ENTRY_SENTINEL_STORAGE_HINTS = CONF_OBJECT_ENTRY_SENTINEL+".storage-hints"

  val CONF_OBJECT_ENTRY_SENTINEL_CUSTOM = CONF_OBJECT_ENTRY_SENTINEL+".custom-sentinels"

  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_COUNTER_MUTATION = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_GOSSIP = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-gossip"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_INTERNAL = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-internal"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_MEMTABLE = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-memtable"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_MUTATION = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_COMPACTION = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-compaction"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_READ = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-read"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_READ_REPAIR = CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_BLOCKED_STAGE_REQUEST_RESPONSE= CONF_OBJECT_ENTRY_SENTINEL+".blocked-stage-request-response"

  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_COUNTER_MUTATION = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-counter"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_GOSSIP = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-gossip"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_INTERNAL = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-internal"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_MEMTABLE = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-memtable"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_MUTATION = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-mutation"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_COMPACTION = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-compaction"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_READ = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-read"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_READ_REPAIR = CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-read-repair"
  val CONF_OBJECT_ENTRY_SENTINEL_PENDING_STAGE_REQUEST_RESPONSE= CONF_OBJECT_ENTRY_SENTINEL+".pending-stage-request-response"
  
  val CONF_OBJECT_ENTRY_SENTINEL_JMX_NOTIFICATION= CONF_OBJECT_ENTRY_SENTINEL+".notification-jmx"


  val CONF_THRESHOLD = "threshold"
  val CONF_ENABLED = "enabled"
  val CONF_LEVEL = "level"
  val CONF_LABEL = "label"
  val CONF_FREQUENCY = "period"
  val CONF_WINDOW_SIZE = "window-size"
  val CONF_WINDOW_MEAN = "check-mean"
  val CONF_COMMIT_LOG_THRESHOLD = "commitlog-threshold"

  val CONF_JMX_NAME = "objectName"
  val CONF_JMX_ATTR = "objectAttr"
  val CONF_EPSILON = "precision"
  val CONF_CUSTOM_MSG = "message"

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

  val CONF_ORCHESTRATOR = "argos.orchestrator"
  val CONF_ORCHESTRATOR_ENABLE = CONF_ORCHESTRATOR + ".enable"
  val CONF_ORCHESTRATOR_HOST = CONF_ORCHESTRATOR + ".http-hostname"
  val CONF_ORCHESTRATOR_PORT = CONF_ORCHESTRATOR + ".http-port"
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