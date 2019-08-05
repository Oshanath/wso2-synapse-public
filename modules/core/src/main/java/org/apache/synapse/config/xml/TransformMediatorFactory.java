/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *   * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package org.apache.synapse.config.xml;

import org.apache.axiom.om.OMAttribute;
import org.apache.axiom.om.OMElement;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.synapse.Mediator;
import org.apache.synapse.SynapseConstants;
import org.apache.synapse.mediators.MediatorProperty;
import org.apache.synapse.mediators.Value;
import org.apache.synapse.mediators.builtin.TransformMediator;

import javax.xml.namespace.QName;
import java.util.List;
import java.util.Properties;

public class TransformMediatorFactory extends AbstractMediatorFactory {
    private static final Log log = LogFactory.getLog(IterateMediatorFactory.class);
    private static final QName TRANSFORM_Q = new QName(SynapseConstants.SYNAPSE_NAMESPACE, "transform");
    private static final QName ATT_SCHEMA = new QName("schema");

    @Override
    protected Mediator createSpecificMediator(OMElement elem, Properties properties) {
        TransformMediator transformMediator = new TransformMediator();
        processAuditStatus(transformMediator, elem);
        OMAttribute schema = elem.getAttribute(ATT_SCHEMA);
        if (schema != null) {
            // ValueFactory for creating dynamic or static Value
            ValueFactory keyFac = new ValueFactory();
            // create dynamic or static key based on OMElement
            Value generatedKey = keyFac.createValue("schema", elem);
            transformMediator.setSchemaKey(generatedKey);
        } else {
            List<MediatorProperty> mediatorPropertyList = MediatorPropertyFactory.getMediatorProperties(elem);
            if (mediatorPropertyList != null && !mediatorPropertyList.isEmpty()) {
                transformMediator.addAllProperties(mediatorPropertyList);
            } else {
                handleException("Transform mediator should contain either a schema or custom properties");
            }
        }
        return transformMediator;
    }

    @Override
    public QName getTagQName() {
        return TRANSFORM_Q;
    }
}
