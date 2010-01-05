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
package org.sonatype.nexus.proxy;

import org.sonatype.nexus.proxy.item.StorageNotFoundItem;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Thrown if the requested item is not found.
 * 
 * @author cstamas
 */
public class ItemNotFoundException
    extends Exception
{
    private static final long serialVersionUID = -4964273361722823796L;

    private final StorageNotFoundItem notFoundItem;

    private final ResourceStoreRequest request;

    private final Repository repository;

    @Deprecated
    public ItemNotFoundException( String message, ResourceStoreRequest request, Repository repository )
    {
        super( message );

        this.notFoundItem = null;

        this.repository = repository;

        this.request = request;
    }

    @Deprecated
    public ItemNotFoundException( ResourceStoreRequest request, Repository repository )
    {
        this( repository != null ? "Item not found on path \"" + request.toString() + "\" in repository \""
            + repository.getId() + "\"!" : "Item not found on path \"" + request.toString() + "\"!", request,
            repository );
    }

    public ItemNotFoundException( StorageNotFoundItem notFoundItem )
    {
        this( notFoundItem.getRepository() != null ? "Item not found on path \""
            + notFoundItem.getResourceStoreRequest().toString() + "\" in repository \""
            + notFoundItem.getRepository().getId() + "\"!" : "Item not found on path \""
            + notFoundItem.getResourceStoreRequest() + "\"!", notFoundItem );
    }

    public ItemNotFoundException( String message, StorageNotFoundItem notFoundItem )
    {
        super( message );

        this.notFoundItem = notFoundItem;

        this.request = null;

        this.repository = null;
    }

    public StorageNotFoundItem getStorageNotFoundItem()
    {
        return notFoundItem;
    }

    public Repository getRepository()
    {
        if ( getStorageNotFoundItem() != null )
        {
            return getStorageNotFoundItem().getRepository();
        }
        else
        {
            return repository;
        }
    }

    public ResourceStoreRequest getRequest()
    {
        if ( getStorageNotFoundItem() != null )
        {
            return getStorageNotFoundItem().getResourceStoreRequest();
        }
        else
        {
            return request;
        }
    }
}
