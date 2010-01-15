/**
 * Sonatype Nexus (TM) Open Source Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://nexus.sonatype.org/dev/attributions.html
 * This program is licensed to you under Version 3 only of the GNU General Public License as published by the Free Software Foundation.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License Version 3 for more details.
 * You should have received a copy of the GNU General Public License Version 3 along with this program.
 * If not, see http://www.gnu.org/licenses/.
 * Sonatype Nexus (TM) Professional Version is available from Sonatype, Inc.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.proxy.item;

import java.util.Collection;

import org.sonatype.nexus.proxy.ItemNotFoundException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.StorageException;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * The Class DefaultStorageCollectionItem.
 */
public class DefaultStorageCollectionItem
    extends AbstractStorageItem
    implements StorageCollectionItem
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = -7329636330511885938L;

    /**
     * Instantiates a new default storage collection item.
     * 
     * @param repository the repository
     * @param path the path
     * @param canRead the can read
     * @param canWrite the can write
     */
    public DefaultStorageCollectionItem( Repository repository, ResourceStoreRequest request )
    {
        super( repository, request );
    }

    /**
     * Instantiates a new default storage collection item.
     * 
     * @param router the router
     * @param path the path
     * @param virtual the virtual
     * @param canRead the can read
     * @param canWrite the can write
     */
    public DefaultStorageCollectionItem( RepositoryRouter router, ResourceStoreRequest request )
    {
        super( router, request );
    }

    public Collection<StorageItem> list()
        throws ItemNotFoundException, StorageException
    {
        if ( isVirtual() )
        {
            return getStore().list( getResourceStoreRequest() );
        }
        else
        {
            Repository repo = getRepositoryItemUid().getRepository();

            Collection<StorageItem> result = repo.list( false, this );

            correctPaths( result );

            return result;
        }
    }

    /**
     * This method "normalizes" the paths back to the "level" from where the original item was requested.
     * 
     * @param list
     */
    protected void correctPaths( Collection<StorageItem> list )
    {
        for ( StorageItem item : list )
        {
            if ( getPath().endsWith( RepositoryItemUid.PATH_SEPARATOR ) )
            {
                ( (AbstractStorageItem) item ).setPath( getPath() + item.getName() );
            }
            else
            {
                ( (AbstractStorageItem) item ).setPath( getPath() + RepositoryItemUid.PATH_SEPARATOR + item.getName() );
            }
        }
    }
}
