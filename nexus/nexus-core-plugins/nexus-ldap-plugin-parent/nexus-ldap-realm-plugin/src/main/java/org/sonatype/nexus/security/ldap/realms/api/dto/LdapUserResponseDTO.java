/**
 * Sonatype Nexus (TM) Professional Version.
 * Copyright (c) 2008 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://www.sonatype.com/products/nexus/attributions/.
 * "Sonatype" and "Sonatype Nexus" are trademarks of Sonatype, Inc.
 */
package org.sonatype.nexus.security.ldap.realms.api.dto;

import java.util.ArrayList;

public class LdapUserResponseDTO
{
    /**
     * User ID.  The id of the user.
     */
    private String userId;
    
    /**
     * Field name
     */
    private String name;

    /**
     * Field email
     */
    private String email;

    /**
     * Field roles.
     */
    private java.util.List<String> roles = new ArrayList<String>();

    /**
     * @return the userId
     */
    public String getUserId()
    {
        return userId;
    }

    /**
     * @param userId the userId to set
     */
    public void setUserId( String userId )
    {
        this.userId = userId;
    }

    /**
     * @return the roles
     */
    public java.util.List<String> getRoles()
    {
        return roles;
    }

    /**
     * @param roles the roles to set
     */
    public void setRoles( java.util.List<String> roles )
    {
        this.roles = roles;
    }
    
    public void addRole(String role)
    {
        this.roles.add( role );
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName( String name )
    {
        this.name = name;
    }


    /**
     * @return the email
     */
    public String getEmail()
    {
        return email;
    }

    /**
     * @param email the email to set
     */
    public void setEmail( String email )
    {
        this.email = email;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ( ( email == null ) ? 0 : email.hashCode() );
        result = prime * result + ( ( name == null ) ? 0 : name.hashCode() );
        result = prime * result + ( ( roles == null ) ? 0 : roles.hashCode() );
        result = prime * result + ( ( userId == null ) ? 0 : userId.hashCode() );
        return result;
    }

    /* (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj )
    {
        if ( this == obj )
            return true;
        if ( obj == null )
            return false;
        if ( getClass() != obj.getClass() )
            return false;
        final LdapUserResponseDTO other = (LdapUserResponseDTO) obj;
        if ( email == null )
        {
            if ( other.email != null )
                return false;
        }
        else if ( !email.equals( other.email ) )
            return false;
        if ( name == null )
        {
            if ( other.name != null )
                return false;
        }
        else if ( !name.equals( other.name ) )
            return false;
        if ( roles == null )
        {
            if ( other.roles != null )
                return false;
        }
        else if ( !roles.equals( other.roles ) )
            return false;
        if ( userId == null )
        {
            if ( other.userId != null )
                return false;
        }
        else if ( !userId.equals( other.userId ) )
            return false;
        return true;
    }
    
    
}
