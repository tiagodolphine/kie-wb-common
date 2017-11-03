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

import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.command.CanvasCommandResultBuilder;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.CompositeCommand;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessor;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.tree.TreeWalkTraverseProcessorImpl;

/**
 * Clone a node shape into de canvas.
 */
public class CloneCanvasNodeCommand extends AddCanvasChildNodeCommand {

    private transient CompositeCommand<AbstractCanvasHandler, CanvasViolation> commands;
    private transient ChildrenTraverseProcessor childrenTraverseProcessor;

    public CloneCanvasNodeCommand(Node parent, Node candidate, String shapeSetId) {
        super(parent, candidate, shapeSetId);
        this.commands = new CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                .reverse()
                .build();
        this.childrenTraverseProcessor = new ChildrenTraverseProcessorImpl(new TreeWalkTraverseProcessorImpl());
    }

    @Override
    public CommandResult<CanvasViolation> execute(AbstractCanvasHandler context) {
        GWT.log("canvas executed !");
        CommandResult<CanvasViolation> rootResult = super.execute(context);

        if (CommandUtils.isError(rootResult)) {
            return rootResult;
        }

        //first process clone children nodes
        Graph graph = context.getGraphIndex().getGraph();
        childrenTraverseProcessor.consume(graph, getCandidate(), node ->
                commands.addCommand(new CloneCanvasNodeCommand(getCandidate(), node, getShapeSetId())));

        //process clone connectors
        childrenTraverseProcessor.consume(graph, getCandidate(), node ->
                node.getOutEdges()
                        .stream()
                        .forEach(edge -> commands.addCommand(new AddCanvasConnectorCommand((Edge) edge, getShapeSetId()))));

        return commands.execute(context);
    }

    @Override
    public CommandResult<CanvasViolation> undo(AbstractCanvasHandler context) {
        CommandResult<CanvasViolation> commandResult = commands.undo(context);
        return new CanvasCommandResultBuilder(Stream.concat(StreamSupport.stream(commandResult.getViolations().spliterator(), false),
                                                            StreamSupport.stream(super.undo(context).getViolations().spliterator(), false))
                                                      .collect(Collectors.toList()))
                .build();
    }
}