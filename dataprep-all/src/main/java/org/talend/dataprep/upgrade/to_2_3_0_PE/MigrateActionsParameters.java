/*
 * Copyright (C) 2006-2017 Talend Inc. - www.talend.com
 *
 * This source code is available under agreement available at
 * https://github.com/Talend/data-prep/blob/master/LICENSE
 *
 * You should have received a copy of the agreement
 * along with this program; if not, write to Talend SA
 * 9 rue Pages 92150 Suresnes, France
 */

package org.talend.dataprep.upgrade.to_2_3_0_PE;

import static org.talend.dataprep.upgrade.model.UpgradeTask.target.VERSION;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.talend.dataprep.preparation.store.PreparationRepository;
import org.talend.dataprep.upgrade.common.ActionNewColumnToggleCommon;

@Component
public class MigrateActionsParameters extends BaseUpgradeTaskTo_2_3_0_PE {

    @Autowired
    private PreparationRepository preparationRepository;

    @Override
    public void run() {
        ActionNewColumnToggleCommon.upgradeActions(preparationRepository);
    }

    @Override
    public int getOrder() {
        return 1;
    }

    @Override
    public target getTarget() {
        return VERSION;
    }
}
