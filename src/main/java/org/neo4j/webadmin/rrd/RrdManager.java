package org.neo4j.webadmin.rrd;

import java.io.File;
import java.io.IOException;

import org.rrd4j.ConsolFun;
import org.rrd4j.DsType;
import org.rrd4j.core.RrdDb;
import org.rrd4j.core.RrdDef;

/**
 * A singleton pre-configured round-robin database. This stores various data
 * points over time. As data points get older, they will be aggregated together,
 * enabling the round robin database to store data points over a massive
 * timespan in very little space.
 * 
 * Basically, the older the data gets, the more coarse grained it becomes, thus
 * taking less space.
 * 
 * 
 * @author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
public class RrdManager
{

    public static final String RRDB_FILENAME = "neo4j.rrdb";

    // DATA SOURCE HANDLES

    public static final String NODE_CACHE_SIZE = "node_cache_size";
    public static final String NODE_COUNT = "node_count";
    public static final String RELATIONSHIP_COUNT = "relationship_count";
    public static final String PROPERTY_COUNT = "property_count";

    /**
     * Singleton instance of central round robin database.
     */
    private static RrdDb INSTANCE;

    public static RrdDb getRrdDB()
    {
        if ( INSTANCE == null )
        {

            try
            {
                // CREATE RRD DEFINITION
                RrdDef rrdDef = new RrdDef( getDbFilePath(), 3000 );

                rrdDef.setVersion( 2 );

                // DEFINE DATA SOURCES

                rrdDef.addDatasource( NODE_CACHE_SIZE, DsType.GAUGE, 3000, 0,
                        Long.MAX_VALUE );

                rrdDef.addDatasource( NODE_COUNT, DsType.GAUGE, 3000, 0,
                        Long.MAX_VALUE );

                rrdDef.addDatasource( RELATIONSHIP_COUNT, DsType.GAUGE, 3000,
                        0, Long.MAX_VALUE );

                rrdDef.addDatasource( PROPERTY_COUNT, DsType.GAUGE, 3000, 0,
                        Long.MAX_VALUE );

                // DEFINE ARCHIVES

                rrdDef.addArchive( ConsolFun.AVERAGE, 0.5, 1, 50 );
                rrdDef.addArchive( ConsolFun.AVERAGE, 0.5, 10, 50 );

                // INSTANTIATE

                INSTANCE = new RrdDb( rrdDef );
            }
            catch ( IOException e )
            {
                throw new RuntimeException(
                        "IO Error trying to access round robin database path. See nested exception.",
                        e );
            }
        }

        return INSTANCE;
    }

    //
    // INTERNALS
    //

    /**
     * Get database path. Create any missing folders on the path.
     */
    public static String getDbFilePath() throws IOException
    {
        File dbPath = new File(
                System.getProperty( "org.neo4j.webadmin.rrdb.location" ) );

        if ( !dbPath.exists() && !dbPath.mkdirs() )
        {
            throw new IllegalStateException(
                    "Unable to use round-robin path '" + dbPath.toString()
                            + "'. Does user have write permissions?" );
        }

        return new File( dbPath, RRDB_FILENAME ).getAbsolutePath();
    }
}
