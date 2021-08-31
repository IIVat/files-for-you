package src.app

import sttp.capabilities.WebSockets
import sttp.capabilities.akka.AkkaStreams
import sttp.client3
import sttp.client3.{SttpBackend, UriContext, asWebSocket, basicRequest}
import sttp.ws.WebSocket

import java.util.UUID
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait CpuMonitoringClient[F[_]] {
  def heartbeat[T, R](body: T): F[R]
//  def sendCpuData[T]: F[T]
}

final class CpuMonitoringClientLive private (val config: ClientConfig)(implicit backend: SttpBackend[Future, AkkaStreams with WebSockets]){
  private def post(path: String, msg: String)(f: WebSocket[Future] => Future[Unit]): Future[client3.Response[Either[String, Unit]]] = {
    basicRequest
      .body(msg)
      .post(uri"${config.uri}/$path")
      .response(asWebSocket(f))
      .send(backend)
  }

  def heartbeat(userId: UUID): Future[Unit] = {
    def func(ws: WebSocket[Future]): Future[Unit] = {
      def send() = ws.sendText(s"$userId")
      def receive() = ws.receiveText().map(t => println(s"RECEIVED: $t"))
      for {
        _ <- send()
        _ <- receive()
      } yield ()
    }
    post("status", userId.toString)(func).flatMap {
      r => r.body.fold(err => {
        println(s"Error $err")
        Future.unit
      }, _ => Future.unit)
    }
  }
}

object CpuMonitoringClientLive {
  def apply(uri: String)(implicit backend: SttpBackend[Future, AkkaStreams with WebSockets]): CpuMonitoringClientLive = {
    new CpuMonitoringClientLive(ClientConfig(uri))
  }
}

case class ClientConfig(uri: String)