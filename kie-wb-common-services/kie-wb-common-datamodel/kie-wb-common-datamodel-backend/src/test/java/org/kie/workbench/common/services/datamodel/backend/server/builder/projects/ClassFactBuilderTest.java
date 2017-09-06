/*
 * Copyright 2015 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
*/

package org.kie.workbench.common.services.datamodel.backend.server.builder.projects;

import org.appformer.project.datamodel.commons.oracle.ProjectDataModelOracleImpl;
import org.appformer.project.datamodel.oracle.TypeSource;
import org.junit.Test;
import org.kie.workbench.common.services.datamodel.backend.server.testclasses.superclasses.PapaSmurf;

import static org.junit.Assert.*;

public class ClassFactBuilderTest {

    @Test
    public void testSuperTypes() throws Exception {
        final ProjectDataModelOracleBuilder builder = ProjectDataModelOracleBuilder.newProjectOracleBuilder();
        final ProjectDataModelOracleImpl oracle = new ProjectDataModelOracleImpl();

        final ClassFactBuilder cb = new ClassFactBuilder(builder,
                PapaSmurf.class,
                false,
                TypeSource.JAVA_PROJECT);
        cb.build(oracle);

        assertEquals(2, oracle.getProjectSuperTypes().get(PapaSmurf.class.getName()).size());
    }
}
