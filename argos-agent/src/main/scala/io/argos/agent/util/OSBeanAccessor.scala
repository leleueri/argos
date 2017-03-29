package io.argos.agent.util

import java.lang.management.ManagementFactory

/**
  * Created by eric on 10/03/17.
  */
object OSBeanAccessor {
  private val osMBean = ManagementFactory.getOperatingSystemMXBean()

  def loadAvg = osMBean.getSystemLoadAverage
}
