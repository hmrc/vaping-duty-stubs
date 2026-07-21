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

import play.api.libs.json.{Json, OFormat}
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest

import java.time.Instant

case class ReturnSubmission(
  vpdId: String,
  periodKey: String,
  chargeReference: Option[String],
  submittedReturn: ReturnCreateRequest,
  submittedAt: Instant,
  submissionId: String
)

object ReturnSubmission {
  given format: OFormat[ReturnSubmission] = Json.format[ReturnSubmission]
}