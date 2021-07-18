package com.tw.energy.repository

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.tw.energy.domain.ElectricityReading
import com.tw.energy.repository.FileLineFormat.toLine
import com.tw.energy.repository.FileRepositoryTestUtil.withTempDirectory
import org.scalatest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.Files
import java.time.Instant
import scala.jdk.CollectionConverters.IterableHasAsJava

class StreamingMeterReadingRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll {

  "the repository" - {
    "(reading)" - {
      "return empty stream if no file for the meter id can be located" in withTempDirectory { tempDir =>
        val ret: IO[List[ElectricityReading]] = tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          streamingMeterReadingRepository.getMeterReadings[IO]("meter-1").compile.toList
        })

        ret.asserting(_ shouldBe Nil)
      }

      "should return an empty stream if meter file exists but is empty" in withTempDirectory { tempDir =>
        val ret: IO[List[ElectricityReading]] = tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          Files.createFile(path.resolve("meter-2"))
          streamingMeterReadingRepository.getMeterReadings[IO]("meter-2").compile.toList
        })

        ret.asserting(_ shouldBe Nil)
      }

      "should return a sequence of one electricity reading given a file with one correctly formatted entry" in withTempDirectory { tempDir =>
        val expectedElectricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
        val ret = tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          val meter3Path = Files.createFile(path.resolve("meter-3"))
          Files.write(meter3Path, "1624289430,1234.56".getBytes())

          streamingMeterReadingRepository.getMeterReadings[IO]("meter-3").compile.toList
        })
        ret.asserting(_ shouldBe List(expectedElectricityReading))
      }

      "should return a sequence of 10 electrity readings given a file with ten correctly formatted entries" in withTempDirectory { tempDir =>

        val expectedReadings = (0 to 1000 by 100)
          .zip(0 to 10)
          .map(t => ElectricityReading(Instant.ofEpochSecond(1624289430 + t._1), Kilowatts(1234.56 + t._2)))

        val ret = tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          val meter4Path = Files.createFile(path.resolve("meter-4"))
          Files.write(meter4Path, expectedReadings.toList.map(toLine(_)).asJava)

          streamingMeterReadingRepository.getMeterReadings[IO]("meter-4").compile.toList
        })

        ret.asserting(_ shouldBe expectedReadings.toList)
      }

    }

    "(writing)" - {
      
    }


  }

}
