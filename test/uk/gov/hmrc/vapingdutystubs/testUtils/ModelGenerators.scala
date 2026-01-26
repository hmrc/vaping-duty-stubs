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

package uk.gov.hmrc.vapingdutystubs.testUtils

import org.scalacheck.Gen
import uk.gov.hmrc.vapingdutystubs.data.DataGenerator
import uk.gov.hmrc.vapingdutystubs.models.ReturnPeriod

import java.time.Clock
import scala.language.reflectiveCalls

trait ModelGenerators {
  val clock: Clock

  def vpdIdGen: Gen[String] = Gen.listOfN(10, Gen.numChar).map(id => s"XMADP${id.mkString}")

  lazy val dummyDataGenerator = new DataGenerator(clock) {
    lazy val submissionId      = super.submissionIdGen()
    lazy val chargeReference   = super.chargeReferenceGen()

    override def submissionIdGen(): String      = submissionId
    override def chargeReferenceGen(): String   = chargeReference
  }

  def submissionIdGen(): Gen[String]    = Gen.const(dummyDataGenerator.submissionIdGen())
  def chargeReferenceGen(): Gen[String] = Gen.const(dummyDataGenerator.chargeReferenceGen())

  def periodKeyGen: Gen[String] = for {
    year  <- Gen.chooseNum(23, 50)
    month <- Gen.chooseNum(0, 11)
  } yield s"${year}A${(month + 'A').toChar}"

  def invalidPeriodKeyGen: Gen[String] = Gen.alphaStr
    .retryUntil(s => s.nonEmpty && !s.matches(ReturnPeriod.returnPeriodPattern.toString))
}
