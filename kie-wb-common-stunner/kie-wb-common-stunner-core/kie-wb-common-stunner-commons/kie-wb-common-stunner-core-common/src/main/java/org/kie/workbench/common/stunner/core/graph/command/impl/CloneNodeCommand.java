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
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import org.jboss.errai.common.client.api.annotations.MapsTo;
import org.jboss.errai.common.client.api.annotations.NonPortable;
import org.jboss.errai.common.client.api.annotations.Portable;
import org.kie.soup.commons.validation.PortablePreconditions;
import org.kie.workbench.common.stunner.core.command.Command;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.impl.AbstractCompositeCommand;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.definition.clone.ClonePolicy;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.command.GraphCommandExecutionContext;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.util.UUID;

/**
 * A Command to clone a node and add as a child of the given parent.
 */
@Portable
public final class CloneNodeCommand extends AbstractGraphCompositeCommand {

    private final Node<Definition, Edge> candidate;
    private final Optional<String> parentUuidOptional;
    private final Point2D position;
    private Node<Definition, Edge> clone;
    private Optional<CloneNodeCommandCallback> callbackOptional;

    private static Logger LOGGER = Logger.getLogger(CloneNodeCommand.class.getName());

    /**
     * Callback interface to be used whether it is necessary to receive the cloned node after
     * the {{@link CloneNodeCommand#execute(GraphCommandExecutionContext)}} is called.
     */
    @NonPortable
    public interface CloneNodeCommandCallback {

        void cloned(Node<Definition, Edge> candidate);
    }

    protected CloneNodeCommand() {
        this(null, null, null, null);
    }

    public CloneNodeCommand(final @MapsTo("candidate") Node candidate, final @MapsTo("parentUuid") String parentUuid) {
        this(PortablePreconditions.checkNotNull("candidate", candidate),
             PortablePreconditions.checkNotNull("parentUuid", parentUuid), null, null);
    }

    public CloneNodeCommand(final Node candidate, final String parentUuid, final CloneNodeCommandCallback callback, final Point2D position) {
        this.candidate = candidate;
        this.parentUuidOptional = Optional.ofNullable(parentUuid);
        this.callbackOptional = Optional.ofNullable(callback);
        this.position = position;
    }

    @Override
    @SuppressWarnings("unchecked")
    public CommandResult<RuleViolation> allow(final GraphCommandExecutionContext context) {
        return super.allow(context);
    }

    @Override
    protected boolean delegateRulesContextToChildren() {
        return false;
    }

    @Override
    protected AbstractCompositeCommand<GraphCommandExecutionContext, RuleViolation> initialize(GraphCommandExecutionContext context) {

        //getting the node parent
        Optional<String> parentUUID = getParentUUID();
        if (!parentUUID.isPresent()) {
            throw new IllegalStateException("Parent not found for node " + candidate);
        }

        final Object bean = candidate.getContent().getDefinition();
        clone = (Node<Definition, Edge>) context.getFactoryManager().newElement(UUID.uuid(), bean.getClass()).asNode();

        //Cloning the node content with properties
        Object clonedDefinition = context.getDefinitionManager().cloneManager().clone(candidate.getContent().getDefinition(), ClonePolicy.ALL);
        clone.getContent().setDefinition(clonedDefinition);

        //creating node commands to be executed
        addCommand(new RegisterNodeCommand(clone));
        addCommand(new AddChildNodeCommand(parentUUID.get(), clone, position.getX(), position.getY()));

        return this;
    }

    @Override
    protected CommandResult<RuleViolation> doExecute(GraphCommandExecutionContext context, Command<GraphCommandExecutionContext, RuleViolation> command) {
        GWT.log("run canvas clone "+ candidate.getUUID());

        return super.doExecute(context, command);
    }

    private Optional<String> getParentUUID() {
        return parentUuidOptional.isPresent() ? parentUuidOptional : getDefaultParent();
    }

    private Optional<String> getDefaultParent() {
        Optional<? extends Element<?>> parent = Optional.ofNullable(GraphUtils.getParent(candidate));
        if (parent.isPresent()) {
            return Optional.of(parent.get().getUUID());
        }
        return Optional.empty();
    }

    @Override
    public CommandResult<RuleViolation> execute(GraphCommandExecutionContext context) {
        CommandResult<RuleViolation> result = super.execute(context);
        callbackOptional.ifPresent(callback -> {
            if (!CommandUtils.isError(result)) {
                callback.cloned(clone);
                LOGGER.info("Node " + candidate.getUUID() + "was cloned successfully to " + clone.getUUID());
            }
        });
        return result;
    }

    @Override
    public CommandResult<RuleViolation> undo(GraphCommandExecutionContext context) {
        //return new SafeDeleteNodeCommand(clone).execute(context);
        return super.undo(context);
    }
}