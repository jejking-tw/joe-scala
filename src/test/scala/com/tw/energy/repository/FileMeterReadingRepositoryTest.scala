package com.tw.energy.repository

import cats.effect.testing.scalatest.AsyncIOSpec
import cats.effect.{IO, Resource}
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import org.scalatest
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.io.File.TempDirectory
import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator
import scala.jdk.CollectionConverters._

class FileMeterReadingRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll {

  def withTempDirectory[F](testCode: Resource[IO, Path] => F): F = {

    val tempDirResource = Resource.make(
    IO.blocking(Files.createTempDirectory("meter-reading-repository"))
    )(
    directory => {
        IO.blocking {
          Files.walk(directory)
          .sorted(Comparator.reverseOrder[Path]())
          .map(_.toFile)
          .forEach(file => file.delete())
        }
      })

    testCode(tempDirResource)
  }

  "repository" - {
    "return None if no file for the meter id can be located" in withTempDirectory { tempDir =>
      val ret: IO[Option[Seq[ElectricityReading]]] = tempDir.use(path => {
        val fileBasedRepository = new FileMeterReadingRepository(path)
        fileBasedRepository.getReadings[IO]("meter-1")
      })

      ret.asserting(_ shouldBe Option.empty):IO[scalatest.Assertion]
    }


    "return an empty sequence if the file exists but is empty" in withTempDirectory { tempDir =>
      val ret = tempDir.use(path => {
        val fileBasedRepository = new FileMeterReadingRepository(path)
        Files.createFile(path.resolve("meter-2"))
        fileBasedRepository.getReadings[IO]("meter-2")
      })

      ret.asserting(_ shouldBe Option(Seq.empty))
    }

    "return a sequence of one electricity reading given a file with one correctly formatted entry" in withTempDirectory { tempDir =>
      val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
      val ret = tempDir.use(path => {
        val fileBasedRepository = new FileMeterReadingRepository(path)
        val meter3Path = Files.createFile(path.resolve("meter-3"))
        Files.write(meter3Path, "1624289430,1234.56".getBytes())


        fileBasedRepository.getReadings[IO]("meter-3")
      })
      ret.asserting(_ shouldBe Some(Seq(expectedElectrityReading)))
    }
  }

  "repository should create a new file for a meter given a new reading and no existing file" in withTempDirectory { tempDir =>
    val ret = tempDir.use(path => {
      val fileBasedRepository = new FileMeterReadingRepository(path)
      val electricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

      for {
               meter4Path <- IO.blocking(Files.createFile(path.resolve("meter-4")))
                 _ <- fileBasedRepository.storeReadings[IO](MeterReadings("meter-4", List(electricityReading)))
                 lines <- IO.blocking(Files.readAllLines(meter4Path).asScala)
      } yield lines
    })
    ret.asserting(_ shouldBe Seq("1624289430,1234.56"))
  }


  "append to a file for a given meter given a new reading and an existing file" in withTempDirectory { tempDir =>

    val existingReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
    val newReading = ElectricityReading(Instant.ofEpochSecond(1624375589), Kilowatts(98.76))

    tempDir.use (path => {
      val fileBasedRepository = new FileMeterReadingRepository(path)
      for {
        meter5Path <- IO.blocking{ Files.createFile(path.resolve("meter-5"))}
        _ <- IO.blocking { Files.writeString(meter5Path, "1624289430,1234.56") }   // from existingReadingValues
        _ <- fileBasedRepository.storeReadings[IO](MeterReadings("meter-5", List(newReading)))
        readings <- fileBasedRepository.getReadings[IO]("meter-5")
      } yield readings
    })
    .asserting(_ shouldBe Some(Seq(
      existingReading,
      newReading
    )))
  }

//  "the line parser" should "parse a string representing a line to an electricity reading" in {
//    val input = "1624289430,1234.56"
//    val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
//
//    FileMeterReadingRepository.parseLine(input) shouldBe expectedElectrityReading
//  }
//
//  "the line serializer" should "serialize an electricity reading to a string" in {
//    val expectedSerializedForm = "1624289430,1234.56"
//    val reading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))
//
//    FileMeterReadingRepository.toLine(reading) shouldBe expectedSerializedForm
//  }
//
//  it should "convert power readings to kW" in {
//    val expectedSerializedForm = "1624289430,1234.56"
//    val reading = ElectricityReading(Instant.ofEpochSecond(1624289430), Watts(1234.56 * 1000))
//
//    FileMeterReadingRepository.toLine(reading) shouldBe expectedSerializedForm
//  }

}
