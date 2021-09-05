package app.services

import app.ClientStatus

import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait ClientsStatusService {
  def saveStatus(status: ClientStatus): Future[Int]
}

class ClientsStatusServiceImpl(/*Here must be DAO*/) extends ClientsStatusService {
  // must be placed in DAO
  val alarmMap: collection.concurrent.Map[String, Instant] =
    new ConcurrentHashMap[String, Instant]().asScala

  override def saveStatus(status: ClientStatus): Future[Int] =
    Future.successful(1)
}
