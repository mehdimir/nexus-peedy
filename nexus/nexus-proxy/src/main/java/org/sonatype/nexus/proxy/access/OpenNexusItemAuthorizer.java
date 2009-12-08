package org.sonatype.nexus.proxy.access;

import java.util.ArrayList;
import java.util.List;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.component.annotations.Requirement;
import org.sonatype.nexus.proxy.NoSuchRepositoryException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.registry.RepositoryRegistry;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.target.TargetSet;

@Component( role = NexusItemAuthorizer.class )
public class OpenNexusItemAuthorizer
    implements NexusItemAuthorizer
{
    @Requirement
    private RepositoryRegistry repoRegistry;

    public boolean authorizePath( TargetSet matched, Action action )
    {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean authorizePath( Repository repository, ResourceStoreRequest request, Action action )
    {
        // TODO Auto-generated method stub
        return true;
    }

    public boolean authorizePermission( String permission )
    {
        // TODO Auto-generated method stub
        return true;
    }

    public TargetSet getGroupsTargetSet( Repository repository, ResourceStoreRequest request )
    {
        TargetSet targetSet = new TargetSet();

        for ( Repository group : getListOfGroups( repository.getId() ) )
        {
            // are the perms transitively inherited from the groups where it is member?
            // !group.isExposed()
            if ( true )
            {
                TargetSet groupMatched = group.getTargetsForRequest( request );

                targetSet.addTargetSet( groupMatched );
            }
        }

        return targetSet;
    }

    public boolean isViewable( String objectType, String objectId )
    {
        // TODO Auto-generated method stub
        return true;
    }

    // =

    protected List<Repository> getListOfGroups( String repositoryId )
    {
        List<Repository> groups = new ArrayList<Repository>();

        List<String> groupIds = repoRegistry.getGroupsOfRepository( repositoryId );

        for ( String groupId : groupIds )
        {
            try
            {
                groups.add( repoRegistry.getRepository( groupId ) );
            }
            catch ( NoSuchRepositoryException e )
            {
                // ignored
            }
        }

        return groups;
    }

}
