package io.cats.agent.sentinels

import com.typesafe.config.Config
import io.cats.agent.Constants

/**
 * The sentinel analyzes information provided by the JMX interface of the Cassandra Node.
 * If the received values go out configured thresholds, the sentinel will send notifications/alerts.
 * @tparam T
 */
trait Sentinel [T] {

  def conf : Config

  def isEnabled = conf.getBoolean(Constants.CONF_ENABLED)

  def analyzeAndReact() : Unit = analyze().map(react(_))

  protected def analyze() : Option[T]

  protected def react(info: T) : Unit

  protected def level() = conf.getString(Constants.CONF_LEVEL)

  protected def label() = conf.getString(Constants.CONF_LABEL)

  protected def title() = s"[${level}] [${label}] Cassandra Sentinel found something"
}
