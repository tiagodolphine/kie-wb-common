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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import com.google.gwt.logging.client.LogConfiguration;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.select.SelectionControl;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandFactory;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.service.ClientRuntimeError;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl.CompositeCommandBuilder;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.BoundsImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;
import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;
import static org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key.C;
import static org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key.CONTROL;
import static org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key.V;

/**
 * This session command obtains the selected elements on session and executes a delete operation for each one.
 * It also captures the <code>DELETE</code> keyboard event and fires the delete operation as well.
 */
@Dependent
public class CutSelectionSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    private static Logger LOGGER = Logger.getLogger(CutSelectionSessionCommand.class.getName());

    private final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    private final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory;
    private final Collection<String> elements;

    protected CutSelectionSessionCommand() {
        this(null,
             null);
    }

    @Inject
    public CutSelectionSessionCommand(final @Session SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                                      final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory) {
        super(true);
        this.sessionCommandManager = sessionCommandManager;
        this.canvasCommandFactory = canvasCommandFactory;
        this.elements = new LinkedHashSet<>();

        GWT.log("PasteSelectionSessionCommand");
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
    }

    @Override
    public <V> void execute(final Callback<V> callback) {
        checkNotNull("callback",
                     callback);

        CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> commandBuilder = new CompositeCommandBuilder<>();
        final AbstractCanvasHandler canvasHandler = (AbstractCanvasHandler) getSession().getCanvasHandler();

        if (null != getSession().getSelectionControl()) {

            final SelectionControl<AbstractCanvasHandler, Element> selectionControl = getSession().getSelectionControl();
            final String canvasRootUUID = canvasHandler.getDiagram().getMetadata().getCanvasRootUUID();

            selectionControl.getSelectedItems().stream()
                    .map(this::getElement).map(Element::asNode)
                    .map(node -> (Node<View<?>, Edge>) node)
                    .forEach(node -> commandBuilder.addCommand(canvasCommandFactory.cloneNode(node, canvasRootUUID, calculateNewLocation(node))));

            // Execute the command.
            final CommandResult<CanvasViolation> result = sessionCommandManager.execute(canvasHandler, commandBuilder.build());

            //Send feedback.
            setCallback(callback, result);
        }
    }

    private <V> void setCallback(Callback<V> callback, CommandResult<CanvasViolation> result) {
        if (!CommandUtils.isError(result)) {
            callback.onSuccess();
        } else {
            callback.onError((V) result);
        }
    }

    private Element<? extends View<?>> getElement(String uuid) {
        AbstractCanvasHandler canvasHandler = (AbstractCanvasHandler) getSession().getCanvasHandler();
        return canvasHandler.getGraphIndex().get(uuid);
    }

    private Point2D calculateNewLocation(final Node<? extends View<?>, Edge> node) {
        final BoundsImpl bounds = (BoundsImpl) node.getContent().getBounds();
        final double x = bounds.getX();
        final double y = bounds.getY();

        return new Point2D(x + 15, y + 15);
    }

    void onKeyDownEvent(final Key... keys) {
        handleCtrlV(keys);
        handleCtrlC(keys);
    }

    private void handleCtrlC(Key[] keys) {
        if (doKeysMatch(keys, CONTROL, C)) {
            GWT.log("CTRL + C");
        }
    }

    private void handleCtrlV(Key[] keys) {
        if (doKeysMatch(keys, CONTROL, V)) {
            GWT.log("CTRL + V");
            this.execute(newDefaultCallback());
        }
    }

    private Callback<ClientRuntimeError> newDefaultCallback() {
        return new Callback<ClientRuntimeError>() {
            @Override
            public void onSuccess() {
                // Nothing to do.
            }

            @Override
            public void onError(final ClientRuntimeError error) {
                LOGGER.log(Level.SEVERE,
                           "Error while trying to delete selected items. Message=[" + error.toString() + "]",
                           error.getThrowable());
            }
        };
    }

    private void log(final Level level,
                     final String message) {
        if (LogConfiguration.loggingIsEnabled()) {
            LOGGER.log(level,
                       message);
        }
    }
}