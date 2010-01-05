package org.sonatype.nexus.maven;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.repository.ArtifactRepositoryPolicy;
import org.apache.maven.artifact.repository.layout.ArtifactRepositoryLayout;
import org.apache.maven.artifact.repository.layout.DefaultRepositoryLayout;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingException;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.project.ProjectBuildingResult;
import org.apache.maven.repository.RepositorySystem;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.configuration.application.ApplicationConfiguration;
import org.sonatype.nexus.proxy.item.StorageFileItem;

@Component( role = MavenGate.class )
public class DefaultMavenGate
    implements MavenGate
{
    @Requirement
    private ApplicationConfiguration applicationConfiguration;

    @Requirement
    private ProjectBuilder projectBuilder;

    @Requirement
    private RepositorySystem repositorySystem;

    public ProjectBuildingResult buildMavenProject( StorageFileItem pomItem, List<String> usedNexusRepositoryIds,
                                                    List<String> profileIds, Map<String, String> systemProperties,
                                                    Map<String, String> userProperties )
        throws ProjectBuildingException
    {
        ClassLoader old = Thread.currentThread().getContextClassLoader();

        try
        {
            // for Maven, we need to set TCCL explicitly
            Thread.currentThread().setContextClassLoader( getClass().getClassLoader() );

            Properties systemProps = new Properties();
            systemProps.putAll( System.getProperties() );
            if ( systemProperties != null )
            {
                systemProps.putAll( systemProperties );
            }

            Properties userProps = new Properties();
            if ( userProperties != null )
            {
                userProps.putAll( userProperties );
            }

            ProjectBuildingRequest config = new DefaultProjectBuildingRequest();

            String localRepoUrl =
                "file://"
                    + System.getProperty( "nexus.maven.repo.local", applicationConfiguration.getWorkingDirectory(
                        "maven-repository" ).getAbsolutePath() );

            config.setLocalRepository( repositorySystem.createArtifactRepository( "local", localRepoUrl,
                new DefaultRepositoryLayout(), null, null ) );

            if ( usedNexusRepositoryIds != null )
            {
                ArrayList<ArtifactRepository> repositories = new ArrayList<ArtifactRepository>();

                for ( String nexusRepositoryId : usedNexusRepositoryIds )
                {
                    repositories.add( repositorySystem.createArtifactRepository( nexusRepositoryId, "nexus://"
                        + nexusRepositoryId, new DefaultRepositoryLayout(), null, null ) );
                }

                config.setRemoteRepositories( repositories );
            }

            if ( profileIds != null )
            {
                config.setActiveProfileIds( profileIds );
            }

            config.setSystemProperties( systemProps );
            config.setUserProperties( userProps );
            // nexus will handles caching
            config.setForceUpdate( true );
            config.setProcessPlugins( false );

            return projectBuilder.build( new StorageFileItemModelSource( pomItem ), config );
        }
        finally
        {
            Thread.currentThread().setContextClassLoader( old );
        }
    }
}
