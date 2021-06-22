package com.tw.energy.repository

import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.Kilowatts

import java.nio.file.{Files, Path}
import java.time.Instant
import java.util.Comparator
import scala.jdk.CollectionConverters._

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

  it should "create a new file for a meter given a new reading and no existing file" in {
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)
    val meter4Path = Files.createFile(tempDir.resolve("meter-4"))

    val electricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    fileBasedRepository.storeReadings(MeterReadings("meter-4", List(electricityReading)))

    Files.readAllLines(meter4Path).asScala shouldBe Seq("1624289430,1234.56")

  }

  it should "append to a file for a given meter given a new reading and an existing file" in {
    val fileBasedRepository = new FileMeterReadingRepository(tempDir)
    val meter5Path = Files.createFile(tempDir.resolve("meter-5"))

    val electricityReadings = Seq(ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56)), ElectricityReading(Instant.ofEpochSecond(1624375589), Kilowatts(98.76)))

    Files.writeString(meter5Path, "1624289430,1234.56") // append first reading

    fileBasedRepository.storeReadings(MeterReadings("meter-5", electricityReadings.tail.toList))

    fileBasedRepository.getReadings("meter-5") shouldBe Some(electricityReadings)
  }

  "the line parser" should "parse a string representing a line to an electricity reading" in {
    val input = "1624289430,1234.56"
    val expectedElectrityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    FileMeterReadingRepository.parseLine(input) shouldBe expectedElectrityReading
  }

  "the line serializer" should "serialize an electricity reading to a string" in {
    val expectedSerializedForm = "1624289430,1234.56"
    val reading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    FileMeterReadingRepository.toLine(reading) shouldBe expectedSerializedForm
  }

}
