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

package uk.gov.hmrc.vapingdutystubs.services.returns

import play.api.Logging
import uk.gov.hmrc.vapingdutystubs.data.financialdata.FinancialDataStubData
import uk.gov.hmrc.vapingdutystubs.models.financialdata.FinancialDataState
import uk.gov.hmrc.vapingdutystubs.models.returns.ReturnSubmission
import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.repositories.{FinancialDataRepository, ObligationsRepository, ReturnSubmissionRepository}
import uk.gov.hmrc.vapingdutystubs.utils.{ChargeReferenceGenerator, RandomUUIDGenerator}

import java.time.{Clock, Instant}
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmissionService @Inject()(
                                         returnSubmissionRepository: ReturnSubmissionRepository,
                                         obligationsRepository: ObligationsRepository,
                                         financialDataRepository: FinancialDataRepository,
                                         chargeReferenceGenerator: ChargeReferenceGenerator,
                                         uuidGenerator: RandomUUIDGenerator,
                                         clock: Clock
                                       )(implicit ec: ExecutionContext) extends Logging {

  /**
   * Processes a return submission request.
   *
   * @param vpdId         The VPD ID from the request header
   * @param returnRequest The validated return create request
   * @return Future[Either[String, ReturnSubmission]] - Left for validation errors, Right for successful submission
   */
  def processSubmission(vpdId: String, returnRequest: ReturnCreateRequest): Future[Either[String, ReturnSubmission]] = {
    logger.info(s"[ReturnSubmissionService] Processing submission for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")
    logger.debug(s"[ReturnSubmissionService] Validating return request for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")

    // Validate the request
    returnRequest.validate match {
      case Left(validationError) =>
        logger.warn(s"[ReturnSubmissionService] Business validation failed for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}, error: $validationError")
        Future.successful(Left(validationError))

      case Right(_) =>
        logger.info(s"[ReturnSubmissionService] Validation passed for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")

        val now = Instant.now(clock)
        val submissionId = uuidGenerator.uuid
        val chargeReference = chargeReferenceGenerator.generate(returnRequest.totalDutyDue.totalDue)

        if (chargeReference.isDefined) {
          logger.debug(s"[ReturnSubmissionService] Generated submissionId: $submissionId, chargeReference: ${chargeReference.get}")
        } else {
          logger.debug(s"[ReturnSubmissionService] Generated submissionId: $submissionId, no chargeReference (totalDue is zero)")
        }

        val submission = ReturnSubmission(
          vpdId = vpdId,
          periodKey = returnRequest.periodKey,
          chargeReference = chargeReference,
          submittedReturn = returnRequest,
          submittedAt = now,
          submissionId = submissionId
        )

        logger.info(s"[ReturnSubmissionService] Saving submission to repository for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")

        for {
          _ <- returnSubmissionRepository.set(submission)
          _ = logger.info(s"[ReturnSubmissionService] Successfully saved submission for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}, chargeRef: $chargeReference")
          _ = logger.info(s"[ReturnSubmissionService] Marking obligation as fulfilled for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")
          obligationResult <- obligationsRepository.markAsFulfilled(vpdId, returnRequest.periodKey, now)
          _ = logger.info(s"[ReturnSubmissionService] Obligation marked as fulfilled: ${obligationResult.isDefined} for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}")
          _ <- generateFinancialData(submission)
        } yield {
          logger.info(s"[ReturnSubmissionService] Successfully completed submission for vpdId: $vpdId, periodKey: ${returnRequest.periodKey}, submissionId: $submissionId")
          Right(submission)
        }
    }
  }

  /**
   * Generates financial data for a return submission if a charge reference exists.
   *
   * @param submission The return submission
   * @return Future[Unit]
   */
  private def generateFinancialData(submission: ReturnSubmission): Future[Unit] = {
    submission.chargeReference match {
      case Some(chargeRef) =>
        logger.info(s"[ReturnSubmissionService] Generating financial data for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}, chargeRef: $chargeRef")

        financialDataRepository.get(submission.vpdId).flatMap { existingStateOpt =>
          val newDocument = FinancialDataStubData
            .fromReturnSubmission(submission)
            .documentDetails
            .head

          val updatedState = existingStateOpt match {
            case Some(existingState) =>
              logger.info(s"[ReturnSubmissionService] Appending to existing financial data for vpdId: ${submission.vpdId} (${existingState.documentDetails.size} existing documents)")
              existingState.copy(
                noDataIdentified = false,
                documentDetails = existingState.documentDetails :+ newDocument,
                lastUpdated = Instant.now(clock)
              )
            case None =>
              logger.info(s"[ReturnSubmissionService] Creating new financial data for vpdId: ${submission.vpdId}")
              FinancialDataState(
                vpdId = submission.vpdId,
                noDataIdentified = false,
                documentDetails = Seq(newDocument),
                lastUpdated = Instant.now(clock)
              )
          }

          financialDataRepository.set(updatedState).map { _ =>
            logger.info(s"[ReturnSubmissionService] Successfully generated financial data for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}")
          }
        }

      case None =>
        logger.info(s"[ReturnSubmissionService] Skipping financial data generation (nil return) for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}")
        Future.successful(())
    }
  }
}
