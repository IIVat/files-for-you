package app.services

import akka.NotUsed
import akka.stream.SinkShape
import akka.stream.scaladsl._
import app.dao.AggregationStorage
import app.{Config, CpuData}

import java.util.UUID

class Aggregator(storage: AggregationStorage, config: Config) {
  def aggregate: Flow[CpuData, CpuData, NotUsed] = {
    Flow[CpuData]
      .alsoTo(_aggregate)
  }

  private def _aggregate: Sink[CpuData, NotUsed] = Flow[CpuData].statefulMapConcat { () =>
    val generator = new WindowCommandGenerator(config.frequencies)
    data =>
      val timestamp = data.timestamp
      generator.generate(timestamp.toEpochMilli, data)
  }.groupBy(64, command => command.window)
    .to(createGraph())

  private  def createGraph() = GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
    import GraphDSL.Implicits._
    val bcast = builder.add(Broadcast[Command](config.frequencies.length))
    config.frequencies.foreach { frequency =>
      //Filter the windows that correspond to our frequency
      val filter = Flow[Command].filter(_.window.duration.get == frequency.toMillis)

      val commands = Flow[Command].takeWhile(!_.isInstanceOf[Close])

      val aggregator: Flow[Command, CpuData, NotUsed] = Flow[Command].fold(List[CpuData]()) {
        case (agg, Open(_)) => agg
        case (agg, Close(_)) => agg
        case (agg, Add(data, _)) => data :: agg
      }.mapConcat(a => a)

      val groupedByClientId = Flow[CpuData].fold(Map[UUID, List[CpuData]]()) { (agg, data) =>
        val key = data.cid
        if (agg.contains(key)) {
          val current = agg(key)
          agg + (key -> (data :: current))
        } else {
          agg + (key -> List(data))
        }
      }.mapConcat(a => a.toList)

      val storageSink = Sink.foreach[(UUID, List[CpuData])](x => storage.save(x._1,frequency.toMillis , x._2))

      bcast ~> filter ~> commands ~> aggregator ~> groupedByClientId ~> storageSink
    }
    SinkShape(bcast.in)
  }
}
