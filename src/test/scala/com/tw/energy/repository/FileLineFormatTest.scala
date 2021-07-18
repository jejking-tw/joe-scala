package com.tw.energy.repository

import com.tw.energy.domain.ElectricityReading
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.{Kilowatts, Watts}

import java.time.Instant

class FileLineFormatTest extends AnyFreeSpec with Matchers {

  import FileLineFormat._

  "the line parser should parse a string representing a line to an electricity reading" in {
    val input = "1624289430,1234.56"
    val expectedElectricityReading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    parseLine(input) shouldBe expectedElectricityReading
  }

  "the line serializer should serialize an electricity reading to a string" in {
    val reading = ElectricityReading(Instant.ofEpochSecond(1624289430), Kilowatts(1234.56))

    val expectedSerializedForm = "1624289430,1234.56"
    toLine(reading) shouldBe expectedSerializedForm
  }

  "the line serializer converts power readings to kW" in {
    val reading = ElectricityReading(Instant.ofEpochSecond(1624289430), Watts(1234.56 * 1000))

    val expectedSerializedForm = "1624289430,1234.56"
    toLine(reading) shouldBe expectedSerializedForm
  }
}
