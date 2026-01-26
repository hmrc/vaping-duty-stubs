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

package uk.gov.hmrc.vapingdutystubs.data

import java.time.Clock
import javax.inject.Inject
import scala.util.Random

class DataGenerator @Inject()(clock: Clock) {
  private val random                          = new Random(clock.millis())
  private def randomNumberString(length: Int) = Range(1, length + 1).map(_ => random.nextInt(10)).mkString

  def submissionIdGen(): String      = randomNumberString(12)
  def chargeReferenceGen(): String   = s"XA${randomNumberString(14)}"
}
