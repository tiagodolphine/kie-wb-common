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

import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.CompositeCommand;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.consumer.ChildrenTransverseConsumerImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.consumer.ChildrenTraverseConsumer;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.tree.TreeWalkTraverseProcessorImpl;

/**
 * Clone a node shape into de canvas.
 */
public class CloneCanvasNodeCommand extends AddCanvasChildNodeCommand {

    private transient CompositeCommand<AbstractCanvasHandler, CanvasViolation> commands;
    private transient ChildrenTraverseConsumer childrenTraverseConsumer;

    public CloneCanvasNodeCommand(Node parent, Node candidate, String shapeSetId) {
        super(parent, candidate, shapeSetId);
        this.commands = new CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                .reverse()
                .build();
        this.childrenTraverseConsumer =
                new ChildrenTransverseConsumerImpl(new ChildrenTraverseProcessorImpl(new TreeWalkTraverseProcessorImpl()));
    }

    @Override
    public CommandResult<CanvasViolation> execute(AbstractCanvasHandler context) {
        CommandResult<CanvasViolation> rootResult = super.execute(context);

        if (CommandUtils.isError(rootResult)) {
            return rootResult;
        }

        //first process clone children nodes
        childrenTraverseConsumer.consume(context.getGraphIndex().getGraph(), getCandidate(), node ->
                commands.addCommand(new CloneCanvasNodeCommand(getCandidate(), node, getShapeSetId()))
        );

        //process clone connectors
        childrenTraverseConsumer.consume(context.getGraphIndex().getGraph(), getCandidate(), node ->
                node.getOutEdges()
                        .stream()
                        .forEach(edge -> commands.addCommand(new AddCanvasConnectorCommand((Edge) edge, getShapeSetId())))
        );

        return commands.execute(context);
    }
}
