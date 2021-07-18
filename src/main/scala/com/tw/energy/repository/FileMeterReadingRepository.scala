package com.tw.energy.repository
import cats.effect.kernel.Sync
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.repository.FileLineFormat.{parseLine, toLine}

import java.nio.file.{Files, Path, StandardOpenOption}
import scala.jdk.CollectionConverters._

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
    Sync[F].delay {
      val meterFilePath = path.resolve(meterReadings.smartMeterId)
      val lines = meterReadings.electricityReadings.map(toLine(_))
      if (!Files.exists(meterFilePath)) {
        Files.createFile(meterFilePath)
      }
      Files.write(meterFilePath, lines.asJava, StandardOpenOption.APPEND)
      ()
    }
  }

}

