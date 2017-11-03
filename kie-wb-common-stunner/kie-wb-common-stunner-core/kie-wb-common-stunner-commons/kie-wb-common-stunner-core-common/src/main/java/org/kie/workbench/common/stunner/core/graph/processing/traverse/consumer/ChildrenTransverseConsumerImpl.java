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

package org.kie.workbench.common.stunner.core.graph.processing.traverse.consumer;

import java.util.List;
import java.util.function.Consumer;

import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.relationship.Child;
import org.kie.workbench.common.stunner.core.graph.content.view.View;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.AbstractChildrenTraverseCallback;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessor;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.content.ChildrenTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.processing.traverse.tree.TreeWalkTraverseProcessorImpl;
import org.kie.workbench.common.stunner.core.graph.util.GraphUtils;

@Dependent
public class ChildrenTransverseConsumerImpl implements ChildrenTraverseConsumer {

    private final ChildrenTraverseProcessor childrenTraverseProcessor;

    @Inject
    public ChildrenTransverseConsumerImpl(ChildrenTraverseProcessor childrenTraverseProcessor) {
        this.childrenTraverseProcessor = childrenTraverseProcessor;
    }

    @Override
    public void consume(Graph graph, Node<?, ? extends Edge> parent, Consumer<Node<?, ? extends Edge>> nodeConsumer) {
        if (GraphUtils.hasChildren(parent)) {
            new ChildrenTraverseProcessorImpl(new TreeWalkTraverseProcessorImpl())
                    .setRootUUID(parent.getUUID())
                    .traverse(graph, new AbstractChildrenTraverseCallback<Node<View, Edge>, Edge<Child, Node>>() {
                        @Override
                        public boolean startNodeTraversal(final List<Node<View, Edge>> parents,
                                                          final Node<View, Edge> node) {
                            super.startNodeTraversal(parents,
                                                     node);
                            nodeConsumer.accept(node);
                            return true;
                        }
                    });
        }
    }
}
