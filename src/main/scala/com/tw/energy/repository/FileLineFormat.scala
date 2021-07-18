package com.tw.energy.repository

import com.tw.energy.domain.ElectricityReading
import squants.energy.Kilowatts

import java.time.Instant

object FileLineFormat {

  val line = raw"(.+),(.+)".r

  def toLine(reading: ElectricityReading): String = s"${reading.time.getEpochSecond},${reading.reading.toKilowatts}"

  def parseLine(input: String): ElectricityReading = {
    input match {
      case line (instant, power) => ElectricityReading(Instant.ofEpochSecond(instant.toLong), Kilowatts(power.toDouble))
    }
  }

}
