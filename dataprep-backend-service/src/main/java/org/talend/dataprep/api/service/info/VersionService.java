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

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.talend.dataprep.info.ManifestInfo;
import org.talend.dataprep.info.Version;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.security.PublicAPI;

import static org.springframework.web.bind.annotation.RequestMethod.GET;
import static org.talend.dataprep.info.Version.constructVersion;

/**
 * Rest controller that returns current version of the Data Prep service.
 *
 * @see Version
 */
@RestController
@Api(value = "version", basePath = "/version", description = "versions of running application")
public class VersionService {

    @Value("${dataprep.version.sources:dataprep-backend-service}")
    private String versionLibs;

    /**
     * @return A {@link Version} built following these conventions:
     * <ul>
     * <li>Concatenation of all {@link ManifestInfo} build ids (as returned by {@link ManifestInfo#getBuildId()}),
     * separated
     * by "-". Value to be returned by {@link Version#getBuildId()}.</li>
     * <li>Concatenation of all <b>unique</b>{@link ManifestInfo} versions ids (as returned by
     * {@link ManifestInfo#getVersionId()} ()}), separated by "-" (when more than one version found). Value to be
     * returned by {@link Version#getVersionId()}</li>
     * </ul>
     */
    @RequestMapping(value = "/version", method = GET)
    @ApiOperation(value = "Get the version of the service", produces = MediaType.APPLICATION_JSON_VALUE)
    @Timed
    @PublicAPI
    public Version version(@RequestParam(value = "libs", required = false) String libs) {
        if (libs == null) libs = versionLibs;
        return constructVersion(libs.split(","));
    }

    public Version version() {
        return version(versionLibs);
    }

}
