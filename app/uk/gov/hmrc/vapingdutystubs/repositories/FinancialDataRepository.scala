/*
 * Copyright 2026 HM Revenue & Customs
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
import uk.gov.hmrc.vapingdutystubs.models.financialdata.FinancialDataState

import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class FinancialDataRepository @Inject()(
  mongoComponent: MongoComponent,
  config: AppConfig
)(implicit ec: ExecutionContext)
    extends PlayMongoRepository[FinancialDataState](
      collectionName = "financial-data",
      mongoComponent = mongoComponent,
      domainFormat = FinancialDataState.format,
      indexes = Seq(
        IndexModel(
          Indexes.ascending("_id"),
          IndexOptions()
            .name("vpdIdIdx")
        ),
        IndexModel(
          Indexes.ascending("lastUpdated"),
          IndexOptions()
            .name("lastUpdatedIdx")
            .expireAfter(config.financialDataTTL, TimeUnit.SECONDS)
        ),
      ),
      extraCodecs = Seq.empty,
      replaceIndexes = true
    ) {

  private def byId(vpdId: String) = Filters.equal("_id", vpdId)

  def get(vpdId: String): Future[Option[FinancialDataState]] =
    collection
      .find(byId(vpdId))
      .headOption()

  def set(state: FinancialDataState): Future[FinancialDataState] =
    collection
      .replaceOne(
        filter = byId(state.vpdId),
        replacement = state,
        options = ReplaceOptions().upsert(true)
      )
      .toFuture()
      .map(_ => state)

  def clear: Future[Option[Unit]] =
    collection.drop().headOption()
}
