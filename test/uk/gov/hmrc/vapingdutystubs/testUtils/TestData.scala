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

package uk.gov.hmrc.vapingdutystubs.testUtils

import uk.gov.hmrc.vapingdutystubs.config.Constants.Headers.correlationIdHeader
import uk.gov.hmrc.vapingdutystubs.models.ErrorData
import uk.gov.hmrc.vapingdutystubs.models.contactPreference.*
import uk.gov.hmrc.vapingdutystubs.models.subscription.{CorrespondenceAddress, EmailPreferences}
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator

import java.time.{Clock, Instant, ZoneId}

trait TestData extends ModelGenerators {
  val clock = Clock.fixed(Instant.ofEpochMilli(1718118467838L), ZoneId.of("UTC"))

  val zeroAmount = BigDecimal(0)

  val regime = "VPD"
  val idType = "ZVPD"

  val uuidGenerator: RandomUUIDGenerator

  val dummyUUID = "01234567-89ab-cdef-0123-456789abcdef"

  val vpdId                   = vpdIdGen.sample.get
  val submissionId: String    = submissionIdGen().sample.get
  val chargeReference: String = chargeReferenceGen().sample.get

  def submitCorrelationIdHeader(): Seq[(String, String)] = Seq((correlationIdHeader, dummyUUID))

  val responseHeadersWithCorrelationId = Map(correlationIdHeader -> dummyUUID)

  lazy val errorData = new ErrorData(uuidGenerator, clock)

  // Start of ECP test data
  val standardEmailAddress     = "john.doe@example.com"
  val standardEmailPreferences = EmailPreferences(true, Some(standardEmailAddress), Some(true), Some(false))

  val ukCorrespondenceAddress    = CorrespondenceAddress(
    addressLine1 = Some("Flat 123"),
    addressLine2 = Some("1 Example Road"),
    postCode = Some("AB1 2CD")
  )
  val overseasAddress1           = CorrespondenceAddress(
    addressLine1 = Some("Flat 123"),
    addressLine2 = Some("1 Example Road"),
    addressLine3 = Some("Toronto"),
    postCode = Some("P55555")
  )
  val overseasAddress2           = CorrespondenceAddress(
    addressLine1 = Some("1 Example Road"),
    addressLine2 = Some("Barcelona"),
    postCode = Some("P66666")
  )
  val unrecognisedCountryAddress = CorrespondenceAddress(
    addressLine1 = Some("Flat 123"),
    addressLine2 = Some("1 Example Road"),
    addressLine3 = Some("District A"),
    postCode = None
  )
  val noCountryAddress           = CorrespondenceAddress(
    addressLine1 = Some("Building 1"),
    postCode = Some("P88888")
  )

  val paperlessPreference                    = PaperlessPreferenceSubmission(true, Some(standardEmailAddress), Some(true), Some(false))
  val paperPreference                        = PaperlessPreferenceSubmission(false, None, None, None)
  val paperlessPreferenceEmailNoVerification =
    PaperlessPreferenceSubmission(true, Some(standardEmailAddress), None, Some(false))

  val emailVerificationStatuses = Seq(
    GetVerificationStatusResponseEmailAddressDetails(
      emailAddress = "john.doe@example.com",
      verified = true,
      locked = false
    ),
    GetVerificationStatusResponseEmailAddressDetails(
      emailAddress = "jane.doe@example.com",
      verified = false,
      locked = true
    ),
    GetVerificationStatusResponseEmailAddressDetails(
      emailAddress = "john.doe2@example.com",
      verified = false,
      locked = false
    ),
    GetVerificationStatusResponseEmailAddressDetails(
      emailAddress = "jane.doe2@example.com",
      verified = true,
      locked = true
    )
  )
}
