package com.tw.energy.repository

import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId

trait MeterReadingRepository {

  def getReadings(smartMeterId: SmartMeterId): Option[Seq[ElectricityReading]]

  def storeReadings(meterReadings: MeterReadings): Unit

}
