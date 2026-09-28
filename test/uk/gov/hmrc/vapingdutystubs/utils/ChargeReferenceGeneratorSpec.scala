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

import org.mockito.Mockito.when
import uk.gov.hmrc.vapingdutystubs.base.SpecBase

class ChargeReferenceGeneratorSpec extends SpecBase {

  private val mockUuidGenerator = mock[RandomUUIDGenerator]
  private val generator = new ChargeReferenceGenerator(mockUuidGenerator)

  private val CHARGE_REFERENCE_PREFIX = "XM"
  private val CHARGE_REFERENCE_TOTAL_LENGTH = 16

  "ChargeReferenceGenerator" - {

    "generate must" - {

      "return None when totalDue is zero" in {
        val result = generator.generate(BigDecimal("0.00"))

        result mustBe None
      }

      "return a charge reference when totalDue is negative" in {
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn(testUuidForNegativeAmount)

        val result = generator.generate(BigDecimal("-100.00"))

        result mustBe defined
        result mustBe Some(testChargeRefNegative)
      }

      "return a charge reference when totalDue is positive" in {
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn(testUuidForPositiveAmount)

        val result = generator.generate(BigDecimal("100.00"))

        result mustBe defined
        result mustBe Some(testChargeRefPositive)
      }

      "generate charge reference with correct format and length" in {
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn(testUuidForFormat)

        val result = generator.generate(BigDecimal("1500.50"))

        result mustBe defined
        val chargeRef = result.get

        chargeRef must startWith(CHARGE_REFERENCE_PREFIX)
        chargeRef must have length CHARGE_REFERENCE_TOTAL_LENGTH
        chargeRef mustBe testChargeRefFormat
      }

      "take only first 14 characters from UUID" in {
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn(testUuidLong)

        val result = generator.generate(BigDecimal("100.00"))

        result mustBe defined
        result.get mustBe testChargeRefLong
        result.get must have length CHARGE_REFERENCE_TOTAL_LENGTH
      }

      "convert charge reference to uppercase" in {
        when(mockUuidGenerator.uuidHyphenTrimmed).thenReturn(testUuidLowercase)

        val result = generator.generate(BigDecimal("100.00"))

        result mustBe defined
        result.get mustBe testChargeRefUppercase
        result.get must fullyMatch regex "[A-Z0-9]+"
      }
    }
  }
}
