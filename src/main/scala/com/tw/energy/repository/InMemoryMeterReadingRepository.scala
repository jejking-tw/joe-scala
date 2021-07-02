package com.tw.energy.repository

import cats.effect.kernel.Sync
import com.tw.energy.domain.{ElectricityReading, MeterReadings}
import com.tw.energy.domain.StringTypes.SmartMeterId

import scala.collection.mutable

class InMemoryMeterReadingRepository(inputMap: Map[SmartMeterId, Seq[ElectricityReading]] = Map()) extends MeterReadingRepository {

  private val map = new mutable.HashMap[SmartMeterId, Seq[ElectricityReading]]() ++ inputMap

  override def getReadings[F[_] : Sync](smartMeterId: SmartMeterId): F[Option[Seq[ElectricityReading]]] = {
    Sync[F].delay {
      map.get(smartMeterId)
    }
  }

  override def storeReadings[F[_] : Sync](meterReadings: MeterReadings): F[Unit] = {
    Sync[F].delay {
      val updatedListOfElectricityReadings = map.get(meterReadings.smartMeterId) match {
        case Some(existingReadings: Seq[ElectricityReading]) => existingReadings ++  meterReadings.electricityReadings
        case None => meterReadings.electricityReadings
      }
      map.put(meterReadings.smartMeterId, updatedListOfElectricityReadings)
    }
  }
}