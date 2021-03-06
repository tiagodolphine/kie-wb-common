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

package org.kie.workbench.common.forms.processing.engine.handling.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;

import com.google.gwt.user.client.ui.IsWidget;
import org.jboss.errai.common.client.api.Assert;
import org.jboss.errai.databinding.client.BindableProxy;
import org.jboss.errai.databinding.client.BindableProxyFactory;
import org.jboss.errai.databinding.client.PropertyChangeUnsubscribeHandle;
import org.jboss.errai.databinding.client.api.Converter;
import org.jboss.errai.databinding.client.api.DataBinder;
import org.jboss.errai.databinding.client.api.StateSync;
import org.kie.workbench.common.forms.processing.engine.handling.FieldChangeHandler;
import org.kie.workbench.common.forms.processing.engine.handling.FieldChangeHandlerManager;
import org.kie.workbench.common.forms.processing.engine.handling.Form;
import org.kie.workbench.common.forms.processing.engine.handling.FormField;
import org.kie.workbench.common.forms.processing.engine.handling.FormHandler;
import org.kie.workbench.common.forms.processing.engine.handling.FormValidator;
import org.kie.workbench.common.forms.processing.engine.handling.IsNestedModel;

public class FormHandlerImpl<T> implements FormHandler<T> {

    protected FormValidator validator;

    protected FieldChangeHandlerManager fieldChangeManager;

    protected DataBinder<T> binder;

    protected List<PropertyChangeUnsubscribeHandle> unsubscribeHandlers = new ArrayList<>();

    protected Form form;

    @Inject
    public FormHandlerImpl(FormValidator validator,
                           FieldChangeHandlerManager fieldChangeManager) {
        this.validator = validator;
        this.fieldChangeManager = fieldChangeManager;

        this.form = new Form();

        fieldChangeManager.setValidator(validator);
    }

    @Override
    public void setUp(DataBinder<T> binder) {
        setUp(binder,
              false);
    }

    @Override
    public void setUp(DataBinder<T> binder,
                      boolean bindInputs) {
        Assert.notNull("DataBinder cannot be null",
                       binder);

        clear();

        this.binder = binder;
    }

    @Override
    public void setUp(T model) {
        Assert.notNull("Model cannot be null",
                       model);

        clear();

        this.binder = getBinderForModel(model);
    }

    protected DataBinder<T> getBinderForModel(T model) {
        return DataBinder.forModel(model);
    }

    @Override
    public void registerInput(FormField formField) {
        registerInput(formField,
                      null);
    }

    @Override
    public void registerInput(FormField formField,
                              Converter valueConverter) {
        Assert.notNull("FormHandler isn't correctly initialized, please run any of the setUp methods before use",
                       binder);
        Assert.notNull("FormField cannot be null!",
                       formField);

        String fieldName = formField.getFieldName();
        IsWidget widget = formField.getWidget();

        form.addField(formField);

        if (formField.isBindable()) {

            BindableProxy proxy = (BindableProxy) binder.getModel();

            Object modelValue = readPropertyValue(proxy,
                                                  formField.getFieldBinding());

            StateSync stateSync = Optional.ofNullable(modelValue).isPresent() ? StateSync.FROM_MODEL : StateSync.FROM_UI;

            binder.bind(widget,
                        formField.getFieldBinding(),
                        valueConverter,
                        stateSync);
        }

        fieldChangeManager.registerField(formField);

        formField.getChangeListeners().forEach(listener -> fieldChangeManager.addFieldChangeHandler(listener.getFieldToListen(),
                                                                                                    listener.getChangeHandler()));

        /**
         * if field isn't bindable we cannot listen to field value changes.
         */
        if (!formField.isBindable()) {
            return;
        }

        if (widget instanceof IsNestedModel) {
            IsNestedModel nestedModelWidget = (IsNestedModel) widget;
            nestedModelWidget.addFieldChangeHandler((childFieldName, newValue) -> fieldChangeManager.notifyFieldChange(fieldName + "." + childFieldName,
                                                                                                                       newValue));
        } else {
            PropertyChangeUnsubscribeHandle unsubscribeHandle = binder.addPropertyChangeHandler(formField.getFieldBinding(),
                                                                                                event -> fieldChangeManager.processFieldChange(fieldName,
                                                                                                                                               event.getNewValue(),
                                                                                                                                               binder.getModel()));
            unsubscribeHandlers.add(unsubscribeHandle);
        }
    }

    protected Object readPropertyValue(BindableProxy proxy,
                                       String fieldBinding) {
        if (fieldBinding.indexOf(".") != -1) {
            // Nested property

            int separatorPosition = fieldBinding.indexOf(".");
            String nestedModelName = fieldBinding.substring(0,
                                                            separatorPosition);
            String property = fieldBinding.substring(separatorPosition + 1);
            Object nestedModel = proxy.get(nestedModelName);
            if (nestedModel == null) {
                return null;
            }

            return readPropertyValue((BindableProxy) BindableProxyFactory.getBindableProxy(nestedModel),
                                     property);
        }
        return proxy.get(fieldBinding);
    }

    public void addFieldChangeHandler(FieldChangeHandler handler) {
        addFieldChangeHandler(null,
                              handler);
    }

    public void addFieldChangeHandler(String fieldName,
                                      FieldChangeHandler handler) {
        Assert.notNull("FieldChangeHandler cannot be null",
                       handler);

        if (fieldName != null) {
            fieldChangeManager.addFieldChangeHandler(fieldName,
                                                     handler);
        } else {
            fieldChangeManager.addFieldChangeHandler(handler);
        }
    }

    @Override
    public boolean validate() {
        return validator.validate(form,
                                  getModel());
    }

    @Override
    public boolean validate(String propertyName) {
        return validator.validate(form.findFormField(propertyName),
                                  getModel());
    }

    @Override
    public void setReadOnly(boolean readOnly) {
        form.getFields().forEach(field -> field.setReadOnly(readOnly));
    }

    @Override
    public Form getForm() {
        return form;
    }

    public void disableNestedForms() {
        form.getFields().stream()
                .filter(formField -> formField.getWidget() instanceof IsNestedModel)
                .map(formField -> (IsNestedModel) formField.getWidget())
                .forEach(IsNestedModel::clear);
    }

    @Override
    public void clear() {

        // Check if it's initialized before clear.
        if (binder == null) {
            return;
        }

        disableNestedForms();

        unsubscribeHandlers.forEach(PropertyChangeUnsubscribeHandle::unsubscribe);

        unsubscribeHandlers.clear();

        fieldChangeManager.clear();

        binder.unbind();

        form = null;
    }

    public T getModel() {
        return binder.getModel();
    }
}
