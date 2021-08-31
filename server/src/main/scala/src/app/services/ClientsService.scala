package src.app.services

import src.app.{ClientStatus, CpuRawData}

import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.concurrent.Future
import scala.jdk.CollectionConverters._

trait ClientsService {
  def saveStatus(status: ClientStatus): Future[Int]
  def aggregate(data: CpuRawData): Future[UUID]
}

class ClientsServiceImpl(/*Here must be DAO*/) extends ClientsService {
  // must be placed in DAO
  val alarmMap: collection.concurrent.Map[String, Instant] =
    new ConcurrentHashMap[String, Instant]().asScala

  override def saveStatus(status: ClientStatus): Future[Int] =
    Future.successful(1)

  override def aggregate(data: CpuRawData): Future[UUID] =
    Future.successful(data.cid)
}
