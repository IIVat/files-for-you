package app

import akka.NotUsed
import akka.stream.scaladsl.{Flow, Source}
import akka.util.ByteString
import io.circe.generic.auto._
import io.circe.syntax._
import sttp.capabilities.WebSockets
import sttp.capabilities.akka._
import sttp.client3._
import sttp.ws.{WebSocket, WebSocketFrame}

import java.time.Instant
import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

trait CpuMonitoringClient[F[_]] {
  def sendStatus(status: ClientStatus): F[Unit]
  def sendCpuData(data: Array[UUID]): F[Unit]
}

final class CpuMonitoringClientLive private (val config: ClientConfig)(
  implicit backend: SttpBackend[Future, AkkaStreams with WebSockets]
) extends CpuMonitoringClient[Future]{
  private def post(path: String, msg: String)(f: WebSocket[Future] => Future[Unit]): Future[Response[Either[String, Unit]]] = {
    basicRequest
      .body(msg)
      .post(uri"${config.uri}/cpu/$path")
      .response(asWebSocket(f))
      .send(backend)
  }

  override def sendStatus(status: ClientStatus): Future[Unit] = {
    def func(ws: WebSocket[Future]): Future[Unit] = {
      def send() = ws.sendText(s"${status.asJson.noSpaces}")
      def receive() = ws.receiveText().map(t => println(s"RECEIVED: $t"))
      for {
        _ <- send()
        _ <- receive()
      } yield ()
    }
    post("status", status.asJson.noSpaces)(func).flatMap {
      r => r.body.fold(err => {
        println(s"Error $err")
        Future.unit
      }, _ => Future.unit)
    }
  }


  override def sendCpuData(cid: Array[UUID]): Future[Unit] = {
    val data = cid.map(cid => CpuData(cid, "",  Instant.now))
    def src(i: Int)(genCpu: Int, now: Instant) = data(i).copy(cpuValue = genCpu.toString, timestamp = now).asJson.noSpaces
//    val source = (genCpu: Int, now: Instant) =>
//      Source
//      .tick(0.second, 1.second, ())
//      .map(_ => WebSocketFrame.Text(src(genCpu, now), false, None))

    val webSocketFramePipe: Flow[WebSocketFrame.Data[_], WebSocketFrame, NotUsed] = Flow[WebSocketFrame.Data[_]].map{
      case o @ WebSocketFrame.Text(str, _, _) =>
        println(s"received string: $str")
        o
      case other =>
        println(s"received other: $other")
        other
    }

    basicRequest
      .streamBody(AkkaStreams)(Source.single(ByteString.fromString("Take CPU data")))
      .post(uri"${config.uri}/cpu/collect")
      .response(asWebSocketStreamAlways(AkkaStreams)(webSocketFramePipe.prepend(
        Source
          .tick(0.second, 1.second, ())
          .map(_ => WebSocketFrame.Text(
            CpuData(data(Random.nextInt(4)).cid, Random.nextInt(100).toString,  Instant.now).asJson.noSpaces,
//            Random.nextString(4),
//            src(Random.nextInt(4))(Random.nextInt(100), Instant.now),
//            data(Random.nextInt(4)).copy(cpuValue = Random.nextInt(100).toString, timestamp = Instant.now).asJson.noSpaces,
            false,
            None))
      )))
      .send(backend)
      .map { response => println(s"RECEIVED:\n${response.body}") }
  }
}

object CpuMonitoringClientLive {
  def apply(uri: String)(implicit backend: SttpBackend[Future, AkkaStreams with WebSockets]): CpuMonitoringClientLive = {
    new CpuMonitoringClientLive(ClientConfig(uri))
  }
}

case class ClientConfig(uri: String)