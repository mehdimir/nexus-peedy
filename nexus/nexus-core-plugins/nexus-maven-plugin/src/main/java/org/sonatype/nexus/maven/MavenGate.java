package org.sonatype.nexus.maven;

import java.util.List;
import java.util.Map;

import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingResult;
import org.sonatype.nexus.proxy.item.StorageFileItem;

public interface MavenGate
{
    // build project
    ProjectBuildingResult buildMavenProject( StorageFileItem pomItem, List<String> usedNexusRepositoryIds,
                                             List<String> profileIds, Map<String, String> systemProperties,
                                             Map<String, String> userProperties )
        throws ProjectBuildingException;

    // get effective pom

    // get transitive dependencies

}
