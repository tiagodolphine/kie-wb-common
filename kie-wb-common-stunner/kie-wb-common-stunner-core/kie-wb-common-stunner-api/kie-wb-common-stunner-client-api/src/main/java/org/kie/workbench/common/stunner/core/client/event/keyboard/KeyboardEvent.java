/*
 * Copyright 2017 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.core.client.event.keyboard;

public interface KeyboardEvent {

    enum Key {
        ESC(27),
        CONTROL(17),
        SHIFT(16),
        DELETE(46),
        ARROW_UP(24),
        ARROW_DOWN(25),
        ARROW_LEFT(27),
        ARROW_RIGHT(26),
        C(67),
        V(86),
        X(88),
        Z(90);

        private final int unicharCode;

        Key(final int unicharCode) {
            this.unicharCode = unicharCode;
        }

        public int getUnicharCode() {
            return unicharCode;
        }
    }

    Key getKey();
}
