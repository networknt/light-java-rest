/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.networknt.validator.parameter;

import com.networknt.status.Status;
import org.junit.Assert;
import org.junit.Test;

import java.math.BigDecimal;

import static com.networknt.validator.ValidatorTestUtil.intParam;

public class IntegerParameterValidatorTest {

    private final IntegerParameterValidator classUnderTest = new IntegerParameterValidator();

    @Test
    public void validate_withNullValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate(null, intParam(false)));
    }

    @Test
    public void validate_withEmptyValue_shouldPass_whenNotRequired() {
        Assert.assertNull(classUnderTest.validate("", intParam(false)));
    }

    @Test
    public void validate_withNullValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate(null, intParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withEmptyValue_shouldFail_whenRequired() {
        Status status = classUnderTest.validate("", intParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11001", status.getCode()); // request parameter missing
    }

    @Test
    public void validate_withNonNumericValue_shouldFail() {
        Status status = classUnderTest.validate("123a", intParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11010", status.getCode()); // request parameter invalid format
    }

    @Test
    public void validate_withNonIntegerValue_shouldFail() {
        Status status = classUnderTest.validate("123.1", intParam(true));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11010", status.getCode()); // request parameter invalid format
    }

    @Test
    public void validate_withIntegerValue_shouldPass() {
        Assert.assertNull(classUnderTest.validate("123", intParam()));
    }

    @Test
    public void validate_withValueGreaterThanMax_shouldFail_ifMaxSpecified() {
        Status status = classUnderTest.validate("2", intParam(null, new BigDecimal(1.0)));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11012", status.getCode()); // request parameter number above max
    }

    @Test
    public void validate_withValueLessThanMin_shouldFail_ifMinSpecified() {
        Status status = classUnderTest.validate("0", intParam(new BigDecimal(1.0), null));
        Assert.assertNotNull(status);
        Assert.assertEquals("ERR11011", status.getCode()); // request parameter number below min
    }

    @Test
    public void validate_withValueInRange_shouldPass() {
        Assert.assertNull(classUnderTest.validate("2", intParam(new BigDecimal(1.0), new BigDecimal(3.0))));
    }

}
