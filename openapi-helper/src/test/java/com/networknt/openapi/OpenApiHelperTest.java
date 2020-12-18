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

package com.networknt.openapi;

import com.networknt.config.Config;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by steve on 23/09/16.
 */
public class OpenApiHelperTest {
    @Test
    public void testOAuth2Name() {
        String spec = Config.getInstance().getStringFromFile("openapi.yaml");
        OpenApiHelper.init(spec);
        Assert.assertEquals(1, OpenApiHelper.oauth2Names.size());
        Assert.assertEquals("petstore_auth", OpenApiHelper.oauth2Names.get(0));
    }

    @Test
    public void testBasePath() {
        Assert.assertEquals("/v1", OpenApiHelper.basePath);
    }
}
