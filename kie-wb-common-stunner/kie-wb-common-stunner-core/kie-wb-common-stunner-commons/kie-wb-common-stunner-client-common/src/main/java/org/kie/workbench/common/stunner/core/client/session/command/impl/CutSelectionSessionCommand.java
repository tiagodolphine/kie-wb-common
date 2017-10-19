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

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.clipboard.ClipboardControl;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.service.ClientRuntimeError;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl.CompositeCommandBuilder;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;
import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;
import static org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key.CONTROL;

/**
 * This session command obtains the selected elements on session and executes a delete operation for each one.
 * It also captures the <code>DELETE</code> keyboard event and fires the delete operation as well.
 */
@Dependent
public class CutSelectionSessionCommand {

//    private static Logger LOGGER = Logger.getLogger(CutSelectionSessionCommand.class.getName());
//
//    protected CutSelectionSessionCommand() {
//        this(null);
//    }
//
//    @Inject
//    public CutSelectionSessionCommand(ClipboardControl clipboardControl) {
//        super(clipboardControl);
//    }
//
//    @Override
//    public <V> void execute(final Callback<V> callback) {
//        checkNotNull("callback",
//                     callback);
//
//        CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> commandBuilder = new CompositeCommandBuilder<>();
//
//        // Execute the command.
//        //final CommandResult<CanvasViolation> result = sessionCommandManager.execute(getCanvasHandler(), commandBuilder.build());
//
//        //Send feedback.
//        //setCallback(callback, result);
//
//    }
//
//    @Override
//    protected void onKeyDownEvent(final Key... keys) {
//        handleCtrlX(keys);
//    }
//
//    private void handleCtrlX(Key[] keys) {
//        if (doKeysMatch(keys, CONTROL, Key.X)) {
//            GWT.log("CTRL + C");
//        }
//    }
//
//    private Callback<ClientRuntimeError> newDefaultCallback() {
//        return new Callback<ClientRuntimeError>() {
//            @Override
//            public void onSuccess() {
//                // Nothing to do.
//            }
//
//            @Override
//            public void onError(final ClientRuntimeError error) {
//                LOGGER.log(Level.SEVERE,
//                           "Error while trying to delete selected items. Message=[" + error.toString() + "]",
//                           error.getThrowable());
//            }
//        };
//    }
}