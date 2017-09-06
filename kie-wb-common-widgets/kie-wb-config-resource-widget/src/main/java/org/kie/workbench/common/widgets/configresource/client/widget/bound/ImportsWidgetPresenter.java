/*
 * Copyright 2012 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.kie.workbench.common.widgets.configresource.client.widget.bound;

import java.util.ArrayList;
import java.util.List;
import javax.enterprise.event.Event;
import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import org.appformer.project.datamodel.imports.Import;
import org.appformer.project.datamodel.imports.Imports;
import org.kie.workbench.common.widgets.client.datamodel.AsyncPackageDataModelOracle;
import org.kie.workbench.common.widgets.client.datamodel.ImportAddedEvent;
import org.kie.workbench.common.widgets.client.datamodel.ImportRemovedEvent;

import static org.uberfire.commons.validation.PortablePreconditions.checkNotNull;

public class ImportsWidgetPresenter implements ImportsWidgetView.Presenter,
                                               IsWidget {

    private ImportsWidgetView view;

    private Event<ImportAddedEvent> importAddedEvent;
    private Event<ImportRemovedEvent> importRemovedEvent;

    private final List<Import> internalFactTypes = new ArrayList<>();
    private final List<Import> externalFactTypes = new ArrayList<>();
    private final List<Import> modelFactTypes = new ArrayList<>();

    private AsyncPackageDataModelOracle dmo;
    private Imports importTypes;

    public ImportsWidgetPresenter() {
    }

    @Inject
    public ImportsWidgetPresenter(final ImportsWidgetView view,
                                  final Event<ImportAddedEvent> importAddedEvent,
                                  final Event<ImportRemovedEvent> importRemovedEvent) {
        this.view = view;
        this.importAddedEvent = importAddedEvent;
        this.importRemovedEvent = importRemovedEvent;
        view.init(this);
    }

    @Override
    public void setContent(final AsyncPackageDataModelOracle dmo,
                           final Imports importTypes,
                           final boolean isReadOnly) {
        this.dmo = checkNotNull("dmo",
                                dmo);
        this.importTypes = checkNotNull("importTypes",
                                        importTypes);

        internalFactTypes.clear();
        externalFactTypes.clear();
        modelFactTypes.clear();

        //Get list of types within the package
        for (String importType : dmo.getInternalFactTypes()) {
            internalFactTypes.add(new Import(importType.replaceAll("\\$",
                                                                   ".")));
        }

        //Get list of potential imports
        for (String importType : dmo.getExternalFactTypes()) {
            externalFactTypes.add(new Import(importType.replaceAll("\\$",
                                                                   ".")));
        }

        //Remove internal imports from model's imports (this should never be the case, but it exists "in the wild")
        modelFactTypes.addAll(importTypes.getImports());
        modelFactTypes.removeAll(internalFactTypes);

        view.setContent(internalFactTypes,
                        externalFactTypes,
                        modelFactTypes,
                        isReadOnly);
    }

    @Override
    public boolean isInternalImport(final Import importType) {
        return internalFactTypes.contains(importType);
    }

    @Override
    public void onAddImport(final Import importType) {
        importTypes.getImports().add(importType);
        dmo.filter();

        //Signal change to any other interested consumers (e.g. some editors support rendering of unknown fact-types)
        importAddedEvent.fire(new ImportAddedEvent(dmo,
                                                   importType));
    }

    @Override
    public void onRemoveImport(final Import importType) {
        importTypes.getImports().remove(importType);
        dmo.filter();

        //Signal change to any other interested consumers (e.g. some editors support rendering of unknown fact-types)
        importRemovedEvent.fire(new ImportRemovedEvent(dmo,
                                                       importType));
    }

    @Override
    public Widget asWidget() {
        return view.asWidget();
    }
}
