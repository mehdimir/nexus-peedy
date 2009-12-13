package org.sonatype.nexus.vui.app;

import org.sonatype.nexus.vui.Feature;
import org.sonatype.nexus.vui.app.ActiveLink.LinkActivatedEvent;

import com.vaadin.terminal.ExternalResource;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;

@SuppressWarnings( "serial" )
public class BreadCrumbs
    extends CustomComponent
    implements ActiveLink.LinkActivatedListener
{
    private final NexusVuiApplication nexusVuiApplication;

    private final HorizontalLayout layout;

    public BreadCrumbs( NexusVuiApplication nexusVuiApplication )
    {
        this.nexusVuiApplication = nexusVuiApplication;
        this.layout = new HorizontalLayout();
        this.layout.setSpacing( true );
        setCompositionRoot( layout );
        setStyleName( "breadcrumbs" );
        setPath( null );
    }

    public void setPath( String path )
    {
        // could be optimized: always builds path from scratch
        layout.removeAllComponents();

        { // home
            ActiveLink link = new ActiveLink( "Home", new ExternalResource( "#" ) );
            link.addListener( this );
            layout.addComponent( link );
        }

        if ( path != null && !"".equals( path ) )
        {
            String parts[] = path.split( "/" );
            String current = "";
            ActiveLink link = null;
            for ( int i = 0; i < parts.length; i++ )
            {
                layout.addComponent( new Label( "&raquo;", Label.CONTENT_XHTML ) );
                current += ( i > 0 ? "/" : "" ) + parts[i];
                Feature f = nexusVuiApplication.getFeatureByPath( current );
                link = new ActiveLink( f.getName(), new ExternalResource( "#" + nexusVuiApplication.getPathFor( f ) ) );
                link.setData( f );
                link.addListener( this );
                layout.addComponent( link );
            }
            if ( link != null )
            {
                link.setStyleName( "bold" );
            }
        }

    }

    public void linkActivated( LinkActivatedEvent event )
    {
        if ( !event.isLinkOpened() )
        {
            ( (NexusVuiWindow) getWindow() ).setFeature( (Feature) event.getActiveLink().getData() );
        }
    }
}
