package src.app

import akka.Done
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.{Flow, Sink, Source}
import src.app.routes.MainRoutes
import src.app.services.{ClientsService, ClientsServiceImpl, CpuReportServiceImpl}

import scala.concurrent._
import scala.io.StdIn

object CpuMonitoringApp extends App {
  implicit val system: ActorSystem = ActorSystem("cpu-monitoring")

  implicit val executionContext: ExecutionContextExecutor = system.dispatcher

  val mainRoutes =
    MainRoutes(
      new CpuReportServiceImpl(),
      new ClientsServiceImpl()
    )

  val bindingFuture = Http().newServerAt("localhost", 9999).bind(mainRoutes.routes)

  println(s"Server online at ws://localhost:9999/\nPress RETURN to stop...")

  StdIn.readLine() // let it run until user presses return

  bindingFuture
    .flatMap(_.unbind()) // trigger unbinding from the port
    .onComplete(_ => system.terminate()) // and shutdown when done
}


