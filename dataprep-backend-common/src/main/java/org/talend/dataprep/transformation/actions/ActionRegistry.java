// ============================================================================
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.transformation.actions;

import java.util.stream.Stream;

public interface ActionRegistry {

    /**
     * Returns the {@link ActionDefinition} for given <code>name</code> or <code>null</code> if not found.
     * @param name An action name
     * @return The {@link ActionDefinition} <b>instance </b>for given action <code>name</code>.
     */
    ActionDefinition get(String name);

    /**
     * @return All the {@link ActionDefinition} as <b>instances</b>.
     */
    Stream<Class<? extends ActionDefinition>> getAll();

    /**
     * @return All the {@link ActionDefinition} as <b>classes</b>.
     */
    Stream<ActionDefinition> findAll();
}
