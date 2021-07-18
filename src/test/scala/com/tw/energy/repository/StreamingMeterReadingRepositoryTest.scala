package com.tw.energy.repository

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.tw.energy.domain.ElectricityReading
import com.tw.energy.repository.FileLineFormat.toLine
import com.tw.energy.repository.FileRepositoryTestUtil.withTempDirectory
import fs2.Stream
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.Files._
import java.nio.file.{Files, StandardOpenOption}
import java.time.Instant
import scala.jdk.CollectionConverters.{CollectionHasAsScala, IterableHasAsJava}

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
          createFile(path.resolve("meter-2"))
          streamingMeterReadingRepository.getMeterReadings[IO]("meter-2").compile.toList
        })

        ret.asserting(_ shouldBe Nil)
      }

      "should return a sequence of one electricity reading given a file with one correctly formatted entry" in withTempDirectory { tempDir =>
        val expectedElectricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
        val ret = tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          val meter3Path = createFile(path.resolve("meter-3"))
          write(meter3Path, "1624289430,1234.56".getBytes())

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
          val meter4Path = createFile(path.resolve("meter-4"))
          write(meter4Path, expectedReadings.toList.map(toLine(_)).asJava)

          streamingMeterReadingRepository.getMeterReadings[IO]("meter-4").compile.toList
        })

        ret.asserting(_ shouldBe expectedReadings.toList)
      }

    }

    "(writing)" - {
      "should create a new file for a meter given a new reading and no pre-existing file" in withTempDirectory { tempDir =>
        val ret = tempDir.use(path => {
          val electricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)

          for {
            meter5Path <- IO.blocking(path.resolve("meter-5"))
            _ <- streamingMeterReadingRepository.storeMeterReadings[IO]("meter-5", Stream.emit(electricityReading))
            lines <- IO.blocking(readAllLines(meter5Path))
          } yield lines.asScala.toList
        })
        ret.asserting(_ shouldBe Seq("1624289430,1234.56"))
      }

      "append to a file for a given meter given a new reading and an existing file" in withTempDirectory { tempDir =>

        val existingReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
        val newReading = ElectricityReading(Instant.ofEpochSecond(1624375589), Kilowatts(98.76))

        tempDir.use(path => {
          val streamingMeterReadingRepository = new FileStreamingMeterReadingRepository(path)
          for {
            meter6Path <- IO.blocking {
              Files.createFile(path.resolve("meter-6"))
            }
            _ <- IO.blocking {
              Files.write(meter6Path, List("1624289430,1234.56").asJava, StandardOpenOption.APPEND)
            }
            _ <- streamingMeterReadingRepository.storeMeterReadings[IO]("meter-6", Stream.emit(newReading))
            lines <- IO.blocking(readAllLines(meter6Path))
          } yield lines.asScala.toList
        }).asserting(_ shouldBe Seq(existingReading, newReading).map(toLine(_)))
      }
    }


  }

}
