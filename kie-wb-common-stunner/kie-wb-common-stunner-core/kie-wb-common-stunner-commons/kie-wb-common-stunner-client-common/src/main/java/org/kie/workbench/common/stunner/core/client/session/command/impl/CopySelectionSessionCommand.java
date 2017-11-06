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

import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.clipboard.ClipboardControl;
import org.kie.workbench.common.stunner.core.client.canvas.controls.select.SelectionControl;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;

import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;

/**
 * This session command obtains the selected elements on session and copy the elements to a clipboard.
 */
@Dependent
public class CopySelectionSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    private static Logger LOGGER = Logger.getLogger(CopySelectionSessionCommand.class.getName());

    private final ClipboardControl clipboardControl;

    protected CopySelectionSessionCommand() {
        this(null);
    }

    @Inject
    public CopySelectionSessionCommand(final ClipboardControl clipboardControl) {
        super(true);
        this.clipboardControl = clipboardControl;

        GWT.log("CopySelectionSessionCommand");
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
    }

    protected void onKeyDownEvent(final Key... keys) {
        handleCtrlC(keys);
    }

    private void handleCtrlC(Key[] keys) {
        if (doKeysMatch(keys, Key.CONTROL, Key.C)) {
            this.execute(newDefaultCallback("Error while trying to copy selected items. Message="));
            GWT.log("CTRL + C");
        }
    }

    @Override
    public <V> void execute(final Callback<V> callback) {
        if (null != getSession().getSelectionControl()) {
            try {
                //for now just copy Nodes not Edges
                final SelectionControl<AbstractCanvasHandler, Element> selectionControl = getSession().getSelectionControl();

                //for now throw error in case trying to copy not Node elements
                if (selectionControl.getSelectedItems().stream()
                        .map(this::getElement)
                        .anyMatch(element -> !(element instanceof Node))) {
                    throw new RuntimeException("Copy node only is allowed");
                }

                //for now just copy Nodes not Edges
                clipboardControl.add(selectionControl.getSelectedItems().stream()
                                             .map(this::getElement)
                                             .filter(element -> element instanceof Node)
                                             .toArray(Element[]::new));
                //TODO: handle Edges copy

                callback.onSuccess();
            } catch (Exception e) {
                callback.onError((V) e);
            }
        }
    }
}