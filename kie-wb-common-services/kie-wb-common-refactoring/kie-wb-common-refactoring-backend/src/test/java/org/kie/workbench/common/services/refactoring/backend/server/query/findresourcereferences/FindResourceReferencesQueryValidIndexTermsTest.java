/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates.
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

package org.kie.workbench.common.services.refactoring.backend.server.query.findresourcereferences;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;
import org.kie.workbench.common.services.refactoring.backend.server.BaseIndexingTest;
import org.kie.workbench.common.services.refactoring.backend.server.TestIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.drl.TestDrlFileIndexer;
import org.kie.workbench.common.services.refactoring.backend.server.drl.TestDrlFileTypeDefinition;
import org.kie.workbench.common.services.refactoring.backend.server.query.NamedQuery;
import org.kie.workbench.common.services.refactoring.backend.server.query.response.DefaultResponseBuilder;
import org.kie.workbench.common.services.refactoring.backend.server.query.response.ResponseBuilder;
import org.kie.workbench.common.services.refactoring.backend.server.query.standard.FindAllChangeImpactQuery;
import org.kie.workbench.common.services.refactoring.backend.server.query.standard.FindResourceReferencesQuery;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueIndexTerm.TermSearchType;
import org.kie.workbench.common.services.refactoring.model.index.terms.valueterms.ValueReferenceIndexTerm;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRequest;
import org.kie.workbench.common.services.refactoring.model.query.RefactoringPageRow;
import org.kie.workbench.common.services.refactoring.service.ResourceType;
import org.uberfire.java.nio.file.Path;
import org.uberfire.paging.PageResponse;

public class FindResourceReferencesQueryValidIndexTermsTest
        extends BaseIndexingTest<TestDrlFileTypeDefinition> {

    protected Set<NamedQuery> getQueries() {
        return new HashSet<NamedQuery>() {{
            add( new FindResourceReferencesQuery() {
                @Override
                public ResponseBuilder getResponseBuilder() {
                    return new DefaultResponseBuilder( ioService() );
                }
            });
            add( new FindAllChangeImpactQuery() {
                @Override
                public ResponseBuilder getResponseBuilder() {
                    return new DefaultResponseBuilder( ioService() );
                }
            } );
        }};
    }

    @Test
    public void testFindResourceReferencesQueryValidIndexTerms() throws IOException, InterruptedException {
        //Add test files
        final Path [] paths = {
                basePath.resolve( "drl1.drl" ),
                basePath.resolve( "drl2.drl" ),
                basePath.resolve( "drl3.drl" ),
                basePath.resolve( "functions.drl" )
        };

        final String [] content = {
                loadText( "../findresources/drl1.drl" ),
                loadText( "../findresources/drl2.drl" ),
                loadText( "../findresources/drl3.drl" ),
                loadText( "../findresources/functions.drl" )
        };

        for( int i = 0; i < paths.length; ++i ) {
            ioService().write( paths[i], content[i] );
        }

        Thread.sleep( 5000 ); //wait for events to be consumed from jgit -> (notify changes -> watcher -> index) -> lucene index

        {
            final RefactoringPageRequest request = new RefactoringPageRequest( FindResourceReferencesQuery.NAME,
                                                                               new HashSet<ValueIndexTerm>() {{
                                                                                   add( new ValueReferenceIndexTerm(
                                                                                           "org.kie.workbench.common.services.refactoring.backend.server.drl.classes.Applicant",
                                                                                           ResourceType.JAVA ) );
                                                                               }},
                                                                               0,
                                                                               10 );

            try {
                final PageResponse<RefactoringPageRow> response = service.query( request );
                assertNotNull( response );
                assertEquals( 2,
                              response.getPageRowList().size() );
                assertResponseContains( response.getPageRowList(),
                                        paths[0] );
                assertResponseContains( response.getPageRowList(),
                                        paths[1] );

            } catch ( IllegalArgumentException e ) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        {
            final RefactoringPageRequest request = new RefactoringPageRequest( FindResourceReferencesQuery.NAME,
                                                                               new HashSet<ValueIndexTerm>() {{
                                                                                   add( new ValueReferenceIndexTerm(
                                                                                           "org.kie.workbench.common.services.refactoring.backend.server.drl.classes",
                                                                                           ResourceType.JAVA,
                                                                                           TermSearchType.PREFIX) );
                                                                               }},
                                                                               0,
                                                                               10 );

            try {
                final PageResponse<RefactoringPageRow> response = service.query( request );
                assertNotNull( response );
                assertEquals( 3,
                              response.getPageRowList().size() );
                assertResponseContains( response.getPageRowList(),
                                        paths[0] );
                assertResponseContains( response.getPageRowList(),
                                        paths[1] );

            } catch ( IllegalArgumentException e ) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        {
            final RefactoringPageRequest request = new RefactoringPageRequest( FindResourceReferencesQuery.NAME,
                                                                               new HashSet<ValueIndexTerm>() {{
                                                                                   add( new ValueReferenceIndexTerm(
                                                                                           "org.kie.workbench.mock.package.f1",
                                                                                           ResourceType.FUNCTION) );
                                                                               }},
                                                                               0,
                                                                               10 );

            try {
                final PageResponse<RefactoringPageRow> response = service.query( request );
                assertNotNull( response );
                assertEquals( 2,
                              response.getPageRowList().size() );
                assertResponseContains( response.getPageRowList(),
                                        paths[0] );
                assertResponseContains( response.getPageRowList(),
                                        paths[1] );

            } catch ( IllegalArgumentException e ) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        // only locally referenced, not referenced in other resources
        {
            final RefactoringPageRequest request = new RefactoringPageRequest( FindResourceReferencesQuery.NAME,
                                                                               new HashSet<ValueIndexTerm>() {{
                                                                                   add( new ValueReferenceIndexTerm(
                                                                                           "org.kie.workbench.mock.package.f4",
                                                                                           ResourceType.FUNCTION) );
                                                                               }},
                                                                               0,
                                                                               10 );

            try {
                final PageResponse<RefactoringPageRow> response = service.query( request );
                assertNotNull( response );
                assertEquals( 0,
                              response.getPageRowList().size() );

            } catch ( IllegalArgumentException e ) {
                fail("Exception thrown: " + e.getMessage());
            }
        }

        {
            final RefactoringPageRequest request = new RefactoringPageRequest( FindResourceReferencesQuery.NAME,
                                                                               new HashSet<ValueIndexTerm>() {{
                                                                                   add( new ValueReferenceIndexTerm(
                                                                                           "org.kie.workbench.mock.package.f4",
                                                                                           ResourceType.FUNCTION) );
                                                                               }},
                                                                               0,
                                                                               10 );

            try {
                final PageResponse<RefactoringPageRow> response = service.query( request );
                assertNotNull( response );
                assertEquals( 0,
                              response.getPageRowList().size() );

            } catch ( IllegalArgumentException e ) {
                fail("Exception thrown: " + e.getMessage());
            }
        }
    }


    @Override
    protected TestIndexer getIndexer() {
        return new TestDrlFileIndexer();
    }

    @Override
    protected TestDrlFileTypeDefinition getResourceTypeDefinition() {
        return new TestDrlFileTypeDefinition();
    }

    @Override
    protected String getRepositoryName() {
        return testName.getMethodName();
    }

}
