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

package uk.gov.hmrc.vapingdutystubs.controllers.returns

import org.mockito.ArgumentMatchers.eq as eqTo
import org.mockito.Mockito.{reset, times, verify, when}
import play.api.libs.json.Json
import play.api.mvc.Result
import uk.gov.hmrc.vapingdutystubs.base.SpecBase
import uk.gov.hmrc.vapingdutystubs.data.emailverification.EmailVerificationErrors.*
import uk.gov.hmrc.vapingdutystubs.data.emailverification.EmailVerificationStatus
import uk.gov.hmrc.vapingdutystubs.models.contactPreference.EmailVerificationState
import uk.gov.hmrc.vapingdutystubs.repositories.EmailVerificationStateRepository

import scala.concurrent.Future

class SubmitReturnControllerSpec extends SpecBase {

  val controller = new SubmitReturnController(cc)
  

  "submitReturn must" - {
    "return 201 CREATED with JSON containing a ReturnCreateResponse" in {

      val result: Future[Result] = controller.submitReturn()(
        fakeRequestWithJsonBody(Json.toJson(""))
          .withHeaders(submitCorrelationIdHeader(): _*)
      )

      status(result) mustBe CREATED
    }
  }
}
