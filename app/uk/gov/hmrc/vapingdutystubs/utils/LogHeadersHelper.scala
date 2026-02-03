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

package uk.gov.hmrc.vapingdutystubs.utils

import play.api.Logging
import play.api.mvc.Request

object LogHeadersHelper extends Logging {
  def logHeaders(request: Request[_], requestType: String, relevantHeaders: Set[String]): Unit = {
    val headers = request.headers.toMap.view
      .filterKeys(key => relevantHeaders.contains(key))
      .map { case (key, values) => s"$key=${values.mkString(",")}" }
      .mkString(",")
    logger.info(s"Headers for $requestType: $headers")
  }
}
