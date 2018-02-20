package io.argos.agent.bean

object SnapshotProtocol {
  val STATUS_OK = "OK"
  val STATUS_KO = "KO"
  val STATUS_ONGOING = "ONGOING"
}

/**
  * Represent a Snapshot request processed by the Actor connected to the Cassandra Node.
  * @param id: the Request ID
  * @param keyspace: the keyspace on which the snapshot must be done
  * @param table: the specific table to snapshot (if missing, snapshot all the keyspace)
  * @param flush: do we have to perform a flush before the snapshot
  */
case class SnapshotOrder(id: String, keyspace: String, table: Option[String] = None, flush: Boolean = true)
/**
  * Represent the status of a Snapshot request.
  * @param id: the Request ID
  * @param gatewayName the human readable name of the gateway agent
  * @param state: the request state OK, KO, ONGOING
  * @param details: the failure reason if available
  */
case class SnapshotStatus(id: String, gatewayName: String, state: String, details: Option[String] = None)