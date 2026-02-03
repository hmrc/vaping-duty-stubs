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

import uk.gov.hmrc.vapingdutystubs.models.contactPreference.{GetVerificationStatusResponse, GetVerificationStatusResponseEmailAddressDetails}

object EmailVerificationStatus {
  val fixedScenarios: GetVerificationStatusResponse = GetVerificationStatusResponse(
    List(
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "john.doe@example.com",
        verified = true,
        locked = false
      ),
      // duplicate emails are possible
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "john.doe@example.com",
        verified = false,
        locked = false
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "jane.doe@example.com",
        verified = false,
        locked = true
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "jane.doe2@example.com",
        verified = true,
        locked = true
      )
    )
  )

  val fixedScenariosAllUnverified: GetVerificationStatusResponse = GetVerificationStatusResponse(
    List(
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "john.doe@example.com",
        verified = false,
        locked = false
      ),
      GetVerificationStatusResponseEmailAddressDetails(
        emailAddress = "jane.doe@example.com",
        verified = false,
        locked = false
      )
    )
  )
}
