package org.apache.spark.instrument

import org.apache.spark.rdd.RDD

case class ServiceRow(trace: Int, uuid: String, start: Long, name: String, host: String, port: Int)

case class Trace(id: Int, processes: Set[Process]) {
  def this(trace: Int, subtree: Map[String, Seq[ServiceRow]]) = {
    this(trace, subtree.map(rows => new Process(null, rows._1, rows._2)).toSet) // FIXME Backward pointer
  }
}

case class Process(trace: Trace, uuid: String, services: Set[Service]) {
  def this(trace: Trace, uuid: String, subtree: Seq[ServiceRow]) = {
    this(trace, uuid, subtree.map(row => Service(null, row.host, row.port, row.name, row.start)).toSet) // FIXME Backward pointer
  }
}

case class Service(process: Process, host: String, port: Int, name: String, start: Long) {
  val id: String = s"$host:$port $name" // TODO Prepend trace ID to ensure uniqueness
}

class ServiceMap(events: Seq[RDD[EventTree]]) { // FIXME Filtering is another reason to keep this mutable inside
  private def serviceRows(traceid: Int, rdd: RDD[EventTree]) = {
    rdd.filter(_(3)(0).is("Service")).collect.map(x => {
      val host = x(3)(2).get.split(":")
      ServiceRow(traceid, x(1).get, x(2).get.toLong, x(3)(1).get, host(0).split("/")(1), host(1).toInt)
    }).toSeq
  }
  private val list = events.zipWithIndex.map(x => serviceRows(x._2, x._1)).reduce(_ ++ _)
  private val tree = list.groupBy(_.trace).mapValues(_.groupBy(_.uuid))
  val traces: Map[Int, Trace] = tree.map(subtree => subtree._1 -> new Trace(subtree._1, subtree._2))
  val processes: Map[String, Process] = traces.flatMap(trace => trace._2.processes.map(process => process.uuid -> process))
  val services: Map[(String, Int), Service] = processes.flatMap(process => process._2.services.map(service => (service.host, service.port) -> service))
}