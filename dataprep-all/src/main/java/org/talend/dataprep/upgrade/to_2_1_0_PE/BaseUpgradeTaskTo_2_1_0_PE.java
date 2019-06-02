// ============================================================================
//
// Copyright (C) 2006-2018 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// https://github.com/Talend/data-prep/blob/master/LICENSE
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================

package org.talend.dataprep.upgrade.to_2_1_0_PE;

import org.talend.dataprep.upgrade.model.UpgradeTask;
import org.talend.dataprep.upgrade.model.UpgradeTaskId;

/**
 * Base interface for the 2.0.0-PE upgrade.
 */
public interface BaseUpgradeTaskTo_2_1_0_PE extends UpgradeTask {

    /** Constant used for the 2.1.0-PE version. */
    String VERSION_2_1_0_PE = "2.1.0-PE";

    @Override
    default UpgradeTaskId getId() {
        return getTaskId(VERSION_2_1_0_PE, this.getClass().getSimpleName());
    }

}
