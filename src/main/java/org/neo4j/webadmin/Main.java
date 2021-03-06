package org.neo4j.webadmin;

import java.io.IOException;

import org.neo4j.helpers.Args;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.webadmin.backup.BackupManager;
import org.neo4j.webadmin.rrd.RrdManager;
import org.neo4j.webadmin.rrd.RrdSampler;
import org.quartz.SchedulerException;

/**
 * Main entry point for the neo4j stand-alone REST system with web
 * administration. This code based on Main class for the REST neo4j
 * distribution.
 * 
 * @author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
public class Main
{

    public static void main( String[] strArgs ) throws Exception
    {
        try
        {
            Args args = new Args( strArgs );

            //
            // 1. ARGUMENT HANDLING
            //

            System.setProperty( "org.neo4j.webadmin.developmentmode",
                    args.get( "development", "false" ) );
            System.setProperty( "org.neo4j.graphdb.location",
                    args.get( "dbPath", "neo4j-rest-db" ) );
            System.setProperty( "org.neo4j.webadmin.rrdb.location",
                    args.get( "rrdbPath", "neo4j-rrdb" ) );

            int restPort = args.getNumber( "restPort",
                    WebServerFactory.DEFAULT_PORT ).intValue();
            int adminPort = args.getNumber( "adminPort",
                    AdminServer.DEFAULT_PORT ).intValue();
            String db_location = System.getProperty( "org.neo4j.graphdb.location");
            System.out.println("graphdb-location: " + db_location);

            String webRoot = args.get( "webRoot",
                    AdminServer.DEFAULT_STATIC_PATH );

            //
            // 2. START SERVERS
            //

            System.out.println( "Starting web servers.." );

            WebServerFactory.getDefaultWebServer().startServer( restPort );
            AdminServer.INSTANCE.startServer( adminPort, webRoot );

            System.out.println( "Starting backup scheduler.." );

            BackupManager.INSTANCE.start();

            System.out.println( "Starting round-robin system state sampler.." );

            RrdSampler.INSTANCE.start();

            System.out.println( String.format( "Running database at [%s]",
                    System.getProperty( "org.neo4j.graphdb.location" ) ) );

            System.out.println( String.format( "Running REST at [%s]",
                    WebServerFactory.getLocalhostBaseUri( restPort ) ) );
            System.out.println( String.format(
                    "Running admin interface at [%s]",
                    AdminServer.getLocalhostBaseUri( adminPort ) ) );
            System.out.println( "\nPress Ctrl-C to kill the server" );

            //
            // 3. AWAIT THE GRIM REAPER
            //

            Runtime.getRuntime().addShutdownHook( new Thread()
            {
                @Override
                public void run()
                {
                    shutdown();
                }
            } );

        }
        catch ( Exception e )
        {
            System.out.println( "\nStartup failed:" );
            e.printStackTrace();
            shutdown();
        }

    }

    private static void shutdown()
    {
        try
        {
            // Kill the round robin sampler
            System.out.println( "\nShutting down the round robin database" );
            RrdSampler.INSTANCE.stop();
            RrdManager.getRrdDB().close();
        }
        catch ( IOException e )
        {
            e.printStackTrace();
        }

        System.out.println( "Shutting down backup scheduler.." );
        try
        {
            BackupManager.INSTANCE.stop();
        }
        catch ( SchedulerException e )
        {
            e.printStackTrace();
        }

        // Kill the REST-server
        System.out.println( "Shutting down the REST server.." );
        WebServerFactory.getDefaultWebServer().stopServer();
        DatabaseLocator.shutdownAndBlockGraphDatabase();

        // Kill the admin-server
        System.out.println( "Shutting down the admin server.." );
        AdminServer.INSTANCE.stopServer();

        System.out.println( "Shutdown complete." );
    }
}
