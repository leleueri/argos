package io.argos.agent.util

import scala.collection.mutable

/**
  * Created by eric on 29/11/16.
  */
class WindowBuffer[T](size: Int, epsilon: Double = 0.0) {
  val buffer = mutable.Queue[T]()

  def push(elt: T): Unit = {
    if (buffer.size == size) buffer.dequeue()
    buffer.enqueue(elt)
  }

  /**
    * @param threshold
    * @return true if the mean pending tasks of the buffer are under the threshold
    */
  def meanUnderThreshold(threshold: Double, extractor : (T => Double)) : Boolean = {
    if (buffer.size == size) {
      val moy: Double = buffer.foldLeft(0.0)((cumul, elt) => cumul + extractor(elt)) / size
      areEquals(moy, threshold) || moy < threshold
    }
    else true
  }

  /**
    * @param threshold
    * @return true if all pending tasks of the buffer are under the threshold
    */
  def underThreshold(threshold: Double, extractor : (T => Double)) : Boolean = {
    if (buffer.size == size) buffer.exists( entry => areEquals(extractor(entry), threshold) || extractor(entry) < threshold)
    else true
  }

  private def areEquals(a: Double, b: Double) : Boolean = {
    if (a < b) Math.abs(a - b) < epsilon else Math.abs(b - a) < epsilon
  }

  def clear() = buffer.clear()
}
