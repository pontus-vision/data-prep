// ============================================================================
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

package org.talend.dataprep.api.service.info;

import org.junit.Test;
import org.talend.dataprep.info.Version;

import static org.junit.Assert.assertEquals;

public class VersionServiceTest {

    private String label = "label";

    @Test
    public void shouldAggregateBuildId() throws Exception {
        // when
        final Version version = VersionService.VERSION;

        // then
        assertEquals("v1", version.getVersionId());
        assertEquals("1234-5678", version.getBuildId());
    }

    @Test
    public void shouldAggregateSameBuildId() throws Exception {
        // when
        final Version version = VersionService.VERSION;

        // then
        assertEquals("v1", version.getVersionId());
        assertEquals("1234-1234", version.getBuildId());
    }

}
