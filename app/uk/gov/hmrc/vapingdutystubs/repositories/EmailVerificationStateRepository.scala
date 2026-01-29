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
import uk.gov.hmrc.vapingdutystubs.models.contactPreference.EmailVerificationState

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EmailVerificationStateRepository @Inject()(
  mongoComponent: MongoComponent
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[EmailVerificationState](
      collectionName = "email-verification-state",
      mongoComponent = mongoComponent,
      domainFormat = EmailVerificationState.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("credId"),
          IndexOptions().name("credIdIdx")
        )
      ),
      extraCodecs = Seq.empty,
      replaceIndexes = true
    ) {

  private def byId(credId: String) = Filters.equal("credId", credId)

  def get(credId: String): Future[Option[EmailVerificationState]] =
    collection
      .find(byId(credId))
      .headOption()

  def set(stateData: EmailVerificationState): Future[EmailVerificationState] =
    collection
      .replaceOne(
        filter = byId(stateData.credId),
        replacement = stateData,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => stateData)
  
  def clear: Future[Option[Unit]] =
    collection.drop().headOption()
}
