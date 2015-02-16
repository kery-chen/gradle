/*
 * Copyright 2015 the original author or authors.
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

package org.gradle.internal.operations;

import org.gradle.api.Action;
import org.gradle.internal.concurrent.ExecutorFactory;
import org.gradle.internal.concurrent.Stoppable;
import org.gradle.internal.concurrent.StoppableExecutor;

public class DefaultBuildOperationProcessor implements BuildOperationProcessor, Stoppable {

    private final StoppableExecutor fixedSizePool;

    public DefaultBuildOperationProcessor(ExecutorFactory executorFactory, int maxThreads) {
        final int actualThreads = actualThreadCount(maxThreads);
        this.fixedSizePool = executorFactory.create("build operations", actualThreads);
    }

    int actualThreadCount(int maxThreads) {
        final int actualThreads;
        if (maxThreads < 0) {
            actualThreads = Runtime.getRuntime().availableProcessors();
        } else if (maxThreads == 0) {
            actualThreads = 1;
        } else {
            actualThreads = maxThreads;
        }
        return actualThreads;
    }

    public <T> OperationQueue<T> newQueue(Action<? super T> worker) {
        return new DefaultOperationQueue<T>(fixedSizePool, worker);
    }

    public void stop() {
        fixedSizePool.stop();
    }
}
