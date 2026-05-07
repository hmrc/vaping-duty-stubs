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
import play.api.inject.ApplicationLifecycle
import uk.gov.hmrc.vapingdutystubs.data.obligations.ObligationsData
import uk.gov.hmrc.vapingdutystubs.data.returns.ReturnSubmissionData
import uk.gov.hmrc.vapingdutystubs.repositories.{ObligationsRepository, ReturnSubmissionRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartupSeeder @Inject()(
  returnSubmissionRepository: ReturnSubmissionRepository,
  obligationsRepository: ObligationsRepository,
  lifecycle: ApplicationLifecycle
)(implicit ec: ExecutionContext) extends Logging {

  // Seed data on startup
  seedData()

  private def seedData(): Unit = {
    logger.info("Seeding sample data into repositories...")

    val seedFuture = for {
      // Seed return submissions
      _ <- Future.sequence(
        ReturnSubmissionData.allSampleReturnSubmissions.map { submission =>
          returnSubmissionRepository.set(submission)
        }
      )
      // Seed obligations
      _ <- Future.sequence(
        ObligationsData.allSampleObligations.map { obligations =>
          obligationsRepository.set(obligations)
        }
      )
    } yield ()

    seedFuture.onComplete {
      case scala.util.Success(_) =>
        logger.info("Successfully seeded sample data")
      case scala.util.Failure(ex) =>
        logger.error("Failed to seed sample data", ex)
    }
  }

  // Clean up on shutdown
  lifecycle.addStopHook { () =>
    logger.info("Application shutting down")
    Future.successful(())
  }
}