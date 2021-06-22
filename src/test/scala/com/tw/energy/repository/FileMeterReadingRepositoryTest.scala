package com.tw.energy.repository

import com.tw.energy.domain.ElectricityReading
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator

class FileMeterReadingRepositoryTest extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  def withTempDirectory(testCode: Path => Any): Unit = {
    val directory = Files.createTempDirectory("meter-reading-repository")
    try {
      testCode(directory)
    }
    finally Files.walk(directory)
      .sorted(Comparator.reverseOrder[Path]())
      .map(_.toFile)
      .forEach(file => file.delete())
  }

  "repository" should "return None if no file for the meter id can be located" in withTempDirectory{ tempDir =>
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)

    val ret = fileBasedRepository.getReadings("meter-1")

    ret shouldBe Option.empty
  }

  it should "return an empty sequence if the file exists but is empty" in withTempDirectory{ tempDir =>
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)
    Files.createFile(tempDir.resolve("meter-2"))

    val ret = fileBasedRepository.getReadings("meter-2")

    ret shouldBe Option(Seq.empty)
  }

  it should "return a sequence of one electricity reading given a file with one correctly formatted entry" in withTempDirectory{ tempDir =>
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)
    val meter3Path = Files.createFile(tempDir.resolve("meter-3"))
    Files.write(meter3Path, "1624289430,1234.56".getBytes())

    val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    val ret = fileBasedRepository.getReadings("meter-3")
    ret shouldBe Some(Seq(expectedElectrityReading))
  }

  "the line parser" should "parse a string representing a line to an electricity reading" in {
    val input = "1624289430,1234.56"
    val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    FileMeterReadingRepository.parseLine(input) shouldBe expectedElectrityReading
  }

}
