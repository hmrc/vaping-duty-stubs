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

package uk.gov.hmrc.vapingdutystubs.controllers.contactPreference

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

class EmailVerificationControllerSpec extends SpecBase {
  val mockEmailVerificationStateRepo: EmailVerificationStateRepository = mock[EmailVerificationStateRepository]

  val controller = new EmailVerificationController(cc, mockEmailVerificationStateRepo)

  override def beforeEach(): Unit = {
    super.beforeEach()
    reset(mockEmailVerificationStateRepo)
  }

  "getVerifiedEmails must" - {
    "return 200 OK with JSON containing a list of email verification statuses when the credId ends in '0'" in {

      println(Json.toJson(emailVerificationStatuses.toList))
      val result: Future[Result] = controller.getVerifiedEmails("TESTCREDID00000")(fakeRequest)

      status(result)        mustBe OK
      contentAsJson(result) mustBe Json.toJson(EmailVerificationStatus.fixedScenarios)
    }

    "when the credId ends in '1'" - {
      val credId = "TESTCREDID00001"

      "return 404 NOT_FOUND if the credId does not exist in the cache" in {
        val stateDataToCache = EmailVerificationState(credId, returnAllUnverified = false)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(None))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result) mustBe NOT_FOUND

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }

      "return 404 NOT_FOUND if returnAllUnverified=true in the cache" in {
        val stateDataFromCache = EmailVerificationState(credId, returnAllUnverified = true)
        val stateDataToCache   = EmailVerificationState(credId, returnAllUnverified = false)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(Some(stateDataFromCache)))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result) mustBe NOT_FOUND

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }

      "return 200 OK with a fixed response if returnAllUnverified=false in the cache" in {
        val stateDataFromCache = EmailVerificationState(credId, returnAllUnverified = false)
        val stateDataToCache   = EmailVerificationState(credId, returnAllUnverified = true)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(Some(stateDataFromCache)))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(EmailVerificationStatus.fixedScenarios)

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }
    }

    "when the credId ends in '2'" - {
      val credId = "TESTCREDID00002"

      "return 200 OK with only unverified verification statuses if the credId does not exist in the cache" in {
        val stateDataToCache = EmailVerificationState(credId, returnAllUnverified = false)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(None))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(EmailVerificationStatus.fixedScenariosAllUnverified)

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }

      "return 200 OK with only unverified verification statuses if returnAllUnverified=true in the cache" in {
        val stateDataFromCache = EmailVerificationState(credId, returnAllUnverified = true)
        val stateDataToCache   = EmailVerificationState(credId, returnAllUnverified = false)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(Some(stateDataFromCache)))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(EmailVerificationStatus.fixedScenariosAllUnverified)

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }

      "return 200 OK with a fixed response if returnAllUnverified=false in the cache" in {
        val stateDataFromCache = EmailVerificationState(credId, returnAllUnverified = false)
        val stateDataToCache   = EmailVerificationState(credId, returnAllUnverified = true)
        when(mockEmailVerificationStateRepo.get(eqTo(credId))).thenReturn(Future.successful(Some(stateDataFromCache)))
        when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))

        val result: Future[Result] = controller.getVerifiedEmails(credId)(fakeRequest)

        status(result)        mustBe OK
        contentAsJson(result) mustBe Json.toJson(EmailVerificationStatus.fixedScenarios)

        verify(mockEmailVerificationStateRepo, times(1)).get(eqTo(credId))
        verify(mockEmailVerificationStateRepo, times(1)).set(eqTo(stateDataToCache))
      }
    }

    "return 400 BAD_REQUEST when the credId ends in '8'" in {
      val result: Future[Result] = controller.getVerifiedEmails("TESTCREDID00008")(fakeRequest)

      status(result)        mustBe BAD_REQUEST
      contentAsJson(result) mustBe badRequestJson
    }

    "return 500 INTERNAL_SERVER_ERROR when the credId ends in '9'" in {
      val result: Future[Result] = controller.getVerifiedEmails("TESTCREDID00009")(fakeRequest)

      status(result)        mustBe INTERNAL_SERVER_ERROR
      contentAsJson(result) mustBe internalServerErrorJson
    }
  }

  "clearStatData must" - {
    "remove email verification data" in {
//      val stateDataToCache = EmailVerificationState("TESTCREDID00009", returnAllUnverified = false)
//      val stateDataFromCache = EmailVerificationState("TESTCREDID00009", returnAllUnverified = false)
//      when(mockEmailVerificationStateRepo.get(eqTo("TESTCREDID00009"))).thenReturn(Future.successful(Some(stateDataFromCache)))
//      when(mockEmailVerificationStateRepo.set(eqTo(stateDataToCache))).thenReturn(Future.successful(stateDataToCache))
      when(mockEmailVerificationStateRepo.clear).thenReturn(Future.successful(Some(())))
      controller.getVerifiedEmails("TESTCREDID00009")
      val result = controller.clearStateData()(fakeRequest)

      status(result) mustBe OK
    }
  }
}
