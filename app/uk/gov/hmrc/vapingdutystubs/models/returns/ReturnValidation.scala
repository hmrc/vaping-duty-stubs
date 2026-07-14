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

package uk.gov.hmrc.vapingdutystubs.models.returns

import uk.gov.hmrc.vapingdutystubs.models.returns.submit.ReturnCreateRequest
import uk.gov.hmrc.vapingdutystubs.models.returns.view.{OverDeclaration, UnderDeclaration, SpoiltProduct, OtherOptions}

object ReturnValidation {

  private val FLAG_YES = "1"
  private val FLAG_NO = "0"

  // Mirrors AdjustmentType.dutyThreshold in vaping-duty-frontend: a reason is only
  // mandatory (and therefore only sent) when the declared duty total is £1000 or more.
  private val REASON_REQUIRED_DUTY_THRESHOLD: BigDecimal = BigDecimal(1000)

  def validate(request: ReturnCreateRequest): Either[String, Unit] = {
    for {
      _ <- validateVapingProductsProduced(request.vapingProductsProduced)
      _ <- validateOverDeclaration(request.overDeclaration)
      _ <- validateUnderDeclaration(request.underDeclaration)
      _ <- validateSpoiltProduct(request.spoiltProduct)
      _ <- validateOtherOptions(request.otherOptions)
    } yield ()
  }

  private def validateVapingProductsProduced(vpp: VapingProductsProduced): Either[String, Unit] = {
    vpp.vapingProdManufactured match {
      case FLAG_YES if vpp.returns.isEmpty =>
        Left("vapingProdManufactured is '1' but returns array is empty")
      case FLAG_NO if vpp.returns.nonEmpty =>
        Left("vapingProdManufactured is '0' but returns array is not empty")
      case FLAG_YES | FLAG_NO => Right(())
      case other => Left(s"vapingProdManufactured must be '0' or '1', got: $other")
    }
  }

  private def validateOverDeclaration(overDecl: Option[OverDeclaration]): Either[String, Unit] = {
    overDecl match {
      case None => Right(())
      case Some(od) => od.overDeclFilled match {
        case FLAG_YES =>
          val totalDutyDue = od.overDeclarationProducts.getOrElse(Seq.empty).map(_.dutyDue).sum
          val reasonRequired = totalDutyDue >= REASON_REQUIRED_DUTY_THRESHOLD

          if (reasonRequired && od.reasonForOverDecl.isEmpty) {
            Left(s"overDeclFilled is '1' and total duty due is $totalDutyDue (>= $REASON_REQUIRED_DUTY_THRESHOLD) but reasonForOverDecl is missing")
          } else if (od.overDeclarationProducts.isEmpty || od.overDeclarationProducts.exists(_.isEmpty)) {
            Left("overDeclFilled is '1' but overDeclarationProducts is empty")
          } else {
            Right(())
          }
        case FLAG_NO =>
          if (od.reasonForOverDecl.isDefined || od.overDeclarationProducts.exists(_.nonEmpty)) {
            Left("overDeclFilled is '0' but reasonForOverDecl or overDeclarationProducts is present")
          } else {
            Right(())
          }
        case other => Left(s"overDeclFilled must be '0' or '1', got: $other")
      }
    }
  }

  private def validateUnderDeclaration(underDecl: Option[UnderDeclaration]): Either[String, Unit] = {
    underDecl match {
      case None => Right(())
      case Some(ud) => ud.underDeclFilled match {
        case FLAG_YES =>
          val totalDutyDue = ud.underDeclarationProducts.getOrElse(Seq.empty).map(_.dutyDue).sum
          val reasonRequired = totalDutyDue >= REASON_REQUIRED_DUTY_THRESHOLD

          if (reasonRequired && ud.reasonForUnderDecl.isEmpty) {
            Left(s"underDeclFilled is '1' and total duty due is $totalDutyDue (>= $REASON_REQUIRED_DUTY_THRESHOLD) but reasonForUnderDecl is missing")
          } else if (ud.underDeclarationProducts.isEmpty || ud.underDeclarationProducts.exists(_.isEmpty)) {
            Left("underDeclFilled is '1' but underDeclarationProducts is empty")
          } else {
            Right(())
          }
        case FLAG_NO =>
          if (ud.reasonForUnderDecl.isDefined || ud.underDeclarationProducts.exists(_.nonEmpty)) {
            Left("underDeclFilled is '0' but reasonForUnderDecl or underDeclarationProducts is present")
          } else {
            Right(())
          }
        case other => Left(s"underDeclFilled must be '0' or '1', got: $other")
      }
    }
  }

  private def validateSpoiltProduct(spoiltProd: Option[SpoiltProduct]): Either[String, Unit] = {
    spoiltProd match {
      case None => Right(())
      case Some(sp) => sp.spoiltProductFilled match {
        case FLAG_YES =>
          if (sp.spoiltProducts.isEmpty || sp.spoiltProducts.exists(_.isEmpty)) {
            Left("spoiltProductFilled is '1' but spoiltProducts is empty")
          } else {
            Right(())
          }
        case FLAG_NO =>
          if (sp.spoiltProducts.exists(_.nonEmpty)) {
            Left("spoiltProductFilled is '0' but spoiltProducts is present")
          } else {
            Right(())
          }
        case other => Left(s"spoiltProductFilled must be '0' or '1', got: $other")
      }
    }
  }

  private def validateOtherOptions(otherOpts: Option[OtherOptions]): Either[String, Unit] = {
    otherOpts match {
      case None => Right(())
      case Some(oo) => oo.vapingProductUnderDutySuspense match {
        case FLAG_YES =>
          if (oo.volumeMovedFromDutySuspense.isEmpty || oo.volumeMovedToDutySuspense.isEmpty) {
            Left("vapingProductUnderDutySuspense is '1' but volume fields are missing")
          } else {
            Right(())
          }
        case FLAG_NO =>
          if (oo.volumeMovedFromDutySuspense.isDefined || oo.volumeMovedToDutySuspense.isDefined) {
            Left("vapingProductUnderDutySuspense is '0' but volume fields are present")
          } else {
            Right(())
          }
        case other => Left(s"vapingProductUnderDutySuspense must be '0' or '1', got: $other")
      }
    }
  }
}
