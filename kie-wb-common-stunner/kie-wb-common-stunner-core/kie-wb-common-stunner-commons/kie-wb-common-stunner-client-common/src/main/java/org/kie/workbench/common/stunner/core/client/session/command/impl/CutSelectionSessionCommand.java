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

package org.kie.workbench.common.stunner.core.client.session.command.impl;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;

import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;

/**
 * This session command obtains the selected elements on session and executes a delete operation for each one.
 * It also captures the <code>DELETE</code> keyboard event and fires the delete operation as well.
 */
@Dependent
public class CutSelectionSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    private final CopySelectionSessionCommand copySelectionSessionCommand;
    private final DeleteSelectionSessionCommand deleteSelectionSessionCommand;

    protected CutSelectionSessionCommand() {
        this(null, null);
    }

    @Inject
    public CutSelectionSessionCommand(final CopySelectionSessionCommand copySelectionSessionCommand, final DeleteSelectionSessionCommand deleteSelectionSessionCommand) {
        super(true);
        this.copySelectionSessionCommand = copySelectionSessionCommand;
        this.deleteSelectionSessionCommand = deleteSelectionSessionCommand;
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        copySelectionSessionCommand.bind(getSession());
        deleteSelectionSessionCommand.bind(getSession());
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
    }

    protected void onKeyDownEvent(final Key... keys) {
        handleCtrlX(keys);
    }

    private void handleCtrlX(Key[] keys) {
        if (doKeysMatch(keys, Key.CONTROL, Key.X)) {
            this.execute(newDefaultCallback("Error while trying to cut selected items. Message="));
            GWT.log("CTRL + X");
        }
    }

    @Override
    public <V> void execute(Callback<V> callback) {

        copySelectionSessionCommand.execute(new Callback<V>() {
            @Override
            public void onSuccess() {
                deleteSelectionSessionCommand.execute(callback);
            }

            @Override
            public void onError(V error) {
                callback.onError(error);
            }
        });
    }
}