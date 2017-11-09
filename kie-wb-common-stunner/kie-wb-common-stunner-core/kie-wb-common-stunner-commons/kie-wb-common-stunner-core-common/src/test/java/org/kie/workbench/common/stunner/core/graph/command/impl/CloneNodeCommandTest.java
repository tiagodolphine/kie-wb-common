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

import java.util.Objects;
import java.util.function.Function;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.TestingGraphInstanceBuilder;
import org.kie.workbench.common.stunner.core.TestingGraphMockHandler;
import org.kie.workbench.common.stunner.core.definition.clone.CloneManager;
import org.kie.workbench.common.stunner.core.definition.clone.ClonePolicy;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.Bounds;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.view.BoundImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.BoundsImpl;
import org.kie.workbench.common.stunner.core.graph.content.view.MagnetConnection;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnector;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnectorImpl;
import org.kie.workbench.common.stunner.core.util.UUID;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloneNodeCommandTest extends AbstractGraphCommandTest {

    private CloneNodeCommand cloneNodeCommand;

    private Node<View, Edge> candidate;

    private Node<View, Edge> parent;

    @Mock
    private Node<View, Edge> clone;

    @Mock
    private Edge cloneEdge;

    @Mock
    private Element cloneElement;

    @Mock
    private View candidateContent;

    private ViewConnector connectorContent;

    @Mock
    private Object connectorDefinition;

    @Mock
    private View cloneContent;

    @Mock
    private Definition definition;

    @Mock
    private CloneManager cloneManager;

    @Mock
    private Bounds bounds;

    @Mock
    private Bounds.Bound bound;

    private Point2D position;

    private TestingGraphInstanceBuilder.TestGraph3 graphInstance;

    private static final String CLONE_UUID = UUID.uuid();

    private static final String CLONE_EDGE_UUID = UUID.uuid();

    @Before
    public void setUp() throws Exception {
        super.init(0, 0);

        //creating the mock graph for test
        TestingGraphMockHandler handler = new TestingGraphMockHandler();
        graphInstance = TestingGraphInstanceBuilder.newGraph3(handler);
        graph = graphInstance.graph;
        candidate = graphInstance.containerNode;
        parent = graphInstance.parentNode;
        candidate.setContent(candidateContent);
        graphIndex = handler.graphIndex;

        //edge mock
        connectorContent = new ViewConnectorImpl(connectorDefinition, new BoundsImpl(new BoundImpl(1d, 1d), new BoundImpl(1d, 1d)));
        connectorContent.setSourceConnection(MagnetConnection.Builder.forElement(graphInstance.startNode));
        connectorContent.setTargetConnection(MagnetConnection.Builder.forElement(graphInstance.intermNode));
        graphInstance.edge1.setContent(connectorContent);
        graphInstance.edge2.setContent(connectorContent);

        //mocking the clone nodes on the graphIndex
        ArgumentCaptor<Node> nodeArgumentCaptor = ArgumentCaptor.forClass(Node.class);
        when(handler.graphIndex.addNode(nodeArgumentCaptor.capture())).thenAnswer(
                t -> {
                    //Node node = (Node)t.getArguments()[0];
                    when(graphIndex.getNode(eq(nodeArgumentCaptor.getValue().getUUID()))).thenReturn(nodeArgumentCaptor.getValue());
                    return graphIndex;
                });

        when(definitionManager.cloneManager()).thenReturn(cloneManager);
        when(cloneManager.clone(definition, ClonePolicy.ALL)).thenReturn(definition);
        when(cloneManager.clone(connectorDefinition, ClonePolicy.ALL)).thenReturn(connectorDefinition);
        when(graphCommandExecutionContext.getGraphIndex()).thenReturn(graphIndex);
        when(candidateContent.getDefinition()).thenReturn(definition);
        when(factoryManager.newElement(anyString(), any(Class.class))).thenReturn(cloneElement);
        when(cloneElement.asNode()).thenReturn(clone);
        when(cloneElement.asEdge()).thenReturn(cloneEdge);
        when(cloneEdge.getContent()).thenReturn(connectorContent);
        when(cloneEdge.getUUID()).thenReturn(CLONE_EDGE_UUID);
        when(clone.getContent()).thenReturn(cloneContent);
        when(clone.getUUID()).thenReturn(CLONE_UUID);
        when(cloneElement.getUUID()).thenReturn(CLONE_UUID);
        when(cloneContent.getBounds()).thenReturn(bounds);
        when(bounds.getUpperLeft()).thenReturn(bound);
        when(bounds.getLowerRight()).thenReturn(bound);

        this.position = new Point2D(1, 1);
        this.cloneNodeCommand = new CloneNodeCommand(candidate, parent.getUUID(), position, null);
    }

    @Test
    public void testInitialize() throws Exception {

        cloneNodeCommand.initialize(graphCommandExecutionContext);

        Node<View, Edge> clone = cloneNodeCommand.getClone();
        assertEquals(clone, this.clone);

        RegisterNodeCommand registerNodeCommand = (RegisterNodeCommand) cloneNodeCommand.getCommands().stream().filter(command -> command instanceof RegisterNodeCommand).findFirst().get();
        assertNotNull(registerNodeCommand);
        assertEquals(registerNodeCommand.getCandidate(), clone);

        AddChildNodeCommand addChildCommand = (AddChildNodeCommand) cloneNodeCommand.getCommands().stream().filter(command -> command instanceof AddChildNodeCommand).findFirst().get();
        assertNotNull(addChildCommand);
        assertEquals(addChildCommand.getCandidate(), clone);
        assertEquals(addChildCommand.getParent(graphCommandExecutionContext), parent);
        assertEquals(addChildCommand.getX(), position.getX(), 0);
        assertEquals(addChildCommand.getY(), position.getY(), 0);
    }

    @Test
    public void testExecute() {
        cloneNodeCommand.execute(graphCommandExecutionContext);

        Function<Object, Boolean> cloneNodeInstanceFunction = command -> command instanceof CloneNodeCommand;
        assertTrue(checkCloneChildrenElement(cloneNodeInstanceFunction, getCheckChildrenNodeFunction(graphInstance.startNode)));
        assertTrue(checkCloneChildrenElement(cloneNodeInstanceFunction, getCheckChildrenNodeFunction(graphInstance.intermNode)));
        assertTrue(checkCloneChildrenElement(cloneNodeInstanceFunction, getCheckChildrenNodeFunction(graphInstance.endNode)));

        Function<Object, Boolean> cloneConnectorInstanceFunction = command -> command instanceof CloneConnectorCommand;
        assertTrue(checkCloneChildrenElement(cloneConnectorInstanceFunction, getCheckChildrenEdgeFunction(graphInstance.edge1)));
        assertTrue(checkCloneChildrenElement(cloneConnectorInstanceFunction, getCheckChildrenEdgeFunction(graphInstance.edge2)));
    }

    private Function<CloneNodeCommand, Boolean> getCheckChildrenNodeFunction(Node node) {
        return command -> Objects.equals(command.getCandidate(), node);
    }

    private Function<CloneConnectorCommand, Boolean> getCheckChildrenEdgeFunction(Edge edge) {
        return command -> Objects.equals(command.getCandidate(), edge);
    }

    private <T> boolean checkCloneChildrenElement(Function<Object, Boolean> commandFilter, Function<T, Boolean> candidateFilter) {
        return cloneNodeCommand.getChildrenCommands().stream()
                .filter(command -> commandFilter.apply(command))
                .map(command -> (T) command)
                .filter(command -> candidateFilter.apply(command))
                .findFirst()
                .isPresent();
    }

    @Test
    public void testDelegateRulesContextToChildren() throws Exception {
        assertFalse(cloneNodeCommand.delegateRulesContextToChildren());
    }

    @Test
    public void testUndo() throws Exception {
        testInitialize();
        cloneNodeCommand.undo(graphCommandExecutionContext);
        verify(graphIndex, times(1)).removeNode(clone);
    }
}