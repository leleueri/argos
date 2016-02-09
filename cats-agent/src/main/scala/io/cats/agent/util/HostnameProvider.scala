package io.cats.agent.util

import scala.io.Source

/**
 * Created by eric on 30/01/16.
 */
object HostnameProvider {
  val hostname = {
    val stream = Runtime.getRuntime.exec("hostname").getInputStream()
    val source = Source.fromInputStream(stream)
    val result = source.getLines().next()
    source.close()
    result
  }
}
