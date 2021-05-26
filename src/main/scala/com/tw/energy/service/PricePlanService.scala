package com.tw.energy.service


import com.tw.energy.domain.StringTypes.{PlanName, SmartMeterId}
import com.tw.energy.domain.{ElectricityReading, PricePlan}
import com.tw.energy.service.PricePlanService.calculateCost
import squants.energy.{Energy, Kilowatts, Power}
import squants.market.{EUR, Money}
import squants.time.{Hours, Time}

import scala.util.Try

class PricePlanService(pricePlans: Seq[PricePlan], meterReadingService: MeterReadingService) {

  def consumptionCostByPricePlan(smartMeterId: SmartMeterId): Option[Map[PlanName, Money]] = {
    meterReadingService.getReadings(smartMeterId).map { readings =>
      pricePlans.map(plan => plan.planName -> calculateCost(readings, plan)).toMap
    }
  }
}

object PricePlanService {

  private def calculateAverageReading(readings: Seq[ElectricityReading]): Power = {
    Kilowatts(readings.map(_.reading.toKilowatts).sum / readings.length)
  }

  private def calculateTimeElapsed(electricityReadings: Seq[ElectricityReading]) : Time = {
    val maybeTimeElapsed = for {
      first <- Try { electricityReadings.minBy(_.time ) }
      last <- Try { electricityReadings.maxBy(_.time) }
    } yield {
      Hours(java.time.Duration.between(first.time, last.time).getSeconds / 3600.0)
    }

    maybeTimeElapsed.getOrElse(Hours(0))
  }

  def calculateCost(readings: Seq[ElectricityReading], plan: PricePlan): Money = {
    if (readings.isEmpty) {
      return EUR(0)
    }

    val average: Power = calculateAverageReading(readings)
    val timeElapsed: Time = calculateTimeElapsed(readings)
    val energyConsumption : Energy = average * timeElapsed
    energyConsumption * plan.unitRate
  }
}


