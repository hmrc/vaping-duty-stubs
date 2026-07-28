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

package uk.gov.hmrc.vapingdutystubs.models.financialdata

import play.api.libs.json.{Json, OFormat}

import java.time.LocalDate

final case class DocumentDetails(
  documentNumber: String,
  documentType: String,
  chargeReferenceNumber: Option[String],
  businessPartnerNumber: String,
  contractAccountNumber: String,
  contractAccountCategory: String,
  contractObjectNumber: String,
  contractObjectType: String,
  postingDate: LocalDate,
  issueDate: LocalDate,
  documentTotalAmount: BigDecimal,
  documentClearedAmount: BigDecimal,
  documentOutstandingAmount: BigDecimal,
  lineItemDetails: Seq[LineItemDetails]
)

object DocumentDetails {
  given format: OFormat[DocumentDetails] = Json.format[DocumentDetails]
}
