package org.sonatype.nexus.remote.domain;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Status
{
    private String version;

    public String getVersion()
    {
        return version;
    }

    public void setVersion( String version )
    {
        this.version = version;
    }

}
