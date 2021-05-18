package com.tw.energy.generator

import java.time.Instant
import com.tw.energy.domain.ElectricityReading
import squants.energy.Kilowatts

import scala.util.Random

object Generator {
  def generateReadings(
                        number: Int,
                        deltaSeconds: Int = 10,
                        time: Instant = Instant.now(),
                        random: Random = new Random(new java.util.Random())
                      ): List[ElectricityReading] = {
    (0 until number)
      .map(i => ElectricityReading(time.minusSeconds(deltaSeconds * i), Kilowatts(random.nextGaussian().abs)))
      .sortBy(_.time)
      .toList
  }

}
