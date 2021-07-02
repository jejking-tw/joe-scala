package com.tw.energy

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import com.tw.energy.controller.{MeterReadingController, PricePlanComparatorController}
import com.tw.energy.repository.FileMeterReadingRepository
import com.tw.energy.service.{AccountService, MeterReadingService, PricePlanService}

import java.nio.file.{Files, Paths}


class JOIEnergyApplication {
  val meterReadingRepository = new FileMeterReadingRepository(Files.createTempDirectory("joe-repo"))
  val meterReadingService = new MeterReadingService(meterReadingRepository)
  val meterReadingController = new MeterReadingController(meterReadingService)

  val accountService = new AccountService(Configuration.smartMeterToPricePlanAccounts)
  val pricePlanService = new PricePlanService(Configuration.pricePlans, meterReadingService)
  val pricePlanComparatorController = new PricePlanComparatorController(pricePlanService, accountService)

  val routes: Route = meterReadingController.routes ~ pricePlanComparatorController.routes
}

object WebApp extends App {
  val appplication = new JOIEnergyApplication()
  new WebServer(appplication.routes).start().stopOnReturn()
}

