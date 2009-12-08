/*
 * Nexus: Maven Repository Manager
 * Copyright (C) 2008 Sonatype Inc.                                                                                                                          
 * 
 * This file is part of Nexus.                                                                                                                                  
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 */
package org.sonatype.nexus.proxy.cache;

import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.AbstractLogEnabled;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.Startable;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StartingException;
import org.codehaus.plexus.personality.plexus.lifecycle.phase.StoppingException;

/**
 * The Class EhCacheCacheManager is a thin wrapper around EhCache, just to make things going.
 * 
 * @author cstamas
 */
@Component( role = CacheManager.class )
public class EhCacheCacheManager
    extends AbstractLogEnabled
    implements CacheManager, Startable
{
    private net.sf.ehcache.CacheManager ehCacheManager;

    public static final String SINGLE_PATH_CACHE_NAME = "path-cache";

    public PathCache getPathCache( String cache )
    {
        if ( !getCacheManager().cacheExists( SINGLE_PATH_CACHE_NAME ) )
        {
            getCacheManager().addCache( SINGLE_PATH_CACHE_NAME );
        }

        return new EhCachePathCache( cache, getCacheManager().getEhcache( SINGLE_PATH_CACHE_NAME ) );
    }

    public void start()
        throws StartingException
    {
        ehCacheManager = new net.sf.ehcache.CacheManager();
    }

    public void stop()
        throws StoppingException
    {
        ehCacheManager.shutdown();
    }

    protected net.sf.ehcache.CacheManager getCacheManager()
    {
        return ehCacheManager;
    }
}
