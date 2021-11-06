/*
 * Copyright @ 2018 - present 8x8, Inc.
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

package org.jitsi.utils.queue;

import java.util.concurrent.*;

/**
 * A single-threaded executor - like the result of
 * {@link Executors#newSingleThreadExecutor()} - except it won't run any
 * tasks until its {@link #start} method is called.
 */
public class BlockedExecutor extends ThreadPoolExecutor
{
    private final Semaphore startBlocker = new Semaphore(0);

    public BlockedExecutor()
    {
        super(1, 1,
            0L, TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>());
        execute(() -> {
            try
            {
                startBlocker.acquire();
            }
            catch (InterruptedException ignored)
            {
                ;
            }
        });
    }

    public void start()
    {
        startBlocker.release();
    }
}
