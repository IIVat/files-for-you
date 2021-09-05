package app.dao

import app.CpuData

import scala.concurrent.Future

trait CpuLogDao {
  def insert(data: CpuData): Future[Int]
}

class CpuDataDaoImpl extends CpuLogDao {
  override def insert(data: CpuData): Future[Int] = {
    println(s"Logged to db id = ${data.cid}")
    Future.successful(1)
  }
}
