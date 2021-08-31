package src.app.dao

import src.app.CpuRawData

import scala.concurrent.Future

trait CpuLogDao {
  def insert(data: CpuRawData): Future[Int]
}

class CpuDataDaoImpl extends CpuLogDao {
  override def insert(data: CpuRawData): Future[Int] = {
    Future.successful(1)
  }
}
