package app.dao

import app.CpuData

import java.util.UUID
import scala.concurrent.Future

trait AggregationStorage {
  def save(cid: UUID, window: Long, data: Seq[CpuData]): Future[Int]
}

class AggregationStorageImpl() extends AggregationStorage{
  def save(cid: UUID, window: Long, data: Seq[CpuData]): Future[Int] = {
    println(s"Saved to storage id = $cid, window = $window")
    Future.successful(1)
  }
}
