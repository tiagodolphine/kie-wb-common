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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.jboss.errai.common.client.api.annotations.MapsTo;
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
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessor;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.tree.TreeWalkTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.util.UUID;

import static org.kie.workbench.common.stunner.core.graph.util.GraphUtils.getPosition;

/**
 * A Command to clone a node and set as a child of the given parent.
 */
@Portable
public final class CloneNodeCommand extends AbstractGraphCompositeCommand {

    private final Node<Definition, Edge> candidate;
    private final Optional<String> parentUuidOptional;
    private final Point2D position;
    private Node<View, Edge> clone;
    private Optional<Consumer<Node>> callbackOptional;
    private transient ChildrenTraverseProcessor childrenTraverseProcessor;
    private List<Command<GraphCommandExecutionContext, RuleViolation>> childrenCommands;

    private static Logger LOGGER = Logger.getLogger(CloneNodeCommand.class.getName());

    protected CloneNodeCommand() {
        this(null, null, null, null);
    }

    public CloneNodeCommand(final @MapsTo("candidate") Node candidate, final @MapsTo("parentUuid") String parentUuid) {
        this(PortablePreconditions.checkNotNull("candidate", candidate),
             PortablePreconditions.checkNotNull("parentUuid", parentUuid), null, null);
    }

    public CloneNodeCommand(final Node candidate, final String parentUuid, final Point2D position, final Consumer<Node> callback) {
        this.candidate = candidate;
        this.parentUuidOptional = Optional.ofNullable(parentUuid);
        this.position = position;
        this.callbackOptional = Optional.ofNullable(callback);
        this.childrenTraverseProcessor = new ChildrenTraverseProcessorImpl(new TreeWalkTraverseProcessorImpl());
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
        clone = (Node<View, Edge>) context.getFactoryManager().newElement(UUID.uuid(), bean.getClass()).asNode();

        //Cloning the node content with properties
        Object clonedDefinition = context.getDefinitionManager().cloneManager().clone(candidate.getContent().getDefinition(), ClonePolicy.ALL);
        clone.getContent().setDefinition(clonedDefinition);

        //creating node commands to be executed
        addCommand(new RegisterNodeCommand(clone));
        addCommand(new AddChildNodeCommand(parentUUID.get(), clone, position.getX(), position.getY()));

        return this;
    }

    @Override
    public CommandResult<RuleViolation> execute(final GraphCommandExecutionContext context) {
        CommandResult<RuleViolation> result = super.execute(context);
        if (CommandUtils.isError(result)) {
            return result;
        }

        List<CommandResult<RuleViolation>> commandResults = new ArrayList<>();
        commandResults.add(result);

        //Children cloning process
        childrenCommands = new LinkedList<>();
        final Map<String, Node<View, Edge>> cloneNodeMapUUID = new HashMap<>();

        childrenTraverseProcessor.consume(getGraph(context), candidate, node -> {
            //clone child and map clone uuid
            childrenCommands.add(new CloneNodeCommand(node,
                                                      clone.getUUID(),
                                                      getPosition((View) node.getContent()),
                                                      childClone -> cloneNodeMapUUID.put(node.getUUID(), childClone)
            ));
        });
        commandResults.addAll(childrenCommands.stream().map(c -> c.execute(context)).collect(Collectors.toList()));

        //Cloning connectors process
        //get connector from source node and map to cloned node
        List<CommandResult<RuleViolation>> connectorsResults = cloneNodeMapUUID.keySet().stream()
                .flatMap(sourceUUID -> getNode(context, sourceUUID).getOutEdges().stream())
                .map(edge -> {

                    CloneConnectorCommand cloneConnectorCommand = new CloneConnectorCommand(edge,
                                                                                            cloneNodeMapUUID.get(edge.getSourceNode().getUUID()).getUUID(),
                                                                                            cloneNodeMapUUID.get(edge.getTargetNode().getUUID()).getUUID());
                    childrenCommands.add(cloneConnectorCommand);
                    return cloneConnectorCommand.execute(context);
                }).collect(Collectors.toList());
        commandResults.addAll(connectorsResults);

        //in case of any error than rollback all commands
        CommandResult<RuleViolation> finalResult = buildResult(commandResults);
        if (CommandUtils.isError(finalResult)) {
            undoMultipleExecutedCommands(context, childrenCommands);
            undoMultipleExecutedCommands(context, getCommands());
            return finalResult;
        }

        callbackOptional.ifPresent(callback -> callback.accept(clone));
        LOGGER.info("Node " + candidate.getUUID() + "was cloned successfully to " + clone.getUUID());
        return finalResult;
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
    public CommandResult<RuleViolation> undo(GraphCommandExecutionContext context) {
        return new SafeDeleteNodeCommand(clone).execute(context);
    }

    protected Node<View, Edge> getClone() {
        return clone;
    }

    protected Node<Definition, Edge> getCandidate() {
        return candidate;
    }

    protected List<Command<GraphCommandExecutionContext, RuleViolation>> getChildrenCommands() {
        return childrenCommands;
    }
}