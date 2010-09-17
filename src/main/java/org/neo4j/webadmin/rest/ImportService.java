package org.neo4j.webadmin.rest;

import static org.neo4j.rest.domain.JsonHelper.jsonToMap;
import static org.neo4j.webadmin.rest.WebUtils.addHeaders;
import static org.neo4j.webadmin.rest.WebUtils.buildExceptionResponse;

import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.neo4j.rest.domain.JsonRenderers;
import org.neo4j.webadmin.task.ImportTask;

/**
 * Handles importing graphml into the underlying database, either by uploading
 * or file or by providing a url to a graphml file.
 * 
 * @author Jacob Hansson <jacob@voltvoodoo.com>
 * 
 */
@Path( ImportService.ROOT_PATH )
public class ImportService
{

    public static final String ROOT_PATH = "/server/import";
    public static final String IMPORT_UPLOAD_PATH = "";
    public static final String IMPORT_URL_PATH = "/url";

    public static final String URL_KEY = "url";

    @POST
    @Produces( MediaType.APPLICATION_JSON )
    @Consumes( MediaType.APPLICATION_JSON )
    @Path( IMPORT_URL_PATH )
    public Response importFromUrl( String json )
    {

        try
        {
            Map<String, Object> req = jsonToMap( json );

            if ( req.containsKey( URL_KEY ) )
            {
                ImportTask task = new ImportTask( req.get( URL_KEY ).toString() );
                task.run();
            }
            else
            {
                throw new IllegalArgumentException(
                        "You have to specify a url." );
            }

            return addHeaders( Response.ok() ).build();
        }
        catch ( Exception e )
        {
            return buildExceptionResponse( Status.BAD_REQUEST,
                    "Request failed.", e, JsonRenderers.DEFAULT );
        }
    }

}
