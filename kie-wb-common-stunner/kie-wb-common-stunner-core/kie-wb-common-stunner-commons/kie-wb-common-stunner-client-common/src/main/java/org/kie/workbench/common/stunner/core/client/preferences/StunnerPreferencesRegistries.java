/*
 * Copyright 2018 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.stunner.core.client.preferences;

import java.lang.annotation.Annotation;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Any;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.api.ManagedInstance;
import org.kie.workbench.common.stunner.core.client.session.impl.InstanceUtils;
import org.kie.workbench.common.stunner.core.preferences.StunnerPreferences;
import org.kie.workbench.common.stunner.core.util.DefinitionUtils;

@ApplicationScoped
public class StunnerPreferencesRegistries {

    private final DefinitionUtils definitionUtils;
    private final ManagedInstance<StunnerPreferencesRegistry> preferencesRegistries;

    @Inject
    public StunnerPreferencesRegistries(final DefinitionUtils definitionUtils,
                                        final @Any ManagedInstance<StunnerPreferencesRegistry> preferencesRegistries) {
        this.definitionUtils = definitionUtils;
        this.preferencesRegistries = preferencesRegistries;
    }

    public StunnerPreferences get(final String definitionSetId) {
        final Annotation qualifier = definitionUtils.getQualifier(definitionSetId);
        return get(qualifier);
    }

    public StunnerPreferences get(final Annotation qualifier) {
        return InstanceUtils
                .lookup(preferencesRegistries,
                        qualifier)
                .get();
    }
}
