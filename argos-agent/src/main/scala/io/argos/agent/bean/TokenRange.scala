package io.argos.agent.bean

/**
 * Created by eric on 06/06/16.
 */
case class CatsTokenRange(start: Long, end: Long, endpoints: List[String], rpc_endpoints: List[String], endpointDetails: List[CatsEndpointDetails])
case class CatsEndpointDetails(host: String, dc: String, rack: String)