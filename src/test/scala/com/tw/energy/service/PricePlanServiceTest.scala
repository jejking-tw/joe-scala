package com.tw.energy.service

import com.tw.energy.domain.{ElectricityReading, PricePlan}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import squants.energy.{KilowattHours, Kilowatts}
import squants.market.EUR

import java.time.Instant
import java.time.temporal.{ChronoUnit, TemporalUnit}

class PricePlanServiceTest extends AnyFlatSpec with Matchers {

  "calculateCost" should "return the total cost based on unit-rate" in {
    val now = Instant.now()
    val actual = PricePlanService.calculateCost(
      List(
        ElectricityReading(now.minus(1, ChronoUnit.HOURS), Kilowatts(0.42)),
        ElectricityReading(now.minus(2, ChronoUnit.HOURS), Kilowatts(0.32)),
        ElectricityReading(now.minus(3, ChronoUnit.HOURS), Kilowatts(0.70))
      ),
      PricePlan("test-plan", "energy-supplier-test", EUR(0.3)/KilowattHours(1))
    )
    actual should be(EUR(BigDecimal("0.288")))
  }


}
