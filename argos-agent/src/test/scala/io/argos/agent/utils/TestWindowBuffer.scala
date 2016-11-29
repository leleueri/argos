package io.argos.agent.utils

import io.argos.agent.bean.ThreadPoolStats
import io.argos.agent.util.WindowBuffer
import org.scalatest.{FlatSpecLike, Matchers}

/**
  * Created by eric on 29/11/16.
  */
class TestWindowBuffer extends FlatSpecLike with Matchers{

  "WindowBuffer underThreshold " should " returns TRUE if all element of the BUFFER are under the Threshold or if the BUFFER ins't full" in {
    val buffer = new WindowBuffer[Int](2, 0.0)

    def extractor(input: Int) : Double = input.toDouble

    // Not full
    buffer.push(2)
    buffer.underThreshold(3, extractor) shouldBe true
    // full but some elements are under the threshold
    buffer.push(3)
    buffer.underThreshold(3, extractor) shouldBe true
    // full but all elements are over the threshold
    buffer.push(3)
    buffer.underThreshold(3, extractor) shouldBe false
    // full but some elements are under the threshold
    buffer.push(2)
    buffer.underThreshold(3, extractor) shouldBe true

  }

  "WindowBuffer underThreshold " should " returns TRUE if the mean value of the BUFFER are under the Threshold or if the BUFFER ins't full" in {
    val buffer = new WindowBuffer[ThreadPoolStats](2, 0.0)

    def extractor(input: ThreadPoolStats) : Double = input.pendingTasks.toDouble

    // Not full
    buffer.push(ThreadPoolStats("", 0, 0, 0, 0, 2, 0))
    buffer.meanUnderThreshold(3, extractor) shouldBe true
    // full but mean value is under the threshold
    buffer.push(ThreadPoolStats("", 0, 0, 0, 0, 3, 0))
    buffer.meanUnderThreshold(3, extractor) shouldBe true

    buffer.clear()

    // Not full
    buffer.push(ThreadPoolStats("", 0, 0, 0, 0, 8, 0))
    buffer.meanUnderThreshold(3, extractor) shouldBe true
    // full but mean value is over the threshold
    buffer.push(ThreadPoolStats("", 0, 0, 0, 0, 1, 0))
    buffer.meanUnderThreshold(3, extractor) shouldBe false

  }
}
