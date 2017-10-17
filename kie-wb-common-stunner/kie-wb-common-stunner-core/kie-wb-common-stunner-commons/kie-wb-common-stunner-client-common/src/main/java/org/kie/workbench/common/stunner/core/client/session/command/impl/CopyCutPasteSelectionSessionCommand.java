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
import org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher;
import org.kie.workbench.common.stunner.core.client.canvas.controls.select.SelectionControl;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandFactory;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent;
import org.kie.workbench.common.stunner.core.client.service.ClientRuntimeError;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.BoundsImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;

/**
 * This session command obtains the selected elements on session and executes a delete operation for each one.
 * It also captures the <code>DELETE</code> keyboard event and fires the delete operation as well.
 */
@Dependent
public class CopyCutPasteSelectionSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    private static Logger LOGGER = Logger.getLogger(CopyCutPasteSelectionSessionCommand.class.getName());

    private final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    private final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory;
    private final Collection<String> elements;
    private CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> commandBuilder;

    protected CopyCutPasteSelectionSessionCommand() {
        this(null,
             null);
    }

    @Inject
    public CopyCutPasteSelectionSessionCommand(final @Session SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                                               final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory) {
        super(true);
        this.sessionCommandManager = sessionCommandManager;
        this.canvasCommandFactory = canvasCommandFactory;
        this.elements = new LinkedHashSet<String>();

        final CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> commandBuilder =
                new CompositeCommandImpl
                        .CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                        .forward();
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
    }

    @Override
    public <V> void execute(final Callback<V> callback) {
//        checkNotNull("callback",
//                     callback);

        GWT.log("execute");

        if (null != getSession().getSelectionControl()) {
            final AbstractCanvasHandler canvasHandler = (AbstractCanvasHandler) getSession().getCanvasHandler();
            final SelectionControl<AbstractCanvasHandler, Element> selectionControl = getSession().getSelectionControl();

            selectionControl.getSelectedItems().stream().map(this::getElement).map(Element::asNode).map(node -> (Node<View<?>, Edge>) node)
                    .forEach(node -> {
                        GWT.log("node " + node);
                        commandBuilder.addCommand(canvasCommandFactory.cloneNode(node, clone -> afterClone(node)));
                    });

            // Execute the command.
            final CommandResult<CanvasViolation> result = getSession()
                    .getCommandManager()
                    .execute(getSession().getCanvasHandler(),
                             commandBuilder.build());

            // Send feedback.
            if (!CommandUtils.isError(result)) {
                callback.onSuccess();
            } else {
                callback.onError((V) result);
            }

            GWT.log("execute OK");
        }
    }

    private void afterClone(Node<View<?>, Edge> node) {
        //If cut
        //commandBuilder.addCommand(canvasCommandFactory.deleteNode(node));

//                        Optional.ofNullable(GraphUtils.getParent(node))
//                                .map(parent -> (Node) parent)
//                                .map(parentNode -> canvasCommandFactory.setChildNode(parentNode, node))
//                                .ifPresent(setParent -> commandBuilder.addCommand(setParent));

        Point2D newLocation = calculateNewLocation(node);
        commandBuilder.addCommand(canvasCommandFactory.updatePosition(node, newLocation.getX(), newLocation.getY()));
    }

    private Element<? extends View<?>> getElement(String uuid) {
        AbstractCanvasHandler canvasHandler = (AbstractCanvasHandler) getSession().getCanvasHandler();
        return canvasHandler.getGraphIndex().get(uuid);
    }

    private Point2D calculateNewLocation(final Node<? extends View<?>, Edge> node) {
        final BoundsImpl bounds = (BoundsImpl) node.getContent().getBounds();
        final double x = bounds.getX();
        final double y = bounds.getY();
        final double width = bounds.getWidth();
        final double height = bounds.getHeight();
        return new Point2D(x + 25, y + 25);

    }

    void onKeyDownEvent(final KeyboardEvent.Key... keys) {
        if (KeysMatcher.doKeysMatch(keys,
                                    KeyboardEvent.Key.DELETE)) {
            CopyCutPasteSelectionSessionCommand.this.execute(new Callback<ClientRuntimeError>() {
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
            });
        }
    }

    private void log(final Level level,
                     final String message) {
        if (LogConfiguration.loggingIsEnabled()) {
            LOGGER.log(level,
                       message);
        }
    }
}
