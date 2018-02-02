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

package org.kie.workbench.common.stunner.bpmn.backend.marshall.json.builder;

import java.util.Objects;
import java.util.stream.Collectors;

import org.kie.workbench.common.stunner.bpmn.backend.marshall.json.oryx.Bpmn2OryxManager;
import org.kie.workbench.common.stunner.bpmn.definition.BPMNDefinition;
import org.kie.workbench.common.stunner.core.api.FactoryManager;
import org.kie.workbench.common.stunner.core.command.CommandResult;
import org.kie.workbench.common.stunner.core.command.util.CommandUtils;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.command.impl.AddControlPointCommand;
import org.kie.workbench.common.stunner.core.graph.command.impl.AddNodeCommand;
import org.kie.workbench.common.stunner.core.graph.command.impl.GraphCommandFactory;
import org.kie.workbench.common.stunner.core.graph.command.impl.SetConnectionTargetNodeCommand;
import org.kie.workbench.common.stunner.core.graph.content.view.Connection;
import org.kie.workbench.common.stunner.core.graph.content.view.ControlPoint;
import org.kie.workbench.common.stunner.core.graph.content.view.ControlPointImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.MagnetConnection;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.processing.index.MutableIndex;
import org.kie.workbench.common.stunner.core.rule.RuleViolation;
import org.kie.workbench.common.stunner.core.util.Counter;

// TODO: Improve error handling.
public abstract class AbstractEdgeBuilder<W, T extends Edge<View<W>, Node>>
        extends AbstractObjectBuilder<W, T> implements EdgeObjectBuilder<W, T> {

    protected final Class<?> definitionClass;

    public AbstractEdgeBuilder(final Class<?> definitionClass) {
        this.definitionClass = definitionClass;
    }

    @Override
    public Class<?> getDefinitionClass() {
        return definitionClass;
    }

    public boolean isSourceAutoConnection() {
        return isAutoConnection(Bpmn2OryxManager.SOURCE);
    }

    public boolean isTargetAutoConnection() {
        return isAutoConnection(Bpmn2OryxManager.TARGET);
    }

    @Override
    public String toString() {
        return new StringBuilder(super.toString()).append(" [defClass=").append(definitionClass.getName()).append("] ").toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected T doBuild(final BuilderContext context) {
        if (context.getIndex().getEdge(this.nodeId) == null) {
            FactoryManager factoryManager = context.getFactoryManager();
            String definitionId = context.getOryxManager().getMappingsManager().getDefinitionId(definitionClass);
            T result = (T) factoryManager.newElement(this.nodeId,
                                                     definitionId);
            setProperties(context,
                          (BPMNDefinition) result.getContent().getDefinition());
            addEdgeIntoIndex(context,
                             result);
            afterEdgeBuild(context,
                           result);
            return result;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    protected void afterEdgeBuild(final BuilderContext context,
                                  final T edge) {
        // Outgoing connections.
        if (outgoingResourceIds != null && !outgoingResourceIds.isEmpty()) {
            for (String outgoingNodeId : outgoingResourceIds) {
                GraphObjectBuilder<?, ?> outgoingNodeBuilder = getBuilder(context,
                                                                          outgoingNodeId);
                if (outgoingNodeBuilder == null) {
                    throw new RuntimeException("No edge for " + outgoingNodeId);
                }
                Node node = (Node) outgoingNodeBuilder.build(context);
                // Command - Add the node into the graph store.
                GraphCommandFactory commandFactory = context.getCommandFactory();
                AddNodeCommand addNodeCommand = commandFactory.addNode(node);
                // Command - Set the edge connection's target node.
                Double targetDocker[] = null;
                if (dockers != null && dockers.size() > 1) {
                    targetDocker = dockers.get(dockers.size() - 1);
                }

                Connection targetConnection = null;
                if (null != targetDocker) {
                    targetConnection = MagnetConnection.Builder
                            .at(targetDocker[0],
                                targetDocker[1])
                            .setAuto(isTargetAutoConnection());
                }

                SetConnectionTargetNodeCommand setTargetNodeCommand = commandFactory.setTargetNode(node,
                                                                                                                edge,
                                                                                                                targetConnection);
                CommandResult<RuleViolation> results1 = context.execute(addNodeCommand);
                if (hasErrors(results1)) {
                    throw new RuntimeException("Error building BPMN graph. Command 'addNodeCommand' execution failed.");
                }
                CommandResult<RuleViolation> results2 = context.execute(setTargetNodeCommand);
                if (hasErrors(results2)) {
                    throw new RuntimeException("Error building BPMN graph. Command 'SetConnectionTargetNodeCommand' execution failed.");
                }

                //ControlPoints
                if(dockers.size()>2) {
                    Counter indexCounter = new Counter(0);
                    ControlPoint[] controlPoints = dockers.subList(1, dockers.size() - 1).stream()
                            .sequential()
                            .map(docker -> Objects.equals(docker.length, 2) ? new Point2D(docker[0], docker[1]) : null)
                            .filter(Objects::nonNull)
                            .map(point -> new ControlPointImpl(point, indexCounter.increment()))
                            .toArray(ControlPoint[]::new);

                    CommandResult<RuleViolation> addControlPointsResult = context.execute(commandFactory.addControlPoint(edge, controlPoints));
                    if (hasErrors(addControlPointsResult)) {
                        throw new RuntimeException("Error building BPMN graph. Command 'AddControlPointCommand' execution failed." + addControlPointsResult);
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void addEdgeIntoIndex(final BuilderContext context,
                                    final T edge) {
        MutableIndex<Node, Edge> index = (MutableIndex<Node, Edge>) context.getIndex();
        index.addEdge(edge);
    }

    private boolean isAutoConnection(String type) {
        String autoRaw = properties.get(Bpmn2OryxManager.MAGNET_AUTO_CONNECTION + type);
        return null != autoRaw && Boolean.TRUE.equals(Boolean.parseBoolean(autoRaw));
    }
}
