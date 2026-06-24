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

package uk.gov.hmrc.vapingdutystubs.repositories

import org.mongodb.scala.model.*
import play.api.Logging
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.vapingdutystubs.config.AppConfig
import uk.gov.hmrc.vapingdutystubs.models.returns.ReturnSubmission

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnSubmissionRepository @Inject()(
  mongoComponent: MongoComponent,
  config: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[ReturnSubmission](
      collectionName = "return-submissions",
      mongoComponent = mongoComponent,
      domainFormat = ReturnSubmission.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("vpdId", "periodKey"),
          IndexOptions()
            .name("vpdIdPeriodKeyIdx")
            .unique(true)
        ),
        IndexModel(
          Indexes.ascending("submittedAt"),
          IndexOptions()
            .name("submittedAtIdx")
            .expireAfter(config.returnSubmissionTTL, TimeUnit.SECONDS)
        )
      ),
      extraCodecs = Seq.empty,
      replaceIndexes = true
    ) with Logging {

  private def byVpdIdAndPeriodKey(vpdId: String, periodKey: String) =
    Filters.and(
      Filters.equal("vpdId", vpdId),
      Filters.equal("periodKey", periodKey)
    )

  def get(vpdId: String, periodKey: String): Future[Option[ReturnSubmission]] = {
    logger.debug(s"[ReturnSubmissionRepository.get] Querying for vpdId: $vpdId, periodKey: $periodKey")
    
    collection
      .find(byVpdIdAndPeriodKey(vpdId, periodKey))
      .headOption()
      .map { result =>
        result match {
          case Some(submission) =>
            logger.info(s"[ReturnSubmissionRepository.get] Found submission for vpdId: $vpdId, periodKey: $periodKey, chargeRef: ${submission.chargeReference}")
            logger.debug(s"[ReturnSubmissionRepository.get] Submission details - submissionId: ${submission.submissionId}, submittedAt: ${submission.submittedAt}")
          case None =>
            logger.info(s"[ReturnSubmissionRepository.get] No submission found for vpdId: $vpdId, periodKey: $periodKey")
        }
        result
      }
  }

  def set(submission: ReturnSubmission): Future[ReturnSubmission] = {
    logger.info(s"[ReturnSubmissionRepository.set] Saving submission for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}")
    logger.debug(s"[ReturnSubmissionRepository.set] Submission details - chargeRef: ${submission.chargeReference}, submissionId: ${submission.submissionId}, submittedAt: ${submission.submittedAt}")
    
    collection
      .replaceOne(
        filter = byVpdIdAndPeriodKey(submission.vpdId, submission.periodKey),
        replacement = submission,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map { result =>
        logger.info(s"[ReturnSubmissionRepository.set] Successfully saved submission for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}, matched: ${result.getMatchedCount}, modified: ${result.getModifiedCount}, upserted: ${Option(result.getUpsertedId).isDefined}")
        submission
      }
      .recover { case ex =>
        logger.error(s"[ReturnSubmissionRepository.set] Failed to save submission for vpdId: ${submission.vpdId}, periodKey: ${submission.periodKey}", ex)
        throw ex
      }
  }

  def getAll(vpdId: String): Future[Seq[ReturnSubmission]] = {
    logger.debug(s"[ReturnSubmissionRepository.getAll] Querying all submissions for vpdId: $vpdId")
    
    collection
      .find(Filters.equal("vpdId", vpdId))
      .toFuture()
      .map { submissions =>
        logger.info(s"[ReturnSubmissionRepository.getAll] Found ${submissions.size} submissions for vpdId: $vpdId")
        submissions
      }
  }

  def delete(vpdId: String, periodKey: String): Future[Boolean] = {
    logger.info(s"[ReturnSubmissionRepository.delete] Deleting submission for vpdId: $vpdId, periodKey: $periodKey")
    
    collection
      .deleteOne(byVpdIdAndPeriodKey(vpdId, periodKey))
      .toFuture()
      .map { result =>
        val deleted = result.getDeletedCount > 0
        logger.info(s"[ReturnSubmissionRepository.delete] Deletion result for vpdId: $vpdId, periodKey: $periodKey - deleted: $deleted")
        deleted
      }
  }

  def clear: Future[Option[Unit]] = {
    logger.warn(s"[ReturnSubmissionRepository.clear] Clearing all return submissions from collection")
    
    collection.drop().headOption().map { result =>
      logger.info(s"[ReturnSubmissionRepository.clear] Successfully cleared all return submissions")
      result
    }
  }
}
