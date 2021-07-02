package com.tw.energy.repository
import cats.effect.IO
import cats.effect.kernel.Sync
import cats.effect.unsafe.implicits.global
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.repository.FileMeterReadingRepository.{parseLine, toLine}
import squants.energy.Kilowatts

import java.nio.file.{Files, Path, StandardOpenOption}
import java.time.Instant
import scala.collection.mutable
import scala.jdk.CollectionConverters._

class InMemoryMeterReadingRepository extends MeterReadingRepository {

  private val map = new mutable.HashMap[SmartMeterId, Seq[ElectricityReading]]

  override def getReadings[F[_] : Sync](smartMeterId: SmartMeterId): F[Option[Seq[ElectricityReading]]] = {
    Sync[F].delay {
      map.get(smartMeterId)
    }
  }

  override def storeReadings[F[_] : Sync](meterReadings: MeterReadings): F[Unit] = {
    Sync[F].delay {
      val updatedListOfElectricityReadings = map.get(meterReadings.smartMeterId) match {
        case Some(existingReadings: Seq[ElectricityReading]) => existingReadings ++  meterReadings.electricityReadings
        case None => meterReadings.electricityReadings
      }
      map.put(meterReadings.smartMeterId, updatedListOfElectricityReadings)
    }
  }
}

class FileMeterReadingRepository(private val path: Path) extends MeterReadingRepository {

  override def getReadings[F[_]:Sync](smartMeterId: SmartMeterId): F[Option[Seq[ElectricityReading]]] = {
    Sync[F].delay{
      val meterFilePath = path.resolve(smartMeterId)
      if (Files.exists(meterFilePath)) {
        Some(Files.readAllLines(meterFilePath).asScala.map(parseLine).toSeq)
      } else {
        None
      }
    }
  }
  // TODO - look for a file with the name of the smartMeterId underneath our directory path
  // open it, read all the lines
  // map each line to an electricity reading
  // return the readings
  // if it does not exist, return None
  // and if the parsing fails.... what then?

  override def storeReadings[F[_]:Sync](meterReadings: MeterReadings): F[Unit] = {
    def fileHasBeenWrittenToAlready(meterFilePath: Path) = {
      Files.size(meterFilePath) > 0
    }

    def appendNewLine(meterFilePath: Path) = {
      Files.writeString(meterFilePath, System.lineSeparator(), StandardOpenOption.APPEND)
    }

    Sync[F].delay {
      val meterFilePath = path.resolve(meterReadings.smartMeterId)
      val lines = meterReadings.electricityReadings.map(toLine(_))
      if (fileHasBeenWrittenToAlready(meterFilePath)) {
        appendNewLine(meterFilePath)
      }
      Files.write(meterFilePath, lines.asJava, StandardOpenOption.APPEND)
      ()
    }
  }



}

object FileMeterReadingRepository {

  def toLine(reading: ElectricityReading): String = s"${reading.time.getEpochSecond},${reading.reading.toKilowatts}"


  val line = raw"(.+),(.+)".r

  def parseLine(input: String): ElectricityReading = {
    input match {
      case line (instant, power) => ElectricityReading(Instant.ofEpochSecond(instant.toLong), Kilowatts(power.toDouble))
    }
  }
}
