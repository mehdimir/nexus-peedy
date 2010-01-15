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
package org.sonatype.nexus.configuration.application;

import java.io.File;
import java.io.IOException;

import org.sonatype.nexus.configuration.model.Configuration;
import org.sonatype.nexus.proxy.storage.remote.RemoteStorageContext;

/**
 * ApplicationConfiguration is the main component to have and maintain configuration.
 */
public interface ApplicationConfiguration
{
    /**
     * Gets the working directory as file. The directory is created if needed and is guaranteed to exists.
     * 
     * @return
     */
    File getWorkingDirectory();

    /**
     * Gets the working directory with some subpath. The directory is created and is guaranteed to exists.
     * 
     * @param key
     * @return
     */
    File getWorkingDirectory( String key );

    /**
     * Returns the configuration directory. It defaults to $NEXUS_WORK/conf.
     * 
     * @return
     */
    File getConfigurationDirectory();

    /**
     * Returns the temporary directory.
     * 
     * @return
     */
    File getTemporaryDirectory();

    /**
     * Returns the wastebasket directory.
     * 
     * @return
     */
    File getWastebasketDirectory();

    /**
     * Is security enabled?
     * 
     * @return
     */
    @Deprecated
    boolean isSecurityEnabled();

    /**
     * Gets the top level remote storage context.
     * 
     * @return
     */
    RemoteStorageContext getGlobalRemoteStorageContext();

    /**
     * Saves the configuration.
     * 
     * @throws IOException
     */
    void saveConfiguration()
        throws IOException;

    /**
     * Gets the Configuration object.
     * 
     * @return
     * @deprecated you should use setters/getters directly on Configurable instances, and not tampering with
     *             Configuration model directly!
     */
    Configuration getConfigurationModel();
}
