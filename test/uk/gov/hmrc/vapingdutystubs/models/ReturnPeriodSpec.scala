/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutystubs.models

import play.api.libs.json.{JsString, Json}
import uk.gov.hmrc.vapingdutystubs.base.SpecBase

import java.time.{LocalDate, YearMonth}

class ReturnPeriodSpec extends SpecBase {
  "ReturnPeriod when" - {
    "converting from a period key must" - {
      "return an error when" - {
        "the key is more than 4 characters" in {
          ReturnPeriod.fromPeriodKey("24AC1") mustBe None
        }

        "the key is less than 4 characters" in {
          ReturnPeriod.fromPeriodKey("24A") mustBe None
        }

        "the key is empty" in {
          ReturnPeriod.fromPeriodKey("") mustBe None
        }

        "the first character is not a digit" in {
          ReturnPeriod.fromPeriodKey("/4AC") mustBe None
          ReturnPeriod.fromPeriodKey(":4AC") mustBe None
          ReturnPeriod.fromPeriodKey("A4AC") mustBe None
        }

        "the second character is not a digit" in {
          ReturnPeriod.fromPeriodKey("2/AC") mustBe None
          ReturnPeriod.fromPeriodKey("2:AC") mustBe None
          ReturnPeriod.fromPeriodKey("2AAC") mustBe None
        }

        "the third character is not an A" in {
          ReturnPeriod.fromPeriodKey("24BC") mustBe None
          ReturnPeriod.fromPeriodKey("244C") mustBe None
          ReturnPeriod.fromPeriodKey("24aC") mustBe None
        }

        "the fourth character is not a A-L" in {
          ReturnPeriod.fromPeriodKey("24A@") mustBe None
          ReturnPeriod.fromPeriodKey("24AM") mustBe None
          ReturnPeriod.fromPeriodKey("24Aa") mustBe None
          ReturnPeriod.fromPeriodKey("24A9") mustBe None
        }
      }

      "throw an exception instead if fromPeriodKeyOrThrow is called if an error" in {
        an[IllegalArgumentException] mustBe thrownBy(ReturnPeriod.fromPeriodKeyOrThrow("24A@"))
      }

      "return a correct ReturnPeriod when" - {
        "a valid period key is passed" in {
          ReturnPeriod.fromPeriodKey("24AA") mustBe Some(ReturnPeriod(YearMonth.of(2024, 1)))
          ReturnPeriod.fromPeriodKey("24AL") mustBe Some(ReturnPeriod(YearMonth.of(2024, 12)))
          ReturnPeriod.fromPeriodKey("28AC") mustBe Some(ReturnPeriod(YearMonth.of(2028, 3)))
        }
      }
    }

    "convert a date to a ReturnPeriod" in {
      ReturnPeriod.fromDateInPeriod(LocalDate.of(2024, 1, 13)).toPeriodKey mustBe "24AA"
    }

    "parse ReturnPeriod with the right json value" in {
      val returnPeriod = ReturnPeriod(YearMonth.of(2024, 1))
      val result       = Json.toJson(returnPeriod)
      result mustBe JsString("24AA")
    }

    "transform a valid Period Key json string into a Return Period" in {
      val periodKey         = periodKeyGen.sample.get
      val periodKeyJsString = JsString(periodKey)
      val result            = periodKeyJsString.as[ReturnPeriod]
      result.toPeriodKey mustBe periodKey
    }

    "throw an IllegalArgumentException when an invalid period key json string is parsed" in {
      val invalidPeriodKey  = invalidPeriodKeyGen.sample.get
      val periodKeyJsString = JsString(invalidPeriodKey)

      val exception = intercept[IllegalArgumentException](
        periodKeyJsString.as[ReturnPeriod]
      )

      exception mustBe an[IllegalArgumentException]
    }

    "getting the due date must" - {
      "return the 25th of the month after" in {
        val returnPeriod = ReturnPeriod(YearMonth.of(2024, 1))
        returnPeriod.dueDate() mustBe LocalDate.of(2024, 2, 25)
      }
    }

    "getting the period from date must" - {
      "return the 1st of the month" in {
        val returnPeriod = ReturnPeriod(YearMonth.of(2024, 1))
        returnPeriod.periodFromDate() mustBe LocalDate.of(2024, 1, 1)
      }
    }

    "getting the period to date must" - {
      "return the last date of the month" in {
        val returnPeriod = ReturnPeriod(YearMonth.of(2024, 1))
        returnPeriod.periodToDate() mustBe LocalDate.of(2024, 1, 31)
      }
    }

    "getting the period using today's date if this month must" - {
      "return using today's day if this month" in {
        val returnPeriod = ReturnPeriod(YearMonth.now(clock))
        returnPeriod.withTodaysDayIfThisMonth(clock) mustBe LocalDate.now(clock)
      }

      "use the first day if not this month" in {
        val returnPeriod = ReturnPeriod(YearMonth.of(2024, 1))
        returnPeriod.withTodaysDayIfThisMonth(clock) mustBe LocalDate.of(2024, 1, 1)
      }
    }
  }
}
