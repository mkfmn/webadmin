package org.neo4j.webadmin.backup;

import static org.neo4j.rest.domain.JsonHelper.createJsonFrom;
import static org.neo4j.rest.domain.JsonHelper.jsonToMap;
import static org.neo4j.webadmin.utils.FileUtils.getFileAsString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.neo4j.rest.domain.Representation;
import org.neo4j.webadmin.domain.BackupJobDescriptionRepresentation;

public class BackupConfig implements Representation
{
    public static final String JOB_LIST_KEY = "jobList";

    private File configFile;
    private ArrayList<BackupJobDescription> descs = new ArrayList<BackupJobDescription>();
    private int idCount = 0;

    //
    // CONSTRUCT
    //

    public BackupConfig( File configFile ) throws IOException
    {
        this.configFile = configFile;
        load();
    }

    //
    // PUBLIC
    //

    public ArrayList<BackupJobDescription> getJobDescriptions()
    {
        return descs;
    }

    /**
     * Add or edit a job description.
     * 
     * @param desc
     * @throws IOException
     */
    public void setJobDescription( BackupJobDescription desc )
            throws IOException
    {

        if ( desc.getId() != null )
        {
            removeJobDescription( desc.getId() );
            if ( idCount < desc.getId() )
            {
                idCount = desc.getId();
            }

            BackupManager.INSTANCE.getLog().logInfo( new Date(), desc,
                    "Settings changed." );
        }
        else
        {
            desc.setId( ++idCount );
        }

        descs.add( desc );
        persist();
    }

    public BackupJobDescription getJobDescription( Integer id )
    {
        for ( BackupJobDescription desc : descs )
        {
            if ( desc.getId() == id )
            {
                return desc;
            }
        }
        return null;
    }

    public void removeJobDescription( Integer id ) throws IOException
    {
        Iterator<BackupJobDescription> it = descs.iterator();
        while ( it.hasNext() )
        {
            if ( it.next().getId() == id )
            {
                it.remove();
                persist();
                break;
            }
        }
    }

    public Object serialize( boolean includeLog )
    {
        Map<String, Object> configMap = new HashMap<String, Object>();
        ArrayList<Object> jobList = new ArrayList<Object>();

        for ( BackupJobDescription desc : descs )
        {
            jobList.add( new BackupJobDescriptionRepresentation( desc ).serialize( includeLog ) );
        }

        configMap.put( JOB_LIST_KEY, jobList );
        return configMap;
    }

    public Object serialize()
    {
        return serialize( true );
    }

    //
    // INTERNALS
    //

    private synchronized void persist() throws IOException
    {
        FileOutputStream configOut = new FileOutputStream( configFile );
        configOut.write( createJsonFrom( serialize( false ) ).getBytes() );
        configOut.close();

    }

    @SuppressWarnings( "unchecked" )
    private synchronized void load() throws IOException
    {
        try
        {
            String raw = getFileAsString( configFile );
            if ( raw == null || raw.trim().length() == 0 )
            {
                persist();
            }
            else
            {

                Map<String, Object> configMap = jsonToMap( raw );
                descs.clear();

                boolean needsPersistence = false;

                for ( Object item : (List<Object>) configMap.get( JOB_LIST_KEY ) )
                {
                    BackupJobDescription job = BackupJobDescriptionRepresentation.deserialize( (Map<String, Object>) item );
                    if ( job.getId() == null )
                    {
                        job.setId( ++idCount );
                        needsPersistence = true;
                    }
                    else
                    {
                        if ( job.getId() > idCount )
                        {
                            idCount = job.getId();
                        }
                    }

                    descs.add( job );
                }

                if ( needsPersistence )
                {
                    persist();
                }
            }
        }
        catch ( Exception e )
        {
            e.printStackTrace();
            throw new RuntimeException( e );
        }
    }
}
