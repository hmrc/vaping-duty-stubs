/*
 * Copyright 2025 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.vapingdutystubs.models.returns

import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.should.Matchers
import play.api.libs.json.{JsSuccess, Json}

class VapingReturnSpec extends AnyFreeSpec with Matchers {

  "VapingReturn" - {
    "must serialize to JSON correctly" in {
      val vapingReturn = VapingReturn(
        taxType = "641",
        dutyRate = BigDecimal("10.50"),
        amountProducedLiquid = BigDecimal("1500.25"),
        dutyDue = BigDecimal("15752.63")
      )

      val expectedJson = Json.obj(
        "taxType" -> "641",
        "dutyRate" -> 10.50,
        "amountProducedLiquid" -> 1500.25,
        "dutyDue" -> 15752.63
      )

      Json.toJson(vapingReturn) shouldBe expectedJson
    }

    "must deserialize from JSON correctly" in {
      val json = Json.obj(
        "taxType" -> "641",
        "dutyRate" -> 10.50,
        "amountProducedLiquid" -> 1500.25,
        "dutyDue" -> 15752.63
      )

      val expectedReturn = VapingReturn(
        taxType = "641",
        dutyRate = BigDecimal("10.50"),
        amountProducedLiquid = BigDecimal("1500.25"),
        dutyDue = BigDecimal("15752.63")
      )

      json.validate[VapingReturn] shouldBe JsSuccess(expectedReturn)
    }
  }
}
