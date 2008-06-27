package org.astrogrid.samp;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

public class Metadata extends SampMap {

    public static final String NAME_KEY = "samp.name";
    public static final String DESCTEXT_KEY = "samp.description.text";
    public static final String DESCHTML_KEY = "samp.description.html";
    public static final String ICONURL_KEY = "samp.icon.url";
    public static final String DOCURL_KEY = "samp.documentation.url";

    public Metadata() {
        super();
    }

    public Metadata( Map map ) {
        super( map );
    }

    public void setName( String name ) {
        put( NAME_KEY, name );
    }

    public String getName() {
        return (String) get( NAME_KEY );
    }

    public void setDescriptionText( String txt ) {
        put( DESCTEXT_KEY, txt );
    }

    public String getDescriptionText() {
        return (String) get( DESCTEXT_KEY );
    }

    public void setDescriptionHtml( String html ) {
        put( DESCHTML_KEY, html );
    }

    public String getDescriptionHtml() {
        return (String) get( DESCHTML_KEY );
    }

    public void setIconUrl( String url ) {
        put( ICONURL_KEY, url );
    }

    public URL getIconUrl() {
        return getUrl( ICONURL_KEY );
    }

    public void setDocumentationUrl( String url ) {
        put( DOCURL_KEY, url );
    }

    public URL getDocumentationUrl() {
        return getUrl( DOCURL_KEY );
    }

    public void check() {
        super.check();
        SampUtils.checkUrl( getString( DOCURL_KEY ) );
        SampUtils.checkUrl( getString( ICONURL_KEY ) );
    }

    public static Metadata asMetadata( Map map ) {
        return ( map instanceof Metadata || map == null )
             ? (Metadata) map
             : new Metadata( map );
    }
}
