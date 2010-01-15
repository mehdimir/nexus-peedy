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
package org.sonatype.nexus.proxy.access;

import org.sonatype.nexus.proxy.AccessDeniedException;
import org.sonatype.nexus.proxy.AuthorizationException;
import org.sonatype.nexus.proxy.ResourceStoreRequest;
import org.sonatype.nexus.proxy.repository.Repository;

/**
 * Interface for access manager.
 * 
 * @author t.cservenak
 */
public interface AccessManager
{
    /**
     * Key used for authenticated username in request.
     */
    String REQUEST_USER = "request.user";

    /**
     * Key used for request source address.
     */
    String REQUEST_REMOTE_ADDRESS = "request.address";

    /**
     * Key used to mark is the request coming over confidential channel (https).
     */
    String REQUEST_CONFIDENTIAL = "request.isConfidential";

    /**
     * Key used to mark the request certificates of confidential channel (https).
     */
    String REQUEST_CERTIFICATES = "request.certificates";

    /**
     * The implementation of this method should throw AccessDeniedException or any subclass if it denies access.
     * 
     * @throws AccessDeniedException the access denied exception
     */
    void decide( Repository repository, ResourceStoreRequest request, Action action )
        throws AuthorizationException;
}
