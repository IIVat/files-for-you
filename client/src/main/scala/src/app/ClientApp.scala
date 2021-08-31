package src.app

import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, Materializer}
import akka.stream.scaladsl.Source
import sttp.client3.akkahttp.AkkaHttpBackend

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationInt
import scala.util.Random

object ClientApp extends App {
  implicit val system = ActorSystem("client-app")
  implicit val mat = Materializer(system)
  implicit val backend = AkkaHttpBackend.usingActorSystem(system)

  val client = CpuMonitoringClientLive("ws://localhost:9999")

  val userIds = Array.tabulate(4)(_ => UUID.randomUUID())

  Source
    .tick(1.second, 1.second,())
    .map(_ => userIds(Random.nextInt(4)))
    .mapAsync(4) {id =>
    client.heartbeat(id)
  }.runForeach(_ => ()).onComplete(_ => backend.close())
}
