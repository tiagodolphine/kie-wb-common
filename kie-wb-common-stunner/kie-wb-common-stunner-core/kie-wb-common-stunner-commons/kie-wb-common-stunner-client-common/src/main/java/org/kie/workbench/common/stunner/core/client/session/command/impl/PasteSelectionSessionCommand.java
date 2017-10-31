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
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.CanvasHandler;
import org.kie.workbench.common.stunner.core.client.canvas.controls.clipboard.ClipboardControl;
import org.kie.workbench.common.stunner.core.client.canvas.util.CanvasLayoutUtils;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandFactory;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.client.command.SessionCommandManager;
import org.kie.workbench.common.stunner.core.client.event.keyboard.KeyboardEvent.Key;
import org.kie.workbench.common.stunner.core.client.session.ClientFullSession;
import org.kie.workbench.common.stunner.core.client.session.Session;
import org.kie.workbench.common.stunner.core.client.session.command.AbstractClientSessionCommand;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl.CompositeCommandBuilder;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;
import static org.kie.workbench.common.stunner.core.client.canvas.controls.keyboard.KeysMatcher.doKeysMatch;

/**
 * This session command obtains the selected elements on the clipboard and clone each one of them.
 */
@Dependent
public class PasteSelectionSessionCommand extends AbstractClientSessionCommand<ClientFullSession> {

    public static final int DEFAULT_PADDING = 15;
    private static Logger LOGGER = Logger.getLogger(PasteSelectionSessionCommand.class.getName());

    private final SessionCommandManager<AbstractCanvasHandler> sessionCommandManager;
    private final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory;
    private final ClipboardControl<Element> clipboardControl;
    private final CanvasLayoutUtils canvasLayoutUtils;

    protected PasteSelectionSessionCommand() {
        this(null,null,null,null);
    }

    @Inject
    public PasteSelectionSessionCommand(final @Session SessionCommandManager<AbstractCanvasHandler> sessionCommandManager,
                                        final CanvasCommandFactory<AbstractCanvasHandler> canvasCommandFactory,
                                        final ClipboardControl<Element> clipboardControl,
                                        final CanvasLayoutUtils canvasLayoutUtils) {
        super(true);
        this.sessionCommandManager = sessionCommandManager;
        this.canvasCommandFactory = canvasCommandFactory;
        this.clipboardControl = clipboardControl;
        this.canvasLayoutUtils = canvasLayoutUtils;

        GWT.log("PasteSelectionSessionCommand");
    }

    @Override
    public void bind(final ClientFullSession session) {
        super.bind(session);
        session.getKeyboardControl().addKeyShortcutCallback(this::onKeyDownEvent);
    }

    void onKeyDownEvent(final Key... keys) {
        handleCtrlV(keys);
    }

    private void handleCtrlV(Key[] keys) {
        if (doKeysMatch(keys, Key.CONTROL, Key.V)) {
            GWT.log("CTRL + V");
            this.execute(newDefaultCallback("Error while trying to paste selected items. Message="));
        }
    }

    @Override
    public <V> void execute(final Callback<V> callback) {
        checkNotNull("callback",
                     callback);

        if (clipboardControl.hasElements()) {
            final CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation> commandBuilder = new CompositeCommandBuilder<>();

            //for now just pasting Nodes not Edges
            commandBuilder.addCommands(clipboardControl.getElements().stream()
                                               .filter(element -> element instanceof Node)
                                               .map(Element::asNode)
                                               .filter(Objects::nonNull)
                                               .map(node -> (Node<View<?>, Edge>) node)
                                               .map(node -> {
                                                   String newParentUUID = getNewParentUUID(node);
                                                   return canvasCommandFactory.cloneNode(node, newParentUUID, calculateNewLocation(node, newParentUUID));
                                               })
                                               .collect(Collectors.toList()));

            // Execute the command.
            final CommandResult<CanvasViolation> result = sessionCommandManager.execute(getCanvasHandler(), commandBuilder.build());

            //Send feedback.
            setCallback(callback, result);
        }
    }

    private String getNewParentUUID(Node node) {
        //getting parent if selected
        Optional<Element> selectedParent = getSelectedParentElement(node);
        if (selectedParent.isPresent() && !Objects.equals(selectedParent.get().getUUID(), node.getUUID()) && checkIfExistsOnCanvas(selectedParent.get().getUUID())) {
            return selectedParent.get().getUUID();
        }

        //getting node parent if no different parent is selected
        String nodeParentUUID = clipboardControl.getParent(node.getUUID());
        if (selectedParent.isPresent() && Objects.equals(selectedParent.get().getUUID(), node.getUUID()) && Objects.nonNull(nodeParentUUID) && checkIfExistsOnCanvas(nodeParentUUID)) {
            return nodeParentUUID;
        }

        //return default parent that is the canvas in case no parent matches
        return getCanvasRootUUID();
    }

    private boolean checkIfExistsOnCanvas(String nodeParentUUID) {
        return Objects.nonNull(getElement(nodeParentUUID));
    }

    private String getCanvasRootUUID() {
        return getCanvasHandler().getDiagram().getMetadata().getCanvasRootUUID();
    }

    private Optional<Element> getSelectedParentElement(Node node) {
        if (null != getSession().getSelectionControl()) {
            Collection<String> selectedItems = getSession().getSelectionControl().getSelectedItems();
            if (Objects.nonNull(selectedItems) && !selectedItems.isEmpty()) {
                GWT.log(selectedItems.toString());
                String selectedUUID = selectedItems.stream().filter(Objects::nonNull).findFirst().orElse(null);
                return Optional.ofNullable(getElement(selectedUUID));
            }
        }
        return Optional.empty();
    }

    private Point2D calculateNewLocation(final Node<? extends View<?>, Edge> node, String newParentUUID) {
        Point2D position = GraphUtils.getPosition(node.getContent());

        //new parent different from the source node
        if (hasParentChanged(node, newParentUUID)) {
            return new Point2D(DEFAULT_PADDING, DEFAULT_PADDING);        }

        //node is still on canvas (not deleted)
        if (existsOnCanvas(node)) {
            return new Point2D(position.getX() + DEFAULT_PADDING, position.getY() + DEFAULT_PADDING);
        }

        //default or node was deleted
        return position;
    }

    private boolean hasParentChanged(Node<? extends View<?>, Edge> node, String newParentUUID) {
        return !Objects.equals(clipboardControl.getParent(node.getUUID()), newParentUUID);
    }

    private boolean existsOnCanvas(Node<? extends View<?>, Edge> node) {
        return Objects.nonNull(getCanvasHandler().getGraphIndex().getNode(node.getUUID()));
    }
}