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

import play.api.http.Status.{GATEWAY_TIMEOUT, UNSUPPORTED_MEDIA_TYPE}
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator

import java.time.{Clock, Instant}
import javax.inject.Inject

class ErrorData @Inject()(uuidGenerator: RandomUUIDGenerator, clock: Clock) {
  val badRequest          = DownstreamError(
    DownstreamErrorDetails(
      "400",
      "Error between computer and chair: _You_ sent a bad request",
      uuidGenerator.uuidHyphenTrimmed.toUpperCase()
    )
  )
  val internalServerError = DownstreamError(
    DownstreamErrorDetails("500", "Computer says No!", uuidGenerator.uuidHyphenTrimmed.toUpperCase())
  )

  val unprocessableEntity = DownstreamError(
    DownstreamErrorDetails("422", "Invalid regime or idType", uuidGenerator.uuidHyphenTrimmed.toUpperCase())
  )

  val etmpUnprocessableEntity = EtmpDownstreamError(
    EtmpDownstreamErrorDetails("422", "Email Verification missing", "2022-01-31T09:26:17Z")
  )

  val duplicateSubmission044 = DuplicateSubmissionError(
    DownstreamErrorsDetails(Instant.now(clock), "044", "Tax Obligation Already Fulfilled")
  )

  val duplicateSubmission999 = DuplicateSubmissionError(
    DownstreamErrorsDetails(Instant.now(clock), "999", " ")
  )

  val gatewayTimeout: DownstreamError = DownstreamError(
    DownstreamErrorDetails(GATEWAY_TIMEOUT.toString, "Gateway Timeout", uuidGenerator.uuidHyphenTrimmed.toUpperCase())
  )

  val unsupportedMediaType = DownstreamError(
    DownstreamErrorDetails(UNSUPPORTED_MEDIA_TYPE.toString, "Unsupported media type", uuidGenerator.uuidHyphenTrimmed.toUpperCase())
  )
}
