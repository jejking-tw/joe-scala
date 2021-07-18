package com.tw.energy.repository
import cats.effect.{Async, Concurrent, IO}
import com.tw.energy.domain.ElectricityReading
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.repository.FileLineFormat.{parseLine, toLine}
import fs2.{Pipe, Stream, text}
import fs2.io.file.Files

import java.nio.file.{NoSuchFileException, Path, Paths, StandardOpenOption}

class FileStreamingMeterReadingRepository(private val path: Path) extends StreamingMeterReadingRepository {

  override def storeMeterReadings[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId, electricityReadings: fs2.Stream[F, ElectricityReading]): F[Unit] = {

    // open a stream to the file representing the meter reading store
    val meterFileSink = meterReadingsFileSink(smartMeterId)

    // read from the stream, convert to string representation and write to file stream
    electricityReadings
      .map(toLine(_))
      .through(text.lines)
      .through(text.utf8Encode)
      .through(meterFileSink)
      .compile
      .drain

    // when the input stream is finished, close the output file stream <- guess the fs2 utils do this for us
  }

  private[this] def meterReadingsFileSink[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId) = {
    val path: Path = meterFilePath(smartMeterId)
    Files[F].writeAll(path, Seq(StandardOpenOption.APPEND, StandardOpenOption.CREATE))
  }

  override def getMeterReadings[F[_] : Files : Concurrent : Async](smartMeterId: SmartMeterId): Stream[F, ElectricityReading] = {
    val path = meterFilePath(smartMeterId)
    Files[F].readAll(path, 4096)
      .through(text.utf8Decode)
      .through(text.lines)
      .filter(! _.isBlank)
      .map(parseLine(_))
      .handleErrorWith(
        _ match {
          case e:NoSuchFileException => Stream.empty
          case throwable: Throwable => throw throwable
      })
  }

  private def meterFilePath[F[_] : Files : Concurrent : Async](smartMeterId: SmartMeterId) = {
    val meterFilePath = path.resolve(smartMeterId)
    meterFilePath
  }
}
