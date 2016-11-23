package io.argos.agent

import com.typesafe.config.Config

import collection.JavaConversions._


object ConfigHelper {

  def extractCustomSentinelsNames (customConfig: Config) : List[String] = {
    customConfig.entrySet()
      .toList
      .map(entry => entry.getKey.split("\\.")(0)).distinct
  }
}