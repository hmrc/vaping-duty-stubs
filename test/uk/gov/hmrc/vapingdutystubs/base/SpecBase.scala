/*
 * Copyright 2024 HM Revenue & Customs
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

package uk.gov.hmrc.vapingdutystubs.base

import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.concurrent.{IntegrationPatience, ScalaFutures}
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers
import org.scalatest.{BeforeAndAfterEach, OptionValues, TryValues}
import org.scalatestplus.play.guice.GuiceOneAppPerSuite
import play.api.Application
import play.api.http.{HeaderNames, Status}
import play.api.inject.bind
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.JsValue
import play.api.mvc.*
import play.api.test.Helpers.*
import play.api.test.{DefaultAwaitTimeout, FakeHeaders, FakeRequest, ResultExtractors}
import uk.gov.hmrc.vapingdutystubs.config.AppConfig
import uk.gov.hmrc.vapingdutystubs.data.DataGenerator
import uk.gov.hmrc.vapingdutystubs.testUtils.{ModelGenerators, TestData}
import uk.gov.hmrc.vapingdutystubs.utils.RandomUUIDGenerator
import uk.gov.hmrc.http.HeaderCarrier

import java.time.Clock
import scala.concurrent.ExecutionContext

trait SpecBase
    extends AnyFreeSpec
    with Matchers
    with TryValues
    with OptionValues
    with ScalaFutures
    with Results
    with DefaultAwaitTimeout
    with ResultExtractors
    with Status
    with HeaderNames
    with GuiceOneAppPerSuite
    with MockitoSugar
    with BeforeAndAfterEach
    with TestData
    with ModelGenerators
    with IntegrationPatience {

  def configOverrides: Map[String, Any] = Map()

  val additionalAppConfig: Map[String, Any] = Map(
    "metrics.enabled"              -> false,
    "auditing.enabled"             -> false,
    "features.subscription-retry"  -> false,
    "features.obligation-retry"    -> false,
    "features.financials-retry"    -> false,
    "features.returns-retry"       -> false,
    "features.submit-return-retry" -> false
  ) ++ configOverrides

  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .configure(additionalAppConfig)
      .overrides(
        bind(classOf[Clock]).toInstance(clock),
        bind(classOf[DataGenerator]).toInstance(dummyDataGenerator)
      )
      .build()

  val uuidGenerator = new RandomUUIDGenerator() {
    override def uuid: String = dummyUUID
  }

  val cc: ControllerComponents = stubControllerComponents()

  val fakeRequest: FakeRequest[AnyContentAsEmpty.type] = FakeRequest()
  def fakeRequestWithParameters(parameters: Map[String, String]): FakeRequest[AnyContentAsEmpty.type] = {
    val url = s"/?${parameters.toSeq.map { case (key, value) => s"$key=$value" }.mkString("&")}"
    FakeRequest("", url)
  }

  val appConfig: AppConfig         = app.injector.instanceOf[AppConfig]
  val bodyParsers: PlayBodyParsers = app.injector.instanceOf[PlayBodyParsers]

  def fakeRequestWithJsonBody(json: JsValue): FakeRequest[JsValue] = FakeRequest("", "/", FakeHeaders(), json)

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
  implicit val hc: HeaderCarrier    = HeaderCarrier()
}
