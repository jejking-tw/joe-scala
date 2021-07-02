package com.tw.energy.repository

import cats.effect.kernel.Sync
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId

trait MeterReadingRepository {

  def getReadings[F[_]:Sync](smartMeterId: SmartMeterId): F[Option[Seq[ElectricityReading]]]

  def storeReadings[F[_]:Sync](meterReadings: MeterReadings): F[Unit]

}
