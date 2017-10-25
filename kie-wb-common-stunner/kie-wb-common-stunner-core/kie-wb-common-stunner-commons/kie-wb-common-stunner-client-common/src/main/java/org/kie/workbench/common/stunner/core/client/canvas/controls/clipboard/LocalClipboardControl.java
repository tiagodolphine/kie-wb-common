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

package org.kie.workbench.common.stunner.core.client.canvas.controls.clipboard;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;

import org.kie.workbench.common.stunner.core.graph.Element;

@ApplicationScoped
public class LocalClipboardControl implements ClipboardControl<Element> {

    private final Set<Element> elements;

    public LocalClipboardControl() {
        this.elements = new HashSet<>();
    }

    @Override
    public ClipboardControl<Element> add(Element[] element) {
        clear();
        elements.addAll(Arrays.stream(element).collect(Collectors.toSet()));
        return this;
    }

    @Override
    public ClipboardControl<Element> remove(Element[] element) {
        elements.removeAll(Arrays.stream(element).collect(Collectors.toSet()));
        return this;
    }

    @Override
    public Collection<Element> getElements() {
        return elements;
    }

    @Override
    public ClipboardControl<Element> clear() {
        elements.clear();
        return this;
    }

    @Override
    public boolean hasElements() {
        return !elements.isEmpty();
    }
}