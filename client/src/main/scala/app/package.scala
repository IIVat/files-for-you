import java.time.Instant
import java.util.UUID

package object app {
  case class CpuData(cid: UUID, cpuValue: String, timestamp: Instant)
  case class ClientStatus(cid: UUID, isTurnedOn: Boolean, timestamp: Instant)
}
