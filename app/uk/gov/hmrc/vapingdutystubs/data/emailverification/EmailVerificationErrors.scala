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

package uk.gov.hmrc.vapingdutystubs.data.emailverification

import play.api.libs.json.Json

object EmailVerificationErrors {
  val badRequestJson = Json.obj(
    "code"    -> "VALIDATION_ERROR",
    "message" -> "Payload validation failed",
    "details" -> Json.obj(
      "obj.email" -> "error.path.missing"
    )
  )

  val internalServerErrorJson = Json.obj(
    "code"    -> "UNEXPECTED_ERROR",
    "message" -> "An unexpected error occurred"
  )

  val badGatewayJson = Json.obj(
    "code"    -> "UPSTREAM_ERROR",
    "message" -> "POST of 'http://localhost:11111/send-templated-email' returned 500. Response body: 'some-5xx-message'"
  )
}
