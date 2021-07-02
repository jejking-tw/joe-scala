package com.tw.energy.service

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import com.tw.energy.domain.StringTypes.SmartMeterId
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.repository.MeterReadingRepository

class MeterReadingService(private val repository: MeterReadingRepository) {
  def getReadings(smartMeterId: SmartMeterId): Option[Seq[ElectricityReading]] = {
    repository.getReadings[IO](smartMeterId).unsafeRunSync()
  }

  def storeReadings(meterReadings: MeterReadings): Unit = {
    repository.storeReadings[IO](meterReadings).unsafeRunSync()
  }
}
