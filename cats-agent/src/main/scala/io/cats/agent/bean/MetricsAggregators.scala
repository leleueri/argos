package io.cats.agent.bean

/**
 * This class groups all independent values relative to a type of ThreadPool (MutationStage, ReadRepairStage...)
 */
case class ThreadPoolStats(`type`: String, activeTasks: Int, completedTasks: Int, currentBlockedTasks: Long, maxPoolSize: Int, pendingTasks: Int, totalBlockedTasks: Long)

/**
 * This class contains all rates of dropped messages for a given type of request (rate unit = events/second)
 */
case class DroppedMessageStats(`type`: String, count: Long, fifteenMinRate: Double, fiveMinRate: Double, meanRate: Double, oneMinRate: Double)