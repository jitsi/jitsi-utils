/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.*;

/**
 * Basic test for {@link PasswordUtil} class.
 *
 * @author Pawel Domas
 */
public class PasswordUtilTest
{
    @Test
    public void testShadowPassword()
    {
        String cmdLine = "AppMain org.jitsi.videobridge.Main" +
            " --host=example.com --secret3=blablabla --port=5347" +
            " -secret=pass1 --subdomain=jvb3 --apis=rest,xmpp" +
            " secret2=23pass4234";

        cmdLine = PasswordUtil.replacePasswords(
            cmdLine,
            "", "secret3", "secret", "secret2");

        assertEquals("AppMain org.jitsi.videobridge.Main" +
                " --host=example.com --secret3=X --port=5347" +
                " -secret=X --subdomain=jvb3 --apis=rest,xmpp" +
                " secret2=X",
            cmdLine);
    }
}
