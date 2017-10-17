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

import java.util.Optional;

import org.jboss.errai.common.client.api.annotations.MapsTo;
import org.jboss.errai.common.client.api.annotations.NonPortable;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandExecutionContext;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandResultBuilder;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.util.UUID;

/**
 * A Command to add a node as a child for the main graph instance.
 * It check parent cardinality rules and containment rules as we..
 */
@Portable
public final class CloneNodeCommand extends AbstractGraphCommand {

    private final Node<Definition, Edge> candidate;
    private Optional<CloneNodeCommandCallback> callback;

    @NonPortable
    public interface CloneNodeCommandCallback {
        void success(Node<Definition, Edge> candidate);
    }

    public CloneNodeCommand(final @MapsTo("candidate") Node candidate) {
        //this.candidate = PortablePreconditions.checkNotNull("candidate", candidate);
        this.candidate = candidate;

    }

    public CloneNodeCommand(Node candidate, CloneNodeCommandCallback callback) {
        this.candidate = candidate;
        this.callback = Optional.ofNullable(callback);
    }

    @Override
    protected CommandResult<RuleViolation> check(GraphCommandExecutionContext context) {
        return GraphCommandResultBuilder.SUCCESS;
    }

    @Override
    public CommandResult<RuleViolation> execute(GraphCommandExecutionContext context) {
        final Object bean = candidate.getContent().getDefinition();
        final String beanId = context.getDefinitionManager().adapters().forDefinition().getId(bean);
        final Node node = context.getFactoryManager().newElement(UUID.uuid(), beanId).asNode();

        //TODO: Copy the "name" property value.
        CommandResult<RuleViolation> result = new RegisterNodeCommand(node).execute(context);
        callback.ifPresent(c -> c.success(node));
        return result;
    }

    @Override
    public CommandResult<RuleViolation> undo(GraphCommandExecutionContext context) {
        return GraphCommandResultBuilder.SUCCESS;
    }
}
