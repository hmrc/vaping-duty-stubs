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
    ) {

  private def byVpdIdAndPeriodKey(vpdId: String, periodKey: String) =
    Filters.and(
      Filters.equal("vpdId", vpdId),
      Filters.equal("periodKey", periodKey)
    )

  def get(vpdId: String, periodKey: String): Future[Option[ReturnSubmission]] =
    collection
      .find(byVpdIdAndPeriodKey(vpdId, periodKey))
      .headOption()

  def set(submission: ReturnSubmission): Future[ReturnSubmission] =
    collection
      .replaceOne(
        filter = byVpdIdAndPeriodKey(submission.vpdId, submission.periodKey),
        replacement = submission,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => submission)

  def clear: Future[Option[Unit]] =
    collection.drop().headOption()
}