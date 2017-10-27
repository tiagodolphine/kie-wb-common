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

package org.kie.workbench.common.stunner.core.client.canvas.command;

import java.util.Objects;
import java.util.Optional;
import java.util.logging.Logger;

import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommand;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.CompositeCommand;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandExecutionContext;
import org.kie.workbench.common.stunner.core.graph.command.impl.UpdateElementPositionCommand;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;

public class CloneNodeCommand extends AbstractCanvasGraphCommand {

    private static Logger LOGGER = Logger.getLogger(CloneNodeCommand.class.getName());

    private final Node candidate;
    private final String parentUuid;
    private Optional<Point2D> cloneLocation;
    private transient CompositeCommand<AbstractCanvasHandler, CanvasViolation> command;
    private transient CanvasCommand undoCommand;

    @SuppressWarnings("unchecked")
    public CloneNodeCommand(final Node candidate, String parentUuid, Point2D cloneLocation) {
        this.command = new CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                .forward()
                .build();
        this.candidate = candidate;
        this.cloneLocation = Optional.ofNullable(cloneLocation);
        this.parentUuid = parentUuid;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Command<GraphCommandExecutionContext, RuleViolation> newGraphCommand(final AbstractCanvasHandler context) {
        return new org.kie.workbench.common.stunner.core.graph.command.impl.CloneNodeCommand(candidate, parentUuid, clone -> {
            //success cloned than create apply it to canvas
            command.addCommand(new CloneCanvasNodeCommand(clone, context.getDiagram().getMetadata().getShapeSetId()));
            command.addCommand(new SetCanvasChildNodeCommand(GraphUtils.getParent(clone).asNode(), clone));

            undoCommand = new DeleteCanvasNodeCommand(clone);

            //update position of cloned node
            cloneLocation.ifPresent(point -> handleClonedNodePosition(context, clone, point));
        });
    }

    private void handleClonedNodePosition(AbstractCanvasHandler context, Node<Definition, Edge> clone, Point2D point) {
        //graph position update
        CommandResult<RuleViolation> result =
                new UpdateElementPositionCommand(context.getGraphIndex().getNode(clone.getUUID()),
                                                 point.getX(),
                                                 point.getY()).execute(context.getGraphExecutionContext());
        //than update canvas position
        if (!CommandUtils.isError(result)) {
            command.addCommand(new UpdateCanvasElementPositionCommand(clone));
        } else {
            LOGGER.severe("Error updating position of cloned element " + clone.getUUID());
        }
    }

    @Override
    protected Command<AbstractCanvasHandler, CanvasViolation> newCanvasCommand(final AbstractCanvasHandler context) {
        return command;
    }

    @Override
    public CommandResult<CanvasViolation> undo(AbstractCanvasHandler context) {
        return undoCommand.execute(context);
    }
}