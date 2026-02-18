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

package uk.gov.hmrc.vapingdutystubs.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration

import scala.util.matching.Regex

@Singleton
class AppConfig @Inject()(config: Configuration) {

  val appName: String = config.get[String]("appName")

  object Patterns {
    /**
     * Matches third from last digit to the first number in the below regex
     */
    val approved: Regex            = "\\w+2\\d{2}$".r
    val rejected: Regex            = "\\w+7\\d{2}$".r
    val withdrawn: Regex           = "\\w+8\\d{2}$".r
    val notFound: Regex            = "\\w+4\\d{2}$".r
    val badRequest: Regex          = "\\w+6\\d{2}$".r
    val unprocessableEntity: Regex = "\\w+5\\d{2}$".r
    
    val allChars: String   = "[a-zA-Z]+"

    /** Determines if a given vpdId is in the correct format */
    val validVpdId: String = "(?:GB|XI)WK[0-9]{7}WK"

    /**
     * Extracts the email flag digit from a given vpdId
     * @return extracted email flag digit (first number in given string)
     */
    def getEmailFlagDigit(vpdId: String): Int = "[0-9]".r.findFirstIn(vpdId).get.toInt
  }
}
