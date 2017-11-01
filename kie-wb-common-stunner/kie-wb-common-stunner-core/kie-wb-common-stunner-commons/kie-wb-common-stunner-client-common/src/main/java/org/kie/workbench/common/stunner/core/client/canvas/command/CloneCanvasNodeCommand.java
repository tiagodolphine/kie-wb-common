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

import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.core.client.GWT;
import org.kie.workbench.common.stunner.core.client.canvas.AbstractCanvasHandler;
import org.kie.workbench.common.stunner.core.client.command.CanvasViolation;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.CompositeCommand;
import org.kie.workbench.common.stunner.core.command.impl.CompositeCommandImpl;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.relationship.Child;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.AbstractChildrenTraverseCallback;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.tree.TreeWalkTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;

/**
 * Clone a node shape into de canvas.
 */
public class CloneCanvasNodeCommand extends AddCanvasChildNodeCommand {

    private transient CompositeCommand<AbstractCanvasHandler, CanvasViolation> commands;

    public CloneCanvasNodeCommand(Node parent, Node candidate, String shapeSetId) {
        super(parent, candidate, shapeSetId);
        this.commands = new CompositeCommandImpl.CompositeCommandBuilder<AbstractCanvasHandler, CanvasViolation>()
                .reverse()
                .build();
    }

    @Override
    public CommandResult<CanvasViolation> execute(AbstractCanvasHandler context) {
        CommandResult<CanvasViolation> rootResult = super.execute(context);

        if (CommandUtils.isError(rootResult)) {
            return rootResult;
        }

        process(getCandidate(), context.getGraphIndex().getGraph(), node -> commands.addCommand(new CloneCanvasNodeCommand(getCandidate(), node, getShapeSetId())));
        return commands.execute(context);
    }

    private void process(Node<?, ? extends Edge> candidate, Graph graph, Consumer<Node> nodeConsumer) {
        if (GraphUtils.hasChildren(candidate)) {
            new ChildrenTraverseProcessorImpl(new TreeWalkTraverseProcessorImpl())
                    .setRootUUID(candidate.getUUID())
                    .traverse(graph, new AbstractChildrenTraverseCallback<Node<View, Edge>, Edge<Child, Node>>() {
                        @Override
                        public boolean startNodeTraversal(final List<Node<View, Edge>> parents,
                                                          final Node<View, Edge> node) {
                            super.startNodeTraversal(parents,
                                                     node);

                            GWT.log("startNodeTraversal-LIST " + node.getUUID());

                            nodeConsumer.accept(node);
                            return true;
                        }
                    });
        }
    }
}
