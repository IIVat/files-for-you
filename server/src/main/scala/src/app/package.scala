package src

import java.time.Instant
import java.util.UUID

package object app {
  case class CpuRawData(cid: UUID, cpuValue: String, timestamp: Instant)
  case class CpuReport(cid: UUID, aggregation: String)
  case class CpuAlert(cid: UUID, reportLink: String)
  case class ClientStatus(cid: UUID, isTurnedOn: Boolean, timestamp: Instant)
}
