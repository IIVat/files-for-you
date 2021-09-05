package app.services

import app.CpuReport

import java.util.UUID
import scala.concurrent.Future

trait CpuReportService {
  def insert(report: CpuReport): Future[Int]
  def getReport(uid: UUID): Future[Option[CpuReport]]
  def getAllFailed: Future[Vector[CpuReport]]
}

class CpuReportServiceImpl() extends CpuReportService {
  override def insert(report: CpuReport): Future[Int] = {
    Future.successful(1)
  }

  override def getReport(cid: UUID): Future[Option[CpuReport]] = {
    Future.successful(Some(CpuReport(cid, "aggregation")))
  }

  override def getAllFailed: Future[Vector[CpuReport]] = {
    Future.successful(
      Vector(
        CpuReport(UUID.randomUUID(), "aggregation"),
        CpuReport(UUID.randomUUID(), "aggregation"),
      )
    )
  }
}