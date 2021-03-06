package org.neo4j.webadmin.rest;

import static org.neo4j.webadmin.rest.WebUtils.addHeaders;
import static org.neo4j.webadmin.rest.WebUtils.buildBadJsonExceptionResponse;
import static org.neo4j.webadmin.rest.WebUtils.buildExceptionResponse;
import static org.neo4j.webadmin.rest.WebUtils.dodgeStartingUnicodeMarker;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.naming.OperationNotSupportedException;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.rest.domain.JsonHelper;
import org.neo4j.rest.domain.JsonRenderers;
import org.neo4j.rest.domain.PropertyValueException;
import org.neo4j.webadmin.console.ConsoleSessions;
import org.neo4j.webadmin.domain.ConfigServiceRepresentation;
import org.neo4j.webadmin.domain.LifecycleRepresentation;
import org.neo4j.webadmin.domain.NoSuchPropertyException;
import org.neo4j.webadmin.domain.ServerPropertyRepresentation;
import org.neo4j.webadmin.properties.ServerConfiguration;
import org.neo4j.webadmin.task.DeferredTask;
import org.neo4j.webadmin.task.JvmRestartTask;

/**
 * A web service that exposes various configuration settings for a running neo4j
 * REST server.
 * 
 * @author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
@Path( ConfigService.ROOT_PATH )
public class ConfigService
{

    public static final String ROOT_PATH = "/server/config";
    public static final String ALL_SETTINGS_PATH = "/all";

    protected ServerConfiguration properties;

    //
    // CONSTRUCT
    //

    public ConfigService() throws IOException
    {
        properties = ServerConfiguration.getInstance();
    }

    @GET
    @Produces( MediaType.APPLICATION_JSON )
    public Response getServiceDefinition( @Context UriInfo uriInfo )
    {

        String entity = JsonRenderers.DEFAULT.render( new ConfigServiceRepresentation(
                uriInfo.getBaseUri() ) );

        return addHeaders(
                Response.ok( entity, JsonRenderers.DEFAULT.getMediaType() ) ).build();
    }

    //
    // PUBLIC
    //

    /**
     * Get a full list of available settings.
     */
    @GET
    @Produces( MediaType.APPLICATION_JSON )
    @Path( ALL_SETTINGS_PATH )
    public synchronized Response listAll()
    {

        String entity = JsonRenderers.DEFAULT.render( properties );

        return addHeaders(
                Response.ok( entity, JsonRenderers.DEFAULT.getMediaType() ) ).build();

    }

    @POST
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( ALL_SETTINGS_PATH )
    public Response jsonSetMany( String data )
    {
        return setMany( data );
    }

    @POST
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_FORM_URLENCODED )
    @Path( ALL_SETTINGS_PATH )
    public Response formSetMany( @FormParam( "value" ) String data )
    {
        return setMany( data );
    }

    //
    // INTERNALS
    //

    /**
     * Set one or more settings at once.
     * 
     * This method will validate all changes to make, if they pass they will all
     * be applied and appropriate action will be taken to apply changes. This
     * will usually entail restarting the REST server, but can also mean
     * re-spawning the JVM that the whole system runs in.
     * 
     * WARNING: Certain settings are only applicable when creating a neo4j
     * database. In the future, changing such a setting will in fact lead to the
     * original database being destroyed and a new one to be created in it's
     * place. The plan is to create a new database and then copy data from the
     * old one to the new, but what the end result will be is not yet decided.
     * 
     * @param json is a json array of objects with a "key" and a "value"
     *            property each. Key maps to the setting to change, value should
     *            be the new value to change it to.
     */
    @SuppressWarnings( "unchecked" )
    protected Response setMany( String json )
    {
        try
        {
            json = dodgeStartingUnicodeMarker( json );
            Collection<Object> newProperties = (Collection<Object>) JsonHelper.jsonToSingleValue( json );

            // Validate all properties
            Map<String, Object> currentPropMap;
            ServerPropertyRepresentation currentPropObj;

            boolean hasJvmChanges = false;
            boolean hasCreationChanges = false;
            boolean hasDbConfigChanges = false;

            for ( Object property : newProperties )
            {
                if ( !( property instanceof Map<?, ?> ) )
                {
                    throw new IllegalArgumentException(
                            "'"
                                    + property
                                    + "' is not a valid configuration directive." );
                }

                currentPropMap = (Map<String, Object>) property;
                currentPropObj = properties.get( (String) currentPropMap.get( "key" ) );

                if ( !currentPropObj.isValidValue( (String) currentPropMap.get( "value" ) ) )
                {
                    throw new IllegalArgumentException(
                            "'" + (String) currentPropMap.get( "value" )
                                    + "' is not a valid value for property '"
                                    + (String) currentPropMap.get( "key" )
                                    + "'." );
                }

                // Keep track of what type of changes we are making
                switch ( currentPropObj.getType() )
                {
                case APP_ARGUMENT:
                case JVM_ARGUMENT:
                    hasJvmChanges = true;
                    break;
                case DB_CREATION_PROPERTY:
                    hasCreationChanges = true;
                    break;
                case CONFIG_PROPERTY:
                    hasDbConfigChanges = true;
                    break;
                }
            }

            // Everything is valid, apply properties
            for ( Object property : newProperties )
            {

                currentPropMap = (Map<String, Object>) property;
                properties.set( (String) currentPropMap.get( "key" ),
                        (String) currentPropMap.get( "value" ) );
            }

            // All changes applied, perform required restarts
            if ( hasCreationChanges )
            {
                throw new OperationNotSupportedException();
            }
            else if ( hasJvmChanges )
            {
                // Client has changed settings that require a JVM restart
                DeferredTask.defer( new JvmRestartTask(), 10 );
            }
            else if ( hasDbConfigChanges )
            {
                // Client has changed settings that only require REST-server
                // restart.
                if ( LifecycleService.serverStatus == LifecycleRepresentation.Status.RUNNING )
                {

                    int restPort = WebServerFactory.getDefaultWebServer().getPort();

                    WebServerFactory.getDefaultWebServer().stopServer();
                    DatabaseLocator.shutdownGraphDatabase();
                    WebServerFactory.getDefaultWebServer().startServer(
                            restPort );
                    ConsoleSessions.destroyAllSessions();
                }
            }

            return addHeaders( Response.ok() ).build();
        }
        catch ( PropertyValueException e )
        {
            return buildBadJsonExceptionResponse( json, e,
                    JsonRenderers.DEFAULT );
        }
        catch ( OperationNotSupportedException e )
        {
            return buildExceptionResponse(
                    Status.FORBIDDEN,
                    "Changing settings that required database re-creation is currently not supported.",
                    e, JsonRenderers.DEFAULT );
        }
        catch ( IllegalArgumentException e )
        {
            return buildExceptionResponse( Status.BAD_REQUEST,
                    "You attempted to set an illegal value.", e,
                    JsonRenderers.DEFAULT );
        }
        catch ( NoSuchPropertyException e )
        {
            return buildExceptionResponse( Status.BAD_REQUEST,
                    "You attempted to modify a property that does not exist.",
                    e, JsonRenderers.DEFAULT );
        }
        catch ( IOException e )
        {
            return buildExceptionResponse(
                    Status.INTERNAL_SERVER_ERROR,
                    "Unable to save changes to disk, does daemon user have write permissions?",
                    e, JsonRenderers.DEFAULT );
        }
    }
}
