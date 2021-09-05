package app

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import app.routes.MainRoutes

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.DurationInt
import scala.io.StdIn

object CpuMonitoringApp extends App {
  implicit val system: ActorSystem = ActorSystem("cpu-monitoring")

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  private val config = Config(frequencies = Seq(1.minute, 15.seconds, 30.seconds))

  val bindingFuture = Http().newServerAt("localhost", 9999).bind(MainRoutes.init(config).routes)

  println(s"Server online at ws://localhost:9999/\nPress RETURN to stop...")

  StdIn.readLine() // let it run until user presses return

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}
