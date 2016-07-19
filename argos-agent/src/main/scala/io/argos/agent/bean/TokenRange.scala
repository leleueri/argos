package io.argos.agent.bean

/**
 * Created by eric on 06/06/16.
 */
case class ArgosTokenRange(start: Long, end: Long, endpoints: List[String], rpc_endpoints: List[String], endpointDetails: List[ArgosEndpointDetails])
case class ArgosEndpointDetails(host: String, dc: String, rack: String)