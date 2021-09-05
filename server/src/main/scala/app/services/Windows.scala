package app.services

import app.CpuData

import scala.collection.mutable
import scala.concurrent.duration._

sealed trait Command {
  def window: Window
}

case class Open(window: Window) extends Command
case class Close(window: Window) extends Command
case class Add(data: CpuData, window: Window) extends Command

class WindowCommandGenerator(frequencies: Seq[FiniteDuration]) {
  private val MaxDelay = 5.seconds.toMillis
  private var watermark = 0L
  private val openWindows = mutable.Set.empty[Window]

  def generate(timestamp: Long, data: CpuData): List[Command] = {
    watermark = math.max(watermark, timestamp - MaxDelay)
    if (timestamp < watermark) Nil
    else {
      val closeCommands = openWindows.flatMap { ow =>
        if (ow.to < watermark) {
          openWindows.remove(ow)
          Some(Close(ow))
        } else {
          None
        }
      }

      //This is key. It checks the different frequencies and it filters out the frequencies that already have an open window.
      val toBeOpen = frequencies.filter(w => openWindows.forall(ow => ow.duration.get != w.toMillis))

      val openCommands = toBeOpen.flatMap { duration =>
        //it creates a list of windows with the frequency defined by the config.
        val listWindows = Window.create(timestamp, duration)
        listWindows.flatMap { w =>
          openWindows.add(w)
          Some(Open(w))
        }
      }

      val addCommands = openWindows.map(Add(data, _))
      openCommands.toList ++ closeCommands.toList ++ addCommands.toList
    }
  }
}

case class Window(from: Long, to: Long, duration: Option[Long] = None)

object Window {
  def create(ts: Long, duration: FiniteDuration): Set[Window] = {
    //As the window length and window step is one, it will return just a Set of one window.
    val WindowLength = duration.toMillis
    val WindowStep = duration.toMillis
    val WindowsPerEvent = (WindowLength / WindowStep).toInt

    val firstWindowStart = ts - ts % WindowStep - WindowLength + WindowStep
    (for (i <- 0 until WindowsPerEvent) yield Window(
      firstWindowStart + i * WindowStep,
      firstWindowStart + i * WindowStep + WindowLength, Some(duration.toMillis)
    )).toSet
  }
}
