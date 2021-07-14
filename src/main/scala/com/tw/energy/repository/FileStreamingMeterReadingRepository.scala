package com.tw.energy.repository
import cats.effect.{Async, Concurrent, IO}
import com.tw.energy.domain.ElectricityReading
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.repository.FileMeterReadingRepository.toLine
import fs2.{Pipe, Stream, text}
import fs2.io.file.Files

import java.nio.file.{Path, Paths}

class FileStreamingMeterReadingRepository(private val path: Path) extends StreamingMeterReadingRepository {

  override def storeMeterReadings[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId, electricityReadings: fs2.Stream[F, ElectricityReading]): Unit = {

    // open a stream to the file representing the meter reading store
    val theSink = sink(smartMeterId)

    // read from the stream, convert to string representation and write to file stream
    electricityReadings
      .map(toLine(_))
      .through(text.lines)
      .through(text.utf8Encode)
      .through(theSink)
      .compile
      .drain

    // when the input stream is finished, close the output file stream <- guess the fs2 utils do this for us
  }

  def sink[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId) = {
    val meterFilePath = path.resolve(smartMeterId)
    Files[F].writeAll(meterFilePath)
  }
}
