import java.time.Instant
import java.util.UUID
import scala.concurrent.duration.FiniteDuration

package object app {
  case class CpuData(cid: UUID, cpuValue: String, timestamp: Instant)
  case class CpuReport(cid: UUID, aggregation: String)
  case class CpuAlert(cid: UUID, reportLink: String)
  case class ClientStatus(cid: UUID, isTurnedOn: Boolean, timestamp: Instant)
  case class Config(frequencies: Seq[FiniteDuration])
}
