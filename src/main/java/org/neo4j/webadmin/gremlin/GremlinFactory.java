package org.neo4j.webadmin.gremlin;

import java.net.URI;
import java.net.URISyntaxException;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.webadmin.Main;

import com.tinkerpop.blueprints.pgm.TransactionalGraph;
import com.tinkerpop.blueprints.pgm.impls.neo4j.Neo4jGraph;
import com.tinkerpop.gremlin.GremlinScriptEngine;

/**
 * Builds gremlin evaluators that come pre-packaged with astonishing connective
 * powers. Such powers include, but are not limited to, connecting to the REST
 * neo4j instance running under the hood of the webadmin system.
 * 
 * @author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
@SuppressWarnings( "restriction" )
public class GremlinFactory
{

    protected volatile static boolean initiated = false;

    public static ScriptEngine createGremlinScriptEngine()
    {
        try
        {
            ScriptEngine engine = new GremlinScriptEngine();

            // Inject the local database
            GraphDatabaseService dbInstance = DatabaseLocator.getGraphDatabase( new URI(
                    WebServerFactory.getLocalhostBaseUri( Main.restPort ) ) );

            TransactionalGraph graph = new Neo4jGraph( dbInstance,
                    DatabaseLocator.getIndexService( dbInstance ) );

            engine.getBindings( ScriptContext.ENGINE_SCOPE ).put( "$_g", graph );

            try
            {
                engine.getBindings( ScriptContext.ENGINE_SCOPE ).put( "$_",
                        graph.getVertex( 0l ) );
            }
            catch ( Exception e )
            {
                // Om-nom-nom
            }

            return engine;
        }
        catch ( URISyntaxException e )
        {
            throw new RuntimeException(
                    "Db path is corrupt, see nested exception.", e );
        }
        catch ( RuntimeException e )
        {
            throw e;
        }
    }

    protected synchronized void ensureInitiated()
    {
        if ( initiated == false )
        {
            new GremlinGarbageCollector();
        }
    }
}
