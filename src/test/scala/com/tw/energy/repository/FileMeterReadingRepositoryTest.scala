package com.tw.energy.repository

import cats.effect.{IO, Resource}
import cats.effect.testing.scalatest.AsyncIOSpec
import com.tw.energy.domain.ElectricityReading
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator

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
      val ret = tempDir.use(path => {
        val fileBasedRepository = new FileMeterReadingRepository(path)
        fileBasedRepository.getReadings[IO]("meter-1")
      })

      ret.asserting(_ shouldBe Option.empty)
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

  "the line parser" - {
    "parse a string representing a line to an electricity reading" in {
      val input = "1624289430,1234.56"
      val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

      FileMeterReadingRepository.parseLine(input) shouldBe expectedElectrityReading
    }
  }
}
