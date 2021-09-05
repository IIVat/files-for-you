package app

import akka.actor.ActorSystem
import akka.stream.Materializer
import akka.stream.scaladsl.{Merge, Source}
import sttp.client3.akkahttp.AkkaHttpBackend

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

object ClientApp extends App {
  implicit val system = ActorSystem("client-app")
  implicit val mat = Materializer(system)
  implicit val backend = AkkaHttpBackend.usingActorSystem(system)

  val client = CpuMonitoringClientLive("ws://localhost:9999")

  val clientsStatuses = Array.tabulate(4)(_ => ClientStatus(UUID.randomUUID(), isTurnedOn = true, Instant.now))

  val status = Source
    .tick(1.second, 1.seconds,())
    .map(_ => clientsStatuses(Random.nextInt(4)))
    .mapAsync(4)(client.sendStatus)

  val cpuData =
    Source
      .future(client.sendCpuData(clientsStatuses.map(_.cid)))

  Source.combine(status, cpuData)(Merge(_)).runForeach(_ => ())
    .onComplete {
    _ =>
      system.terminate()
      backend.close()
  }

}
