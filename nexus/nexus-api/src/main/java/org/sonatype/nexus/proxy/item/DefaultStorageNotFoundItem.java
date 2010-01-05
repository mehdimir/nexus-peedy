package org.sonatype.nexus.proxy.item;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

public class DefaultStorageNotFoundItem
    extends AbstractStorageItem
    implements StorageNotFoundItem
{
    public DefaultStorageNotFoundItem( Repository repository, ResourceStoreRequest request )
    {
        super( repository, request );
    }

    public DefaultStorageNotFoundItem( RepositoryRouter router, ResourceStoreRequest request )
    {
        super( router, request );
    }
}
