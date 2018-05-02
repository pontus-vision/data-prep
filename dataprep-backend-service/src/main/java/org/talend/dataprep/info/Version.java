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

package org.talend.dataprep.info;

import com.github.zafarkhaja.semver.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.talend.dataprep.api.service.info.VersionService;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.*;

public class Version {

    private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);

    private static final String UNDEFINED_VERSION_ID = "N/A";

    /** All dataprep manifests. */
    private static final List<ManifestInfo> MANIFESTS = getManifests();

    private String versionId;

    private String buildId;

    private String serviceName;

    public Version() {
        // needed for the json de/serialization
    }

    public Version(String versionId, String buildId) {
        this(versionId, buildId, EMPTY);
    }

    public Version(String versionId, String buildId, String serviceName) {
        this.versionId = versionId;
        this.buildId = buildId;
        this.serviceName = serviceName;
    }

    public String getVersionId() {
        return versionId;
    }

    public void setVersionId(String versionId) {
        this.versionId = versionId;
    }

    public String getBuildId() {
        return buildId;
    }

    public void setBuildId(String buildId) {
        this.buildId = buildId;
    }

    public String getServiceName() {
        return serviceName;
    }

    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    public static com.github.zafarkhaja.semver.Version fromInternal(Version internalVersion) {
        String versionId = internalVersion.getVersionId();
        final String versionAsString = substringBefore(versionId, "-");
        try {
            return com.github.zafarkhaja.semver.Version.valueOf(versionAsString);
        } catch (IllegalArgumentException | ParseException e) {
            LOGGER.info("Couldn't parse version {}. Message was: {}", versionId, e.getMessage());
            return com.github.zafarkhaja.semver.Version.forIntegers(0);
        }
    }

    @Override
    public String toString() {
        return "{" + "versionId='" + versionId + '\'' + ", buildId='" + buildId + '\'' + ", serviceName='" + serviceName + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        Version version = (Version) o;

        if (versionId != null ? !versionId.equals(version.versionId) : version.versionId != null)
            return false;
        if (buildId != null ? !buildId.equals(version.buildId) : version.buildId != null)
            return false;
        return !(serviceName != null ? !serviceName.equals(version.serviceName) : version.serviceName != null);

    }

    @Override
    public int hashCode() {
        int result = versionId != null ? versionId.hashCode() : 0;
        result = 31 * result + (buildId != null ? buildId.hashCode() : 0);
        result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
        return result;
    }

    /**
     * Static definition. Provides the advantage to reduce computation overhead on version call and the need of a spring
     * application context to get the dataprep running version (as DatasetMetadataBuilder for instance).
     * On the other hand, does not provide placeholder for missing generated git file.
     * For now order lexicographically but might be order based on resource name part as int priority.
     */
    private static List<ManifestInfo> getManifests() {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(VersionService.class.getClassLoader());
        List<ManifestInfo> manifests = new ArrayList<>();
        try {
            // TODO : fails to find other properties
            Resource[] resources = resolver.getResources("classpath:/dataprep-*-git.properties");
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
                manifests.add(new ManifestInfo(versionId, buildId, label));
            }
        } catch (IOException e) {
            // OSEF
            LOGGER.error("error !", e);
        }
        manifests.sort(Comparator.nullsFirst(Comparator.comparing(ManifestInfo::getLabel)));
        return manifests;
    }

    public static Version constructVersion(String[] libs) {
        String buildId = Arrays.stream(libs)
                .map(Version::buildIdForTag)
                .collect(joining("-"));

        final String uniqueVersion = Arrays.stream(libs)
                .map(Version::versionIdForTag) //
                .filter(versionId -> !org.apache.commons.lang3.StringUtils.equals(UNDEFINED_VERSION_ID, versionId)) //
                .reduce((s, s2) -> {
                    if (org.apache.commons.lang3.StringUtils.equals(s, s2)) {
                        return s;
                    }
                    return s + '-' + s2;
                }).orElse(UNDEFINED_VERSION_ID);

        return new Version(uniqueVersion, buildId);
    }

    private static String buildIdForTag(String tag) {
        return Version.MANIFESTS.stream()
                .filter(mi -> mi.getLabel().equals(tag))
                .map(ManifestInfo::getBuildId)
                .findAny()
                .orElse(UNDEFINED_VERSION_ID);
    }

    private static String versionIdForTag(String tag) {
        return Version.MANIFESTS.stream()
                .filter(mi -> mi.getLabel().equals(tag))
                .map(ManifestInfo::getVersionId)
                .findAny()
                .orElse(UNDEFINED_VERSION_ID);
    }

    /**
     * Extract the TAG element of "dataprep-TAG-git.properties"
     */
    private static String extractTag(String tag) {
        // dataprep-${project.artifactId}-git.properties
        if (startsWith(tag, "dataprep-") && endsWith(tag, "-git.properties")) {
            return removeEnd(
                    removeStart(tag, "dataprep-"), "-git.properties");
        } else {
            // no tag
            return null;
        }
    }

}
