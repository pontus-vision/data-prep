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
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.talend.dataprep.info.ManifestInfo;
import org.talend.dataprep.info.Version;
import org.talend.dataprep.metrics.Timed;
import org.talend.dataprep.security.PublicAPI;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.GET;

/**
 * Rest controller that returns current version of the Data Prep service.
 *
 * @see Version
 */
@RestController
@Api(value = "version", basePath = "/version", description = "versions of running application")
public class VersionService {

    private static final String UNDEFINED_VERSION_ID = "N/A";

    private static final Logger LOGGER = getLogger(VersionService.class);

    public static final Version VERSION = constructVersion(getManifests());

    /**
     * Static definition. Provides the advantage to reduce computation overhead on version call and the need of a spring
     * application context to get the dataprep running version (as DatasetMetadataBuilder for instance).
     * On the other hand, does not provide placeholder for missing generated git file.
     * For now order lexicographically but might be order based on resource name part as int priority.
     */
    private static List<ManifestInfo> getManifests() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(VersionService.class.getClassLoader());
        List<ManifestInfo> manifestInfos = new ArrayList<>();
        try {
            Resource[] resources = resolver.getResources("classpath:/dataprep*-git.properties");
            for (Resource resource : resources) {
                final Properties properties = new Properties();
                try (InputStream inputStream = resource.getInputStream()) {
                    properties.load(inputStream);
                } catch (IOException e) {
                    LOGGER.debug("Unable to read from resource '{}'.", resource, e);
                }
                final String versionId = Optional.ofNullable(properties.getProperty("git.build.version")).orElse("N/A");
                final String buildId = Optional.ofNullable(properties.getProperty("git.commit.id.abbrev")).orElse("N/A");
                final String label = extractTag(resource.getFilename());
                manifestInfos.add(new ManifestInfo(versionId, buildId, label));
            }
        } catch (IOException e) {
            // OSEF
            LOGGER.error("error !", e);
        }
        manifestInfos.sort(Comparator.nullsFirst(Comparator.comparing(ManifestInfo::getLabel)));
        return manifestInfos;
    }

    private static Version constructVersion(List<ManifestInfo> manifestInfos) {
        String buildId = manifestInfos
                .stream() //
                .map(ManifestInfo::getBuildId) //
                .collect(joining("-"));
        final Optional<String> uniqueVersion = manifestInfos
                .stream() //
                .map(ManifestInfo::getVersionId) //
                .filter(versionId -> !StringUtils.equals(UNDEFINED_VERSION_ID, versionId)) //
                .reduce((s, s2) -> {
                    if (StringUtils.equals(s, s2)) {
                        return s;
                    }
                    return s + '-' + s2;
                });
        String serviceUnifiedVersionId = uniqueVersion.orElse(UNDEFINED_VERSION_ID);

        return new Version(serviceUnifiedVersionId, buildId);
    }

    /**
     * extract the TAG element of "dataprep-TAG-git.properties" or without tag : "dataprep-git.properties"
     */
    private static String extractTag(String tag) {
        String[] elements = tag.split("-");
        if (elements.length > 2) {
            return elements[1];
        } else {
            // no tag
            return null;
        }
    }

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
    public Version version() {
        return VERSION;
    }

}
