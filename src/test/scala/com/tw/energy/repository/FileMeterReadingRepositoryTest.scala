package com.tw.energy.repository

import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.tw.energy.domain.ElectricityReading
import org.scalatest.BeforeAndAfterAll
import org.scalatest.freespec.AsyncFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator
import scala.concurrent.ExecutionContext

class FileMeterReadingRepositoryTest extends AsyncFreeSpec with AsyncIOSpec with Matchers with BeforeAndAfterAll {

  override implicit def executionContext: ExecutionContext = ExecutionContext.Implicits.global

  def withTempDirectory[F](testCode: Path => F): F = {
    val directory = Files.createTempDirectory("meter-reading-repository")
    try {
      testCode(directory)
    }
    finally Files.walk(directory)
      .sorted(Comparator.reverseOrder[Path]())
      .map(_.toFile)
      .forEach(file => file.delete())
  }

  "repository" - {
    "return None if no file for the meter id can be located" in withTempDirectory { tempDir =>
      val fileBasedRepository = new FileMeterReadingRepository(tempDir)

      val ret = fileBasedRepository.getReadings[IO]("meter-1")

      ret.asserting(_ shouldBe Option.empty)
    }


    "return an empty sequence if the file exists but is empty" in withTempDirectory { tempDir =>
      val fileBasedRepository = new FileMeterReadingRepository(tempDir)
      Files.createFile(tempDir.resolve("meter-2"))

      val ret = fileBasedRepository.getReadings[IO]("meter-2")

      ret.asserting(_  shouldBe Option(Seq.empty))
    }

    "return a sequence of one electricity reading given a file with one correctly formatted entry" in withTempDirectory { tempDir =>
      val fileBasedRepository = new FileMeterReadingRepository(tempDir)
      val meter3Path = Files.createFile(tempDir.resolve("meter-3"))
      Files.write(meter3Path, "1624289430,1234.56".getBytes())

      val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

      val ret = fileBasedRepository.getReadings[IO]("meter-3")
      ret.asserting(_  shouldBe Some(Seq(expectedElectrityReading)))
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
