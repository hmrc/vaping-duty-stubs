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

package uk.gov.hmrc.vapingdutystubs.config

import play.api.Logging
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository, SubscriptionSummaryRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartupSeeder @Inject()(
  obligationsRepository: ObligationsRepository,
  returnSubmissionRepository: ReturnSubmissionRepository
)(implicit ec: ExecutionContext) extends Logging {

  import uk.gov.hmrc.vapingdutystubs.data.obligations.ObligationsData
  import uk.gov.hmrc.vapingdutystubs.data.returns.ReturnSubmissionData

  logger.info("StartupSeeder: Beginning initialization...")

  // Clear return submissions on startup to prevent deserialization issues with old data structure
  logger.info("Clearing return submissions repository on startup...")
  val clearFuture = returnSubmissionRepository.clear.map { _ =>
    logger.info("Return submissions repository cleared successfully")
  }.recover { case e =>
    logger.error(s"Failed to clear return submissions repository: ${e.getMessage}", e)
  }

  // Seed 36 months of obligations and returns for all sample VPD IDs
  val seedFuture = clearFuture.flatMap { _ =>
    logger.info("Seeding 36 months of obligations and returns for sample VPD IDs...")

    val obligationsFuture = Future.sequence(
      ObligationsData.all36MonthsObligations.map { obligationState =>
        obligationsRepository.set(obligationState).map { _ =>
          logger.info(s"Seeded ${obligationState.obligations.size} obligations for VPD ID: ${obligationState.vpdId}")
        }
      }
    )

    val returnsFuture = Future.sequence(
      ReturnSubmissionData.all36ReturnSubmissions.map { returnSubmission =>
        returnSubmissionRepository.set(returnSubmission).map { _ =>
          logger.info(s"Seeded return submission for VPD ID: ${returnSubmission.vpdId}, Period: ${returnSubmission.periodKey}")
        }
      }
    )

    Future.sequence(Seq(obligationsFuture, returnsFuture)).map { _ =>
      logger.info("Successfully seeded 36 months of obligations and returns for all sample VPD IDs")
    }.recover { case e =>
      logger.error(s"Failed to seed test  ${e.getMessage}", e)
    }
  }

  logger.info("StartupSeeder initialized")
}
