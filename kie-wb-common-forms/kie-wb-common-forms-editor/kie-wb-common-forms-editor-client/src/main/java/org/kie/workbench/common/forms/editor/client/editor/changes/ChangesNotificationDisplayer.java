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

package org.kie.workbench.common.forms.editor.client.editor.changes;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.PostConstruct;
import javax.enterprise.context.Dependent;
import javax.inject.Inject;

import org.jboss.errai.ioc.client.container.IOC;
import org.kie.workbench.common.forms.editor.client.editor.changes.conflicts.ConflictsHandler;
import org.kie.workbench.common.forms.editor.client.editor.changes.conflicts.element.ConflictElement;
import org.kie.workbench.common.forms.editor.client.editor.changes.displayers.conflicts.ConflictsDisplayer;
import org.kie.workbench.common.forms.editor.client.editor.changes.displayers.newProperties.NewPropertiesDisplayer;
import org.kie.workbench.common.forms.editor.model.FormModelSynchronizationResult;
import org.kie.workbench.common.forms.editor.model.FormModelerContent;
import org.kie.workbench.common.forms.model.FieldDefinition;
import org.kie.workbench.common.forms.model.FormModel;
import org.uberfire.commons.validation.PortablePreconditions;
import org.uberfire.mvp.Command;

@Dependent
public class ChangesNotificationDisplayer implements ChangesNotificationDisplayerView.Presenter {

    private ChangesNotificationDisplayerView view;

    private NewPropertiesDisplayer newPropertiesDisplayer;

    private ConflictsDisplayer conflictsDisplayer;

    private FormModelerContent content;

    private boolean canDisplay;

    private Command onClose;

    private List<ConflictsHandler> allHandlers = new ArrayList<>();

    private List<ConflictsHandler> activeHandlers = new ArrayList<>();

    @Inject
    public ChangesNotificationDisplayer(ChangesNotificationDisplayerView view,
                                        ConflictsDisplayer conflictsDisplayer,
                                        NewPropertiesDisplayer newPropertiesDisplayer) {
        this.view = view;
        this.newPropertiesDisplayer = newPropertiesDisplayer;
        this.conflictsDisplayer = conflictsDisplayer;
        this.view.init(this);
    }

    @PostConstruct
    protected void lookupConflictsHandlers() {
        IOC.getBeanManager().lookupBeans(ConflictsHandler.class).forEach(def -> register(def.getInstance()));
    }

    protected void register(ConflictsHandler handler) {
        PortablePreconditions.checkNotNull("handler",
                                           handler);
        allHandlers.add(handler);
    }

    public void show(FormModelerContent content,
                     Command onClose) {
        PortablePreconditions.checkNotNull("content",
                                           content);
        PortablePreconditions.checkNotNull("onClose",
                                           onClose);

        this.content = content;
        this.onClose = onClose;

        this.canDisplay = false;

        checkNewModelFields();
        checkContentConflicts();

        if (canDisplay) {
            view.show();
        }
    }

    protected void checkNewModelFields() {
        FormModelSynchronizationResult synchronizationResult = content.getSynchronizationResult();

        if (synchronizationResult != null && synchronizationResult.hasNewProperties()) {
            Set<FieldDefinition> modelFields = new HashSet<>(content.getAvailableFields());

            modelFields.removeIf(fieldDefinition -> !synchronizationResult.getNewProperties().stream().filter(property -> property.getName().equals(fieldDefinition.getBinding())).findAny().isPresent());

            if (!modelFields.isEmpty()) {
                this.canDisplay = true;
                newPropertiesDisplayer.showAvailableFields(modelFields);

                view.getElement().appendChild(newPropertiesDisplayer.getElement());
            }
        }
    }

    protected void checkContentConflicts() {
        activeHandlers.clear();

        allHandlers.forEach(conflictsHandler -> {
            if (conflictsHandler.checkConflicts(content,
                                                this::displayConflict)) {
                this.activeHandlers.add(conflictsHandler);
            }
        });

        if (!activeHandlers.isEmpty()) {
            this.canDisplay = true;
            this.view.getElement().appendChild(conflictsDisplayer.getElement());
        }
    }

    protected void displayConflict(ConflictElement conflictElement) {
        conflictsDisplayer.showConflict(conflictElement);
    }

    @Override
    public FormModel getFormModel() {
        return content.getDefinition().getModel();
    }

    @Override
    public void close() {
        newPropertiesDisplayer.clear();
        conflictsDisplayer.clear();

        activeHandlers.forEach(ConflictsHandler::onAccept);
        activeHandlers.clear();

        onClose.execute();
    }
}
