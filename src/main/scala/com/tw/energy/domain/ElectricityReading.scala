package com.tw.energy.domain

import io.circe._
import squants.energy.Power
import squants.market.{Money, MoneyContext, defaultMoneyContext}

import java.time.Instant
import scala.math.BigDecimal.RoundingMode

case class ElectricityReading(time: Instant, reading: Power)

trait SquantsJsonSupport {

  implicit val moneyContext: MoneyContext = defaultMoneyContext

  implicit val encodePower: Encoder[Power] = (power: Power) => Json.obj(
    ("amount", Json.fromDouble(power.value).getOrElse(Json.Null)),
    ("unit", Json.fromString(power.unit.symbol))
  )

  implicit val decodePower: Decoder[Power] = (c: HCursor) => for {
    amount <- c.downField("amount").as[Double]
    unit <- c.downField("unit").as[String]
    power <- Power((amount, unit)).fold(ex => Left(DecodingFailure.fromThrowable(ex, c.history)), power => Right(power))
  } yield {
    power
  }

  implicit val encodeMoney: Encoder[Money] = (money: Money) => Json.obj(
    ("amount", Json.fromBigDecimal(money.amount.underlying.stripTrailingZeros)),
    ("currency", Json.fromString(money.currency.code))
  )

  implicit val decodeMoney: Decoder[Money] = (c: HCursor) => for {
    amount <- c.downField("amount").as[BigDecimal]
    currency <- c.downField("currency").as[String]
    money <- Money(amount, currency).fold(ex => Left(DecodingFailure.fromThrowable(ex, c.history)), money => Right(money))
  } yield {
    money
  }

}
