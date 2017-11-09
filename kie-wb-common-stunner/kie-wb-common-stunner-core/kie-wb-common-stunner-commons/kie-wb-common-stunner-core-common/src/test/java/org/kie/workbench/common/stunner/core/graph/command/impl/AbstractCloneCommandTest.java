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
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnector;
import org.kie.workbench.common.stunner.core.graph.content.view.ViewConnectorImpl;
import org.kie.workbench.common.stunner.core.util.UUID;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

public abstract class AbstractCloneCommandTest extends AbstractGraphCommandTest {

    @Mock
    protected Node<View, Edge> clone;

    @Mock
    protected Edge cloneEdge;

    @Mock
    protected Element cloneElement;

    @Mock
    protected View candidateContent;

    protected ViewConnector connectorContent;

    @Mock
    protected Object connectorDefinition;

    @Mock
    protected View cloneContent;

    @Mock
    protected Definition definition;

    @Mock
    protected CloneManager cloneManager;

    @Mock
    protected Bounds bounds;

    @Mock
    protected Bounds.Bound bound;

    protected TestingGraphInstanceBuilder.TestGraph3 graphInstance;


    protected MagnetConnection sourceConnection;

    protected MagnetConnection targetConnection;

    protected static final String CLONE_UUID = UUID.uuid();

    protected static final String CLONE_EDGE_UUID = UUID.uuid();

    public void setUp(){
        super.init(0, 0);

        //creating the mock graph for test
        TestingGraphMockHandler handler = new TestingGraphMockHandler();
        graphInstance = TestingGraphInstanceBuilder.newGraph3(handler);
        graph = graphInstance.graph;
        graphIndex = handler.graphIndex;

        //mocking the clone nodes on the graphIndex
        ArgumentCaptor<Node> nodeArgumentCaptor = ArgumentCaptor.forClass(Node.class);
        when(handler.graphIndex.addNode(nodeArgumentCaptor.capture())).thenAnswer(
                t -> {
                    //Node node = (Node)t.getArguments()[0];
                    when(graphIndex.getNode(eq(nodeArgumentCaptor.getValue().getUUID()))).thenReturn(nodeArgumentCaptor.getValue());
                    return graphIndex;
                });

        //edge mock
        connectorContent = new ViewConnectorImpl(connectorDefinition, new BoundsImpl(new BoundImpl(1d, 1d), new BoundImpl(1d, 1d)));
        sourceConnection = MagnetConnection.Builder.forElement(graphInstance.startNode);
        connectorContent.setSourceConnection(sourceConnection);
        targetConnection = MagnetConnection.Builder.forElement(graphInstance.intermNode);
        connectorContent.setTargetConnection(targetConnection);
        graphInstance.edge1.setContent(connectorContent);
        graphInstance.edge2.setContent(connectorContent);

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
    }
}
