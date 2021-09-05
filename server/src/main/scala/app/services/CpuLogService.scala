package app.services

import app.CpuData
import app.dao.CpuLogDao

import scala.concurrent.Future

trait CpuLogService {
  def insert(data: CpuData): Future[Int]
}

class CpuLogServiceImpl(cpuLog: CpuLogDao) extends CpuLogService {
  override def insert(data: CpuData): Future[Int] = cpuLog.insert(data)
}
