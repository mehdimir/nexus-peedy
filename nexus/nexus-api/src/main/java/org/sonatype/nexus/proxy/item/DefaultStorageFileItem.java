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

import java.io.IOException;
import java.io.InputStream;

import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;
import org.sonatype.nexus.proxy.router.RepositoryRouter;

/**
 * The Class DefaultStorageFileItem.
 */
public class DefaultStorageFileItem
    extends AbstractStorageItem
    implements StorageFileItem
{

    /** The Constant serialVersionUID. */
    private static final long serialVersionUID = 3608889194663697395L;

    /** The input stream. */
    private transient ContentLocator contentLocator;

    private long length;

    /**
     * This is here for backward compatibility only, to enable XStream to load up the old XML attributes.
     * 
     * @deprecated The mime-type is now coming from ContentLocator, see getMimeType() method body.
     */
    @SuppressWarnings( "unused" )
    private String mimeType;

    /**
     * Instantiates a new default storage file item.
     * 
     * @param repository the repository
     * @param path the path
     * @param canRead the can read
     * @param canWrite the can write
     * @param contentLocator the content locator
     */
    public DefaultStorageFileItem( Repository repository, ResourceStoreRequest request, ContentLocator contentLocator )
    {
        super( repository, request );
        this.contentLocator = contentLocator;
    }

    /**
     * Instantiates a new default storage file item.
     * 
     * @param RepositoryRouter router
     * @param path the path
     * @param canRead the can read
     * @param canWrite the can write
     * @param contentLocator the content locator
     */
    public DefaultStorageFileItem( RepositoryRouter router, ResourceStoreRequest request, ContentLocator contentLocator )
    {
        super( router, request );
        this.contentLocator = contentLocator;
    }

    public long getLength()
    {
        return length;
    }

    public void setLength( long length )
    {
        this.length = length;
    }

    public String getMimeType()
    {
        return getContentLocator().getMimeType();
    }

    public boolean isReusableStream()
    {
        return getContentLocator().isReusable();
    }

    public InputStream getInputStream()
        throws IOException
    {
        return getContentLocator().getContent();
    }

    public void setContentLocator( ContentLocator locator )
    {
        this.contentLocator = locator;
    }

    public ContentLocator getContentLocator()
    {
        return this.contentLocator;
    }
}
