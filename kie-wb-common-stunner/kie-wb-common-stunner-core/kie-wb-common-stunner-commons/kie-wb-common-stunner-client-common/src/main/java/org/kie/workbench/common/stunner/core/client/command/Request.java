/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.stunner.core.client.command;

import java.lang.annotation.RetentionPolicy;

/**
 * The client scope for request components.
 * <p>
 * A client request starts once a user starts a new interaction and
 * ends once the interaction finishes. Multiple client requests/interactions
 * can occur in the same client session.
 * <p>
 * The request lifecycle depends on the current context,
 * application's client, etc, so it depends
 * on the current context.
 * <p>
 * For example, a mouse down event starts a new request and it ends once mouse up event if captured.
 */
@java.lang.annotation.Documented
@java.lang.annotation.Retention( RetentionPolicy.RUNTIME )
@javax.inject.Qualifier
public @interface Request {

}