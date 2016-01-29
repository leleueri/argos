package io.cats.agent.bean

/**
 * This class groups all independent values relative to a type of ThreadPool (MutationStage, ReadRepairStage...)
 */
case class ThreadPoolStats(`type`: String, activeTasks: Int, completedTasks: Int, currentBlockedTasks: Long, maxPoolSize: Int, pendingTasks: Int, totalBlockedTasks: Long)

/**
 * This class contains all rates of dropped messages for a given type of request (rate unit = events/second)
 */
case class DroppedMessageStats(`type`: String, count: Long, fifteenMinRate: Double, fiveMinRate: Double, meanRate: Double, oneMinRate: Double)

case class StorageSpaceInfo(path: String, usedSpace: Long, availableSpace: Long, totalSpace: Long, commitLog: Boolean) {

  /**
    * Compute the expected available space using the percentage of the total space provide in argument.
    *
    * @param threshold percentage of the totalSpace that must be available
    * @return true if the available space is less than the threshold provided in argument
    */
  def exceedThreshold(threshold: Int) : Boolean = {
    val expectedAvailableSpace = (totalSpace * threshold / 100)
    expectedAvailableSpace > availableSpace
  }
}