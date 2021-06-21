package com.tw.energy.repository
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.repository.FileMeterReadingRepository.parseLine
import squants.energy.Kilowatts

import java.nio.file.{Files, Path}
import java.time.Instant
import scala.jdk.CollectionConverters._

class FileMeterReadingRepository(private val path: Path) extends MeterReadingRepository {
  override def getReadings(smartMeterId: SmartMeterId): Option[Seq[ElectricityReading]] = {
    IO {
      val meterFilePath = path.resolve(smartMeterId)
      if (Files.exists(meterFilePath)) {
        Some(Files.readAllLines(meterFilePath).asScala.map(parseLine(_)).toSeq)
      } else {
        None
      }
    }.unsafeRunSync()
  }
  // TODO - look for a file with the name of the smartMeterId underneath our directory path
  // open it, read all the lines
  // map each line to an electricity reading
  // return the readings
  // if it does not exist, return None
  // and if the parsing fails.... what then?

  override def storeReadings(meterReadings: MeterReadings): Unit = ???



}

object FileMeterReadingRepository {

  val line = raw"(.+),(.+)".r

  def parseLine(input: String): ElectricityReading = {
    input match {
      case line (instant, power) => ElectricityReading(Instant.ofEpochSecond(instant.toLong), Kilowatts(power.toDouble))
    }
  }
}
