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

import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController
import uk.gov.hmrc.vapingdutystubs.data.emailverification.EmailVerificationErrors.*
import uk.gov.hmrc.vapingdutystubs.data.emailverification.EmailVerificationStatus
import uk.gov.hmrc.vapingdutystubs.models.contactPreference.EmailVerificationState
import uk.gov.hmrc.vapingdutystubs.repositories.EmailVerificationStateRepository

import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class EmailVerificationController @Inject()(
  cc: ControllerComponents,
  emailVerificationStateRepository: EmailVerificationStateRepository
)(implicit ec: ExecutionContext)
    extends BackendController(cc) {

  def getVerifiedEmails(credId: String): Action[AnyContent] = Action.async { _ =>
    credId.takeRight(1) match {
      case "0" => Future.successful(Ok(Json.toJson(EmailVerificationStatus.fixedScenarios)))
      case "8" => Future.successful(BadRequest(badRequestJson))
      case "9" => Future.successful(InternalServerError(internalServerErrorJson))
      case "1" =>
        // alternate between NotFound and fixedScenarios
        emailVerificationStateRepository.get(credId).flatMap {
          case None            =>
            emailVerificationStateRepository.set(EmailVerificationState(credId, returnAllUnverified = false)).map { _ =>
              NotFound
            }
          case Some(stateData) =>
            emailVerificationStateRepository
              .set(stateData.copy(returnAllUnverified = !stateData.returnAllUnverified))
              .map { _ =>
                if (stateData.returnAllUnverified) NotFound else Ok(Json.toJson(EmailVerificationStatus.fixedScenarios))
              }
        }
      case _   =>
        // alternate between fixedScenariosAllUnverified and fixedScenarios
        emailVerificationStateRepository.get(credId).flatMap {
          case None            =>
            emailVerificationStateRepository.set(EmailVerificationState(credId, returnAllUnverified = false)).map { _ =>
              Ok(Json.toJson(EmailVerificationStatus.fixedScenariosAllUnverified))
            }
          case Some(stateData) =>
            emailVerificationStateRepository
              .set(stateData.copy(returnAllUnverified = !stateData.returnAllUnverified))
              .map { _ =>
                if (stateData.returnAllUnverified) {
                  Ok(Json.toJson(EmailVerificationStatus.fixedScenariosAllUnverified))
                } else { Ok(Json.toJson(EmailVerificationStatus.fixedScenarios)) }
              }
        }
    }
  }

  def clearStateData: Action[AnyContent] = Action.async { _ =>
    for {
      _ <- emailVerificationStateRepository.clear
    } yield Ok("All email verification state data cleared")
  }
}
