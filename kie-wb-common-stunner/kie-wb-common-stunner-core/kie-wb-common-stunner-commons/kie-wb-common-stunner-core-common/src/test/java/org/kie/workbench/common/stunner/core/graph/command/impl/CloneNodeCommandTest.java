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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.util.UUID;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CloneNodeCommandTest extends AbstractGraphCommandTest{

    private CloneNodeCommand cloneNodeCommand;

    @Mock
    private Node candidate;

    @Mock
    private Node clone;

    @Mock
    private Element cloneElement;

    @Mock
    private View candidateContent;

    @Mock
    private Definition definition;

    private static final String NODE_UUID = UUID.uuid();

    private static final String NODE_DEF_ID = UUID.uuid();

    @Before
    public void setUp() throws Exception {
        super.init(0,0);

        when(candidate.getContent()).thenReturn(candidateContent);
        when(candidateContent.getDefinition()).thenReturn(definition);
        when(graphIndex.getNode(eq(NODE_UUID))).thenReturn(candidate);

        when(factoryManager.newElement(anyString(), eq(NODE_DEF_ID))).thenReturn(cloneElement);
        when(definitionAdapter.getId(definition)).thenReturn(NODE_DEF_ID);
        when(cloneElement.asNode()).thenReturn(clone);

        this.cloneNodeCommand = new CloneNodeCommand(candidate);
    }

    @Test
    public void testInitialize() throws Exception {
//        cloneNodeCommand.initialize(graphCommandExecutionContext);
    }

    @Test
    public void testDelegateRulesContextToChildren() throws Exception {
//        assertFalse(cloneNodeCommand.delegateRulesContextToChildren());
    }

    @Test
    public void testAllow() throws Exception {
        assertFalse(cloneNodeCommand.allow(graphCommandExecutionContext).getViolations().iterator().hasNext());
    }

    @Test
    public void testUndo() throws Exception {
    }
}