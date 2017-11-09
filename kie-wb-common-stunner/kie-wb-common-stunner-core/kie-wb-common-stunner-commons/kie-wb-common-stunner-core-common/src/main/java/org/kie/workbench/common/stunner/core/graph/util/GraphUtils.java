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

package org.kie.workbench.common.stunner.core.graph.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;

import org.kie.workbench.common.stunner.core.api.DefinitionManager;
import org.kie.workbench.common.stunner.core.graph.Edge;
import org.kie.workbench.common.stunner.core.graph.Element;
import org.kie.workbench.common.stunner.core.graph.Graph;
import org.kie.workbench.common.stunner.core.graph.Node;
import org.kie.workbench.common.stunner.core.graph.content.Bounds;
import org.kie.workbench.common.stunner.core.graph.content.definition.Definition;
import org.kie.workbench.common.stunner.core.graph.content.definition.DefinitionSet;
import org.kie.workbench.common.stunner.core.graph.content.relationship.Child;
import org.kie.workbench.common.stunner.core.graph.content.view.Point2D;
import org.kie.workbench.common.stunner.core.graph.content.view.View;

import static org.kie.soup.commons.validation.PortablePreconditions.checkNotNull;

public class GraphUtils {

    public static Object getProperty(final DefinitionManager definitionManager,
                                     final Element<? extends Definition> element,
                                     final String id) {
        if (null != element) {
            final Object def = element.getContent().getDefinition();
            final Set<?> properties = definitionManager.adapters().forDefinition().getProperties(def);
            return getProperty(definitionManager,
                               properties,
                               id);
        }
        return null;
    }

    public static Object getProperty(final DefinitionManager definitionManager,
                                     final Set<?> properties,
                                     final String id) {
        if (null != id && null != properties) {
            for (final Object property : properties) {
                final String pId = definitionManager.adapters().forProperty().getId(property);
                if (pId.equals(id)) {
                    return property;
                }
            }
        }
        return null;
    }

    public static int countDefinitionsById(final DefinitionManager definitionManager,
                                           final Graph<?, ? extends Node> target,
                                           final String id) {
        final int[] count = {0};
        target.nodes().forEach(node -> {
            if (getElementDefinitionId(definitionManager,
                                       node).equals(id)) {
                count[0]++;
            }
        });
        return count[0];
    }

    public static <T> int countDefinitions(final DefinitionManager definitionManager,
                                           final Graph<?, ? extends Node> target,
                                           final T definition) {
        final String id = definitionManager.adapters().forDefinition().getId(definition);
        return countDefinitionsById(definitionManager,
                                    target,
                                    id);
    }

    public static int countEdges(final DefinitionManager definitionManager,
                                 final String edgeId,
                                 final List<? extends Edge> edges) {
        final int[] count = {0};
        if (null != edgeId
                && null != edges
                && !edges.isEmpty()) {
            edges.stream().forEach(edge -> {
                final String eId = getElementDefinitionId(definitionManager,
                                                          edge);
                if (null != eId && edgeId.equals(eId)) {
                    count[0]++;
                }
            });
        }
        return count[0];
    }

    /**
     * Does not returns labels not being used on the graph,
     * even if included in the <code>filter</code>.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Integer> getLabelsCount(final Graph<?, ? extends Node> target,
                                                      final Set<String> filter) {
        final Map<String, Integer> labels = new LinkedHashMap<>();
        target.nodes().forEach(node -> {
            final Set<String> nodeRoles = node.getLabels();
            if (null != nodeRoles) {
                nodeRoles
                        .stream()
                        .filter(role -> null == filter || filter.contains(role))
                        .forEach(role -> {
                            final Integer i = labels.get(role);
                            labels.put(role,
                                       null != i ? i + 1 : 1);
                        });
            }
        });
        return labels;
    }

    @SuppressWarnings("unchecked")
    public static List<String> getParentIds(final DefinitionManager definitionManager,
                                            final Graph<? extends DefinitionSet, ? extends Node> graph,
                                            final Element<?> element) {
        final List<String> result = new ArrayList<>(5);
        Element<?> p = element;
        while (p instanceof Node && p.getContent() instanceof Definition) {
            p = getParent((Node<? extends Definition<?>, ? extends Edge>) p);
            if (null != p) {
                final Object definition = ((Definition) p.getContent()).getDefinition();
                final String id = definitionManager.adapters().forDefinition().getId(definition);
                result.add(id);
            }
        }
        final String graphId = graph.getContent().getDefinition();
        result.add(graphId);
        return result;
    }

    @SuppressWarnings("unchecked")
    public static Element<?> getParent(final Node<?, ? extends Edge> element) {
        return element.getInEdges().stream()
                .filter(e -> e.getContent() instanceof Child)
                .findAny()
                .map(Edge::getSourceNode)
                .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public static Optional<Element<?>> getParentByDefinitionId(final DefinitionManager definitionManager,
                                                               final Node<?, ? extends Edge> candidate,
                                                               final String parentDefId) {
        checkNotNull("candidate",
                     candidate);
        checkNotNull("parentDefId",
                     parentDefId);
        Element<?> p = getParent(candidate);
        while (p instanceof Node
                && p.getContent() instanceof Definition) {
            final String cID = getElementDefinitionId(definitionManager,
                                                      p);
            if (parentDefId.equals(cID)) {
                return Optional.of(p);
            }
            p = getParent((Node<?, ? extends Edge>) p);
        }
        return Optional.empty();
    }

    public static class HasParentPredicate implements BiPredicate<Node<?, ? extends Edge>, Element<?>> {

        @Override
        public boolean test(final Node<?, ? extends Edge> candidate,
                            final Element<?> parent) {
            if (null != candidate) {
                Element<?> p = getParent(candidate);
                while (p instanceof Node && !p.equals(parent)) {
                    p = getParent((Node<?, ? extends Edge>) p);
                }
                return null != p;
            }
            return false;
        }
    }

    public static Point2D getPosition(final View element) {
        final Bounds.Bound ul = element.getBounds().getUpperLeft();
        final double x = ul.getX();
        final double y = ul.getY();
        return new Point2D(x,
                           y);
    }

    public static double[] getGraphSize(final DefinitionSet element) {
        final Bounds.Bound ul = element.getBounds().getUpperLeft();
        final Bounds.Bound lr = element.getBounds().getLowerRight();
        final double w = lr.getX() - ul.getX();
        final double h = lr.getY() - ul.getY();
        return new double[]{Math.abs(w), Math.abs(h)};
    }

    public static double[] getNodeSize(final View element) {
        final Bounds.Bound ul = element.getBounds().getUpperLeft();
        final Bounds.Bound lr = element.getBounds().getLowerRight();
        final double w = lr.getX() - ul.getX();
        final double h = lr.getY() - ul.getY();
        return new double[]{Math.abs(w), Math.abs(h)};
    }

    /**
     * Checks that the given Bounds do not exceed graph limits.
     *
     * @return if bounds exceed graph limits it returns <code>false</code>. Otherwise returns <code>true</code>.
     */
    @SuppressWarnings("unchecked")
    public static boolean checkBoundsExceeded(final Graph<DefinitionSet, ? extends Node> graph,
                                              final Bounds bounds) {
        final Bounds graphBounds = graph.getContent().getBounds();
        if ((bounds.getLowerRight().getX() > graphBounds.getLowerRight().getX())
                || (bounds.getLowerRight().getY() > graphBounds.getLowerRight().getY())) {
            return false;
        }
        return true;
    }

    /**
     * Finds the first node in the graph structure for the given type.
     *
     * @param graph The graph structure.
     * @param type  The Definition type..
     */
    @SuppressWarnings("unchecked")
    public static <C> Node<Definition<C>, ?> getFirstNode(final Graph<?, Node> graph,
                                                          final Class<?> type) {
        if (null != graph) {
            for (final Node node : graph.nodes()) {
                final Object content = node.getContent();
                try {
                    final Definition definitionContent = (Definition) content;
                    if (instanceOf(definitionContent.getDefinition(),
                                   type)) {
                        return node;
                    }
                } catch (final ClassCastException e) {
                    // Node content does not contains a definition.
                }
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public static boolean hasChildren(final Node<?, ? extends Edge> element) {
        final List<? extends Edge> outEdges = element.getOutEdges();
        if (null != outEdges) {
            return
                    outEdges.stream()
                            .filter(edge -> (edge.getContent() instanceof Child))
                            .findAny()
                            .isPresent();
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    public static Long countChildren(final Node<?, ? extends Edge> element) {
        return element.getOutEdges().stream()
                .filter(edge -> (edge.getContent() instanceof Child)).count();
    }

    private static String getElementDefinitionId(final DefinitionManager definitionManager,
                                                 final Element<?> element) {
        String targetId = null;
        if (element.getContent() instanceof Definition) {
            final Object definition = ((Definition) element.getContent()).getDefinition();
            targetId = definitionManager.adapters().forDefinition().getId(definition);
        } else if (element.getContent() instanceof DefinitionSet) {
            targetId = ((DefinitionSet) element.getContent()).getDefinition();
        }
        return targetId;
    }

    private static boolean instanceOf(final Object item,
                                      final Class<?> clazz) {
        return null != item && item.getClass().equals(clazz);
    }
}
