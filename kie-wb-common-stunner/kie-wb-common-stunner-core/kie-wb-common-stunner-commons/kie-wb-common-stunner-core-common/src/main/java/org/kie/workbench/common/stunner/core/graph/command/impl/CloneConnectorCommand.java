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
package org.kie.workbench.common.stunner.core.graph.command.impl;

import org.jboss.errai.common.client.api.annotations.Portable;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.definition.clone.ClonePolicy;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandExecutionContext;
import org.kie.workbench.common.stunner.core.graph.content.view.Connection;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnector;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.util.UUID;

/**
 * A Command which adds an candidate into a graph and sets its target sourceNode.
 */
@Portable
public final class CloneConnectorCommand extends AbstractGraphCompositeCommand {

    private final Edge candidate;
    private transient Edge clone;
    private transient Connection sourceConnection;
    private transient Connection targetConnection;
    private transient Node<? extends View<?>, Edge> sourceNode;
    private transient Node<? extends View<?>, Edge> targetNode;
    private final String sourceNodeUUID;
    private final String targetNodeUUID;

    public CloneConnectorCommand() {
        this(null, null, null);
    }

    public CloneConnectorCommand(final Edge candidate, final String sourceNodeUUID, final String targetNodeUUID) {
        this.candidate = PortablePreconditions.checkNotNull("candidate",
                                                            candidate);
        this.sourceNodeUUID = sourceNodeUUID;
        this.targetNodeUUID = targetNodeUUID;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected CloneConnectorCommand initialize(final GraphCommandExecutionContext context) {
        super.initialize(context);

        this.sourceNode = (Node<? extends View<?>, Edge>) getNode(context, sourceNodeUUID).asNode();
        this.targetNode = (Node<? extends View<?>, Edge>) getNode(context, targetNodeUUID).asNode();

        //clone candidate
        ViewConnector edgeContent = (ViewConnector) candidate.getContent();
        final Object bean = edgeContent.getDefinition();
        clone = context.getFactoryManager().newElement(UUID.uuid(), bean.getClass()).asEdge();

        //Cloning the candidate content with properties
        Object clonedDefinition = context.getDefinitionManager().cloneManager().clone(edgeContent.getDefinition(), ClonePolicy.ALL);
        ViewConnector clonedContent = (ViewConnector) clone.getContent();
        clonedContent.setDefinition(clonedDefinition);

        // Magnet being moved on node
        ViewConnector connectionContent = (ViewConnector) candidate.getContent();
        this.sourceConnection = (Connection) connectionContent.getSourceConnection().orElse(null);
        this.targetConnection = (Connection) connectionContent.getTargetConnection().orElse(null);

        commands.add(new AddConnectorCommand(sourceNode, clone, sourceConnection));
        commands.add(new SetConnectionTargetNodeCommand(targetNode, clone, targetConnection));

        return this;
    }

    @Override
    public CommandResult<RuleViolation> allow(final GraphCommandExecutionContext context) {
        // Add the candidate into index, so child commands can find it.
        getMutableIndex(context).addEdge(candidate);
        final CommandResult<RuleViolation> results = super.allow(context);
        if (CommandUtils.isError(results)) {
            // Remove the transient candidate after the error.
            getMutableIndex(context).removeEdge(candidate);
        }
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResult<RuleViolation> execute(final GraphCommandExecutionContext context) {
        // Add the candidate into index, so child commands can find it.
        getMutableIndex(context).addEdge(candidate);
        final CommandResult<RuleViolation> results = super.execute(context);
        if (CommandUtils.isError(results)) {
            // Remove the transient candidate after the error.
            getMutableIndex(context).removeEdge(candidate);
        }
        return results;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResult<RuleViolation> undo(final GraphCommandExecutionContext context) {
        final DeleteConnectorCommand undoCommand = new DeleteConnectorCommand(candidate);
        return undoCommand.execute(context);
    }

    public Edge getCandidate() {
        return candidate;
    }

    public Connection getSourceConnection() {
        return sourceConnection;
    }

    public Node<?, Edge> getSourceNode() {
        return sourceNode;
    }

    @Override
    protected boolean delegateRulesContextToChildren() {
        return true;
    }
}