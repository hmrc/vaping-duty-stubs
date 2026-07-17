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

  val regimeInvalid               = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "001", "REGIME missing or invalid")
  )

  val requestCouldNotBeProcessed  = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "003", "Request could not be processed")
  )

  val idTypeInvalid               = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "011", "ID_TYPE missing or invalid")
  )

  val idValueInvalid              = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "012", "ID_VALUE missing or invalid")
  )

  val emailVerificationMissing    = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "013", "Email Verification missing")
  )

  val emailAddressInvalid         = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "014", "Email Address missing or invalid")
  )

  val previousAmendmentInProgress = UnprocessableEntityError(
    DownstreamErrorsDetails(Instant.now(clock), "015", "Previous Amendment is in progress")
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
