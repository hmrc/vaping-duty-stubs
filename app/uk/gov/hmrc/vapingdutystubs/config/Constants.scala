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

package uk.gov.hmrc.vapingdutystubs.config

object Constants {

  object Headers {
    val correlationIdHeader: String       = "correlationid"
    val xOriginatingSystemHeader: String  = "X-Originating-System"
    val xReceiptDateHeader: String        = "X-Receipt-Date"
    val xTransmittingSystemHeader: String = "X-Transmitting-System"
    val xZAD: String                      = "X-ZAD"
  }

  val ukTimeZoneStringId = "Europe/London"
}
