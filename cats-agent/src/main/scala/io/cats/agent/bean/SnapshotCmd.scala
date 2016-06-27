package io.cats.agent.bean

/**
 * Created by eric on 20/06/16.
 */
case class SnapshotCmd(keyspace: String, table: Option[String] = None)
