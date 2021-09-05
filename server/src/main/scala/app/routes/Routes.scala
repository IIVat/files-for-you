package app.routes

import akka.http.javadsl.server.RouteResult
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{RequestContext, Route}
import akka.stream.scaladsl.{Flow, Sink, Source}
import app.dao.{AggregationStorageImpl, CpuDataDaoImpl}
import app.services._
import app.{ClientStatus, Config, CpuData}
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode

import java.time.Instant
import scala.concurrent.Future
import scala.util.{Failure, Success}

trait Routes[Context, Result] {
  def routes: Context => Future[Result]
}

final class MainRoutes private (clientsRoutes: ClientsRoutes,
                                reportsRoutes: CpuReportsRoutes) extends Routes[RequestContext, RouteResult]{
  def routes: Route = pathPrefix("cpu")(
    concat(
      clientsRoutes.routes,
      reportsRoutes.routes
    )
  )
}

object MainRoutes {
  def init(config: Config): MainRoutes = {
    val cpuReportService  = new CpuReportServiceImpl()
    val clientsService = new ClientsStatusServiceImpl()
    val storage = new AggregationStorageImpl()
    val aggregator = new Aggregator(storage, config)
    val cpuLogDao = new CpuDataDaoImpl()
    val cpuLogService = new CpuLogServiceImpl(cpuLogDao)
    val clientsRoutes = new ClientsRoutes(clientsService,  cpuLogService, aggregator)
    val cpuReportsRoutes = new CpuReportsRoutes(cpuReportService)
    new MainRoutes(clientsRoutes, cpuReportsRoutes)
  }
}

final class ClientsRoutes(clientsService: ClientsStatusService, cpuLogService: CpuLogService, aggregator: Aggregator) extends Routes[RequestContext, RouteResult]{
  def routes: Route =
    concat(
      path("status") {
        handleWebSocketMessages(heartbeat)
    }, path("collect"){
        handleWebSocketMessages(cpuData)
    })

  private def heartbeat: Flow[Message, Message, Any] =
    Flow[Message]
      .via(decoder[ClientStatus])
      .via(saveStatus)

  private def cpuData: Flow[Message, Message, Any] =
    Flow[Message]
      .via(decoder[CpuData])
      .via(aggregator.aggregate)
      .alsoTo(saveToLog)
      .via(toMessage)

  private def saveToLog = {
    Flow[CpuData]
      .mapAsync(4)(cpuLogService.insert)
      .to(Sink.ignore)
  }

  private def decoder[T: Decoder]: Flow[Message, T, Any] =
    Flow[Message]
      .map {
        case tm: TextMessage =>
          decode[T](tm.getStrictText).toOption
        case _: BinaryMessage =>
          None
      }.collect({case Some(data) => data})

  private def saveStatus: Flow[ClientStatus, Message, Any] =
    Flow[ClientStatus]
      .mapAsync(4) { tm =>
        clientsService.saveStatus(ClientStatus(tm.cid, isTurnedOn = true, Instant.now()))
      }.mapConcat { _ =>
        TextMessage(Source.single("Thanks for the STATUS!")) :: Nil
    }

  private def toMessage: Flow[CpuData, Message, Any] =
    Flow[CpuData]
      .mapConcat { _ =>
      TextMessage(Source.single("Thanks for CPU!")) :: Nil
    }
}

final class CpuReportsRoutes(cpuReportService: CpuReportService) extends Routes[RequestContext, RouteResult] {
  def routes: Route =
    path("report") {
      concat(
        //TODO put timestamp and required window
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
