package src.app.routes

import akka.Done
import akka.http.javadsl.server.RouteResult
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import src.app.{ClientStatus, CpuRawData}
import src.app.CpuMonitoringApp._
import src.app.services.{ClientsService, CpuReportService}

import java.time.Instant
import java.util.UUID
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success}

trait Routes[Context, Result] {
  def routes: Context => Future[Result]
}

final class MainRoutes private (clientsRoutes: ClientsRoutes,
                       reportsRoutes: CpuReportsRoutes) extends Routes[RequestContext, RouteResult]{
  def routes: Route = path("cpu")(
    concat(
      clientsRoutes.routes,
      reportsRoutes.routes
    )
  )
}

object MainRoutes {
  def apply(cpuReportService: CpuReportService, clientsService: ClientsService): MainRoutes = {
    val clientsRoutes = new ClientsRoutes(clientsService)
    val cpuReportsRoutes = new CpuReportsRoutes(cpuReportService)
    new MainRoutes(clientsRoutes, cpuReportsRoutes)
  }
}

final class ClientsRoutes(clientsService: ClientsService) extends Routes[RequestContext, RouteResult]{
  def routes: Route =
    concat(path("status") {
      handleWebSocketMessages(heartbeat)
    },path("collect"){
      handleWebSocketMessages(cpuData)
    })

  private def heartbeat: Flow[Message, Message, Any] =
    Flow[Message]
      .via(decoder)
      .via(saveStatus)

  private def cpuData: Flow[Message, Message, Any] =
    Flow[Message]
      .via(decoder)
      .via(aggregator)

  private def saveStatus: Flow[String, Message, Any] =
    Flow[String]
      .mapAsync(4) { tm =>
        clientsService.saveStatus(ClientStatus(UUID.fromString(tm), isTurnedOn = true, Instant.now()))
      }.mapConcat { _ =>
        TextMessage(Source.single("Thanks!")) :: Nil
    }

  private def decoder: Flow[Message, String, Any] =
    Flow[Message]
      .map {
        //here must be decoder
        //decode[CpuDataRaw](tm)
        case tm: TextMessage => tm.getStrictText
        case _: BinaryMessage => ""
      }.collect({case s if s.nonEmpty => s})

  private def aggregator: Flow[String, Message, Any] =
    Flow[String]
      .mapAsync(4){ tm =>
        clientsService.aggregate(CpuRawData(UUID.randomUUID(), tm, Instant.now()))
      }.mapConcat { _ =>
      TextMessage(Source.single("Thanks!")) :: Nil
    }
}

final class CpuReportsRoutes(cpuReportService: CpuReportService) extends Routes[RequestContext, RouteResult] {
  def routes: Route =
    path("report") {
      concat(
        path(JavaUUID) { id =>
          get {
            onComplete(cpuReportService.getReport(id)) {
              case Success(Some(res)) => complete(StatusCodes.OK, res.toString)
              case Success(_) => complete(StatusCodes.NotFound)
              case Failure(_) => complete(StatusCodes.InternalServerError)
            }
          }
        },
        path("report/allFailed") {
          get {
            onComplete(cpuReportService.getAllFailed) {
              case Success(value) if value.isEmpty => complete(StatusCodes.NotFound)
              case Success(res) => complete(StatusCodes.OK, res.toString)
              case Failure(_) => complete(StatusCodes.InternalServerError)
            }
          }
        }
      )
    }
}
