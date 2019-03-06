/*
 * Copyright (c) 2016 Network New Technologies Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file except in compliance with the License.
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
package com.networknt.validator;

/**
 * Validator configuration class that maps to validator.yml properties
 *
 * @author Steve Hu
 */
public class ValidatorConfig {
    boolean enabled;
    boolean logError;
    boolean skipBodyValidation = false;

    public ValidatorConfig() {
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isLogError() { return logError; }

    public void setLogError(boolean logError) { this.logError = logError; }

    public boolean isSkipBodyValidation() {
        return skipBodyValidation;
    }

    public void setSkipBodyValidation(boolean skipBodyValidation) {
        this.skipBodyValidation = skipBodyValidation;
    }
}
