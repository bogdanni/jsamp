package org.astrogrid.samp.web;

import java.io.IOException;
import java.io.PrintStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.logging.Logger;
import org.astrogrid.samp.Message;
import org.astrogrid.samp.client.ClientProfile;
import org.astrogrid.samp.httpd.HttpServer;
import org.astrogrid.samp.hub.HubProfile;
import org.astrogrid.samp.hub.KeyGenerator;
import org.astrogrid.samp.xmlrpc.internal.InternalServer;
import org.astrogrid.samp.xmlrpc.internal.RpcLoggingInternalServer;
import org.astrogrid.samp.xmlrpc.internal.XmlLoggingInternalServer;

/**
 * HubProfile implementation for Web Profile.
 *
 * @author   Mark Taylor
 * @since    2 Feb 2011
 */
public class WebHubProfile implements HubProfile {

    private final ServerFactory serverFactory_;
    private final ClientAuthorizer auth_;
    private final KeyGenerator keyGen_;
    private final boolean controlUrls_;
    private InternalServer xServer_;
    private WebHubXmlRpcHandler wxHandler_;
    private static final Logger logger_ =
        Logger.getLogger( WebHubProfile.class.getName() );

    /**
     * Constructs a profile with configuration options.
     *
     * @param   serverFactory  factory for server providing HTTP
     *                         and XML-RPC implementation
     * @param   auth  client authorizer implementation
     * @param   keyGen  key generator for private keys
     */
    public WebHubProfile( ServerFactory serverFactory, ClientAuthorizer auth,
                          KeyGenerator keyGen, boolean controlUrls ) {
        serverFactory_ = serverFactory;
        auth_ = auth;
        keyGen_ = keyGen;
        controlUrls_ = controlUrls;
    }

    /**
     * Constructs a profile with default configuration.
     */
    public WebHubProfile() throws IOException {
        this( new ServerFactory(), new HubSwingClientAuthorizer( null ),
              createKeyGenerator(), true );
    }

    public String getProfileName() {
        return "Web";
    }

    public synchronized void start( ClientProfile profile ) throws IOException {
        if ( isRunning() ) {
            logger_.info( "Profile already running" );
            return;
        }
        xServer_ = serverFactory_.createSampXmlRpcServer();
        HttpServer hServer = xServer_.getHttpServer();
        WebHubXmlRpcHandler wxHandler =
            new WebHubXmlRpcHandler( profile, auth_, keyGen_,
                                     hServer.getBaseUrl(),
                                     controlUrls_ ? new UrlTracker() : null );
        xServer_.addHandler( wxHandler );
        hServer.addHandler( wxHandler.getUrlTranslationHandler() );
        hServer.start();
    }

    public synchronized boolean isRunning() {
        return xServer_ != null;
    }

    public synchronized void stop() {
        if ( ! isRunning() ) {
            logger_.info( "Profile already stopped" );
            return;
        }
        xServer_.getHttpServer().stop();
        xServer_ = null;
    }

    /**
     * Convenience method to return a new key generator
     * suitable for use with a WebHubProfile.
     *
     * @return  new key generator for web hub private keys
     */
    public static KeyGenerator createKeyGenerator() {
        return new KeyGenerator( "wk:", 24, KeyGenerator.createRandom() );
    }

    /**
     * Creates and configures the HTTP server on which the Web Profile resides.
     */
    public static class ServerFactory {
        private String logType_;
        private int port_;
        private String xmlrpcPath_;
        private boolean allowFlash_;
        private boolean allowSilverlight_;
        private OriginAuthorizer oAuth_;

        /**
         * Constructs a ServerFactory with default properties.
         */
        public ServerFactory() {
            logType_ = null;
            port_ = WebClientProfile.WEBSAMP_PORT;
            xmlrpcPath_ = WebClientProfile.WEBSAMP_PATH;
            allowFlash_ = true;
            allowSilverlight_ = false;
            oAuth_ = OriginAuthorizers.TRUE;
        }
        
        /**
         * Returns a new internal server.
         *
         * @return   new server for use with WebHubProfile
         */
        public InternalServer createSampXmlRpcServer() throws IOException {
            String path = getXmlrpcPath();
            ServerSocket socket = createServerSocket( getPort() );
            String logType = getLogType();
            OriginAuthorizer oAuth = getOriginAuthorizer();
            PrintStream logOut = System.err;
            CorsHttpServer hServer = "http".equals( logType )
                                   ? new LoggingCorsHttpServer( socket, oAuth,
                                                                logOut )
                                   : new CorsHttpServer( socket, oAuth );
            if ( isAllowFlash() ) {
                hServer.addHandler( OpenPolicyResourceHandler
                                   .createFlashPolicyHandler( oAuth ) );
                logger_.info( "Web Profile HTTP server permits "
                            + "Flash-style cross-domain access" );
            }
            else {
                logger_.info( "Web Profile HTTP server does not permit "
                            + "Flash-style cross-domain access" );
            }
            if ( isAllowSilverlight() ) {
                hServer.addHandler( OpenPolicyResourceHandler
                                   .createSilverlightPolicyHandler( oAuth ) );
                logger_.info( "Web Profile HTTP server permits "
                            + "Silverlight-style cross-domain access" );
            }
            else {
                logger_.info( "Web Profile HTTP server does not permit "
                            + "Silverlight-style cross-domain access" );
            }
            hServer.setDaemon( true );
            if ( "rpc".equals( logType ) ) {
                return new RpcLoggingInternalServer( hServer, path, logOut );
            }
            else if ( "xml".equals( logType ) ) {
                return new XmlLoggingInternalServer( hServer, path, logOut );
            }
            else if ( "none".equals( logType ) || "http".equals( logType ) ||
                      logType == null || logType.length() == 0 ) {
                return new InternalServer( hServer, path );
            }
            else {
                throw new IllegalArgumentException( "Unknown logType "
                                                  + logType );
            }
        }

        /**
         * Sets the type of logging to use.
         *
         * @param  logType  logging type;
         *                  may be "http", "rpc", "xml", "none" or null
         */
        public void setLogType( String logType ) {
            if ( logType == null ||
                 logType.equals( "http" ) ||
                 logType.equals( "rpc" ) ||
                 logType.equals( "xml" ) ||
                 logType.equals( "none" ) ) {
                logType_ = logType;
            }
            else {
                throw new IllegalArgumentException( "Unknown log type "
                                                  + logType );
            }
        }

        /**
         * Returns the type of logging to use.
         *
         * @return  logging type; may be "http", "rpc", "xml", "none" or null
         */
        public String getLogType() {
            return logType_;
        }

        /**
         * Sets the port number the server will run on.
         * If port=0, then an unused port will be used at run time.
         *
         * @param  port  port number
         */
        public void setPort( int port ) {
            port_ = port;
        }

        /**
         * Returns the port number the server will run on.
         *
         * @return  port number
         */
        public int getPort() {
            return port_;
        }

        /**
         * Sets the path on the HTTP server at which the XML-RPC server
         * will reside.
         *
         * @param  xmlrpcPath  server path for XML-RPC server
         */
        public void setXmlrpcPath( String xmlrpcPath ) {
            xmlrpcPath_ = xmlrpcPath;
        }

        /**
         * Returns the path on the HTTP server at which the XML-RPC server
         * will reside.
         *
         * @return   XML-RPC path on server
         */
        public String getXmlrpcPath() {
            return xmlrpcPath_;
        }

        /**
         * Sets whether Adobe Flash cross-domain workaround will be supported.
         *
         * @param  allowFlash  true iff supported
         */
        public void setAllowFlash( boolean allowFlash ) {
            allowFlash_ = allowFlash;
        }

        /**
         * Indicates whether Adobe Flash cross-domain workaround
         * will be supported.
         *
         * @return  true iff supported
         */
        public boolean isAllowFlash() {
            return allowFlash_;
        }

        /**
         * Sets whether Microsoft Silverlight cross-domain workaround
         * will be supported.
         *
         * @param  allowSilverlight  true iff supported
         */
        public void setAllowSilverlight( boolean allowSilverlight ) {
            allowSilverlight_ = allowSilverlight;
        }

        /**
         * Indicates whether Microsoft Silverlight cross-domain workaround
         * will be supported.
         *
         * @return  true iff supported
         */
        public boolean isAllowSilverlight() {
            return allowSilverlight_;
        }

        /**
         * Sets the authorization policy for external origins.
         *
         * @param  oAuth  authorizer
         */
        public void setOriginAuthorizer( OriginAuthorizer oAuth ) {
            oAuth_ = oAuth;
        }

        /**
         * Returns the authorization policy for external origins.
         *
         * @return  authorizer
         */
        public OriginAuthorizer getOriginAuthorizer() {
            return oAuth_;
        }

        /**
         * Creates a socket on a given port to be used by the server this
         * object produces.
         *
         * @param  port  port number
         * @return  new server socket
         */
        protected ServerSocket createServerSocket( int port )
                throws IOException {
            ServerSocket sock = new ServerSocket();
            sock.setReuseAddress( true );
            sock.bind( new InetSocketAddress( port ) );
            return sock;
        }
    }
}
