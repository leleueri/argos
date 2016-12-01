package io.argos.agent

import java.util.concurrent.TimeUnit

import com.typesafe.config.Config
import Constants._
import scala.collection.JavaConverters._

import collection.JavaConversions._
import scala.concurrent.duration.Duration

case class KeyspaceConsistency(val keyspace: String, val consistency: String)

class SentinelConfiguration(val threshold: Double, val enabled: Boolean, 
                            val level: String, val label: String, 
                            val period: Long, val windowSize: Int,
                            val checkMean: Boolean, val commitLogThreshold: Double,
                            val objectName: Option[String], val objectAttr: Option[String],
                            val precision: Double, val messageHeader: Option[String],
                            val keyspaces: List[KeyspaceConsistency]) {}

object SentinelConfiguration {
  def apply(cfgName: String, cfg: Config) : SentinelConfiguration = new SentinelConfiguration(
    if (cfg.hasPath(CONF_THRESHOLD)) cfg.getDouble(CONF_THRESHOLD) else 0.0,
    if (cfg.hasPath(CONF_ENABLED)) cfg.getBoolean(CONF_ENABLED) else false,
    if (cfg.hasPath(CONF_LEVEL)) cfg.getString(CONF_LEVEL)else "WARNING",
    if (cfg.hasPath(CONF_LABEL)) cfg.getString(CONF_LABEL) else cfgName,
    if (cfg.hasPath(CONF_FREQUENCY)) cfg.getDuration(CONF_FREQUENCY).toMillis else Duration.create(60, TimeUnit.SECONDS).toMillis,
    if (cfg.hasPath(CONF_WINDOW_SIZE)) cfg.getInt(CONF_WINDOW_SIZE) else 1,
    if (cfg.hasPath(CONF_WINDOW_MEAN)) cfg.getBoolean(CONF_WINDOW_MEAN) else false,
    if (cfg.hasPath(CONF_COMMIT_LOG_THRESHOLD)) cfg.getDouble(CONF_COMMIT_LOG_THRESHOLD) else 5.0,

    if (cfg.hasPath(CONF_JMX_NAME)) Some(cfg.getString(CONF_JMX_NAME)) else None,
    if (cfg.hasPath(CONF_JMX_NAME) && cfg.hasPath(CONF_JMX_ATTR)) Some(cfg.getString(CONF_JMX_ATTR)) else None,
    if (cfg.hasPath(CONF_EPSILON)) cfg.getDouble(CONF_EPSILON) else 0.001,
    if (cfg.hasPath(CONF_CUSTOM_MSG)) Some(cfg.getString(CONF_CUSTOM_MSG)) else None,
    if (cfg.hasPath(CONF_KEYSPACES)) {
      for {
        pair <- cfg.getConfigList(CONF_KEYSPACES).asScala.toList
        ks = pair.getString(CONF_KEYSPACE_NAME)
        cl = pair.getString(CONF_CONSISTENCY_LEVEL)
      } yield KeyspaceConsistency(ks, cl)
    } else List()
  )
}

object ConfigHelper {

  def extractCustomSentinelsNames (customConfig: Config) : List[String] = {
    customConfig.entrySet()
      .toList
      .map(entry => entry.getKey.split("\\.")(0)).distinct
  }
}