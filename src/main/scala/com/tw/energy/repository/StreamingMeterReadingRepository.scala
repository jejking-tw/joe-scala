package com.tw.energy.repository

import cats.effect.{Async, Concurrent}
import com.tw.energy.domain.ElectricityReading
import com.tw.energy.domain.StringTypes.SmartMeterId
import fs2.Stream
import fs2.io.file.Files

trait StreamingMeterReadingRepository {

  def storeMeterReadings[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId, electricityReadings: Stream[F, ElectricityReading]): Unit

  def getMeterReadings[F[_]: Files : Concurrent : Async](smartMeterId: SmartMeterId): Stream[F, ElectricityReading]

}
