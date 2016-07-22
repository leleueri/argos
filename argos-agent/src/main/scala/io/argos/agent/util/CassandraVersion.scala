package io.argos.agent.util

import com.typesafe.config.ConfigFactory
import io.argos.agent.Constants._

/**
  * Created by eric on 21/07/16.
  */
object CassandraVersion {
  val version = ConfigFactory.load().getDouble(CONF_CASSANDRA_VERSION)
}
