package src.app.services

import src.app.CpuRawData

import scala.concurrent.Future

trait CpuLogService {
  def insert(data: CpuRawData): Future[Int]
}

class CpuLogServiceImpl() extends CpuLogService {
  override def insert(data: CpuRawData): Future[Int] = ???
}
