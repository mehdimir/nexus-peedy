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

import java.util.Map;

import org.sonatype.nexus.proxy.RequestContext;
import org.sonatype.nexus.proxy.ResourceStore;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * The Interface StorageItem, a top of the item abstraction.
 */
public interface StorageItem
{
    /**
     * Gets the originating ResourceStore. This may be a repository or a router.
     * 
     * @return
     */
    ResourceStore getStore();

    /**
     * Gets the originating Repository. This is actually a helper method, that calls into getStore() and returns the
     * value only if it is not null and is instance of Repository.
     * 
     * @return
     */
    Repository getRepository();

    /**
     * The request.
     * 
     * @return
     */
    ResourceStoreRequest getResourceStoreRequest();

    /**
     * Gets the repository item uid from where originates item.
     * 
     * @return the repository item uid
     */
    RepositoryItemUid getRepositoryItemUid();

    /**
     * Set the repository item uid.
     * 
     * @param repositoryItemUid
     */
    void setRepositoryItemUid( RepositoryItemUid repositoryItemUid );

    /**
     * Gets the repository id from where originates item.
     * 
     * @return the repository id
     */
    String getRepositoryId();

    /**
     * Gets the creation time.
     * 
     * @return the created
     */
    long getCreated();

    /**
     * Gets the modification time.
     * 
     * @return the modified
     */
    long getModified();

    /**
     * Gets the stored locally time.
     * 
     * @return the stored locally
     */
    long getStoredLocally();

    /**
     * Sets the stored locally time.
     * 
     * @return the stored locally
     */
    void setStoredLocally( long ts );

    /**
     * Gets the remote checked.
     * 
     * @return the remote checked
     */
    long getRemoteChecked();

    /**
     * Sets the remote checked.
     * 
     * @return the remote checked
     */
    void setRemoteChecked( long ts );

    /**
     * Gets the last requested.
     * 
     * @return time when it was last served
     */
    long getLastRequested();

    /**
     * Sets the last requested.
     * 
     * @return time when it was last served
     */
    void setLastRequested( long ts );

    /**
     * Checks if is virtual. Virtual items are not backed by any storage directly, they are generated.
     * 
     * @return true, if is virtual
     */
    boolean isVirtual();

    /**
     * Returns true if the item is expired.
     */
    boolean isExpired();

    /**
     * Sets if the item is expired.
     */
    void setExpired( boolean expired );

    /**
     * Returns true if the item is existing.
     * 
     * @return
     */
    boolean isFound();

    /**
     * Gets the path.
     * 
     * @return the path
     */
    String getPath();

    /**
     * Gets the name.
     * 
     * @return the name
     */
    String getName();

    /**
     * Gets the parent path.
     * 
     * @return the parent path
     */
    String getParentPath();

    /**
     * Gets the remote url.
     * 
     * @return the remote url
     */
    String getRemoteUrl();

    /**
     * Gets the user attributes. These are saved and persisted.
     * 
     * @return the attributes
     */
    Map<String, String> getAttributes();

    /**
     * Gets the item context. It is living only during item processing, it is not stored.
     * 
     * @return the attributes
     */
    RequestContext getItemContext();

    /**
     * Overlay.
     * 
     * @param item the item
     */
    void overlay( StorageItem item );

    /**
     * Returns the generation of the attributes. For Nexus internal use only!
     * 
     * @return
     */
    int getGeneration();

    /**
     * Increments the generation of the attributes. For Nexus internal use only!
     * 
     * @return
     */
    void incrementGeneration();
}
