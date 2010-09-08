package org.neo4j.webadmin.backup;

import static org.neo4j.webadmin.utils.FileUtils.delTree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.onlinebackup.Backup;
import org.neo4j.onlinebackup.Neo4jBackup;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.webadmin.domain.BackupFailedException;
import org.neo4j.webadmin.domain.NoBackupFoundationException;
import org.neo4j.webadmin.properties.ServerProperties;
import org.neo4j.webadmin.utils.GraphDatabaseUtils;

public class BackupPerformer
{

    public final static String LOGICAL_LOG_REGEX = "\\.v[0-9]+$";

    public static void doBackup( File backupPath )
            throws NoBackupFoundationException, BackupFailedException
    {
        try
        {
            // Naive check to see if folder is initialized
            // I don't want to add an all-out check here, it'd be better
            // for the Neo4jBackup class to throw an exception.
            if ( backupPath.listFiles() == null
                 || backupPath.listFiles().length == 0
                 || !( new File( backupPath, "neostore" ) ).exists() )
            {
                throw new NoBackupFoundationException(
                        "No foundation in: " + backupPath.getAbsolutePath() );
            }

            // Perform backup
            EmbeddedGraphDatabase db = GraphDatabaseUtils.getLocalDatabase();

            Backup backup = Neo4jBackup.allDataSources( db,
                    backupPath.getAbsolutePath() );

            backup.doBackup();
        }
        catch ( IOException e )
        {
            throw new BackupFailedException(
                    "IOException while performing backup, see nested.", e );
        }
    }

    public static void doBackupFoundation( File backupPath )
            throws BackupFailedException
    {
        try
        {
            File mainDbPath = new File( DatabaseLocator.DB_PATH ).getAbsoluteFile();

            setupBackupFolders( backupPath );

            boolean wasRunning = GraphDatabaseUtils.isRunning();

            if ( wasRunning )
            {
                GraphDatabaseUtils.shutdownAndBlock( "Performing backup foundation, please wait." );
            }

            cpTree( mainDbPath, backupPath );

            ServerProperties.getInstance().set( "keep_logical_logs", "true" );

            if ( wasRunning )
            {
                GraphDatabaseUtils.unblock();
            }
        }
        catch ( IOException e )
        {
            throw new BackupFailedException(
                    "IOException while creating backup foundation, see nested.",
                    e );
        }
    }

    //
    // INTERNALS
    //

    /**
     * Creates any folders not existing on the backupPath, deletes any files in
     * the bottom folder.
     */
    private static void setupBackupFolders( File backupPath )
    {
        // Delete any pre-existing files in backup folder (if it is a folder)
        if ( backupPath.exists() )
        {
            if ( backupPath.isDirectory() )
            {
                delTree( backupPath );
            }

            backupPath.delete();
        }

        // Create new, empty folder
        backupPath.mkdirs();
    }

    /**
     * Copy a file system folder/file tree from one spot to another. This
     * implementation will ignore copying logical logs.
     * 
     * @param src
     * @param target
     * @throws IOException
     */
    private static void cpTree( File src, File target ) throws IOException
    {
        if ( src.isDirectory() )
        {

            if ( !target.exists() )
            {
                target.mkdir();
            }

            for ( File childFile : src.listFiles() )
            {
                // Ignore logical log files
                if ( !childFile.getName().matches( LOGICAL_LOG_REGEX ) )
                {
                    cpTree( childFile, new File( target, childFile.getName() ) );
                }
            }
        }
        else
        {
            InputStream in = new FileInputStream( src );
            OutputStream out = new FileOutputStream( target );

            byte[] buf = new byte[1024];

            int len;

            while ( ( len = in.read( buf ) ) > 0 )
            {
                out.write( buf, 0, len );
            }

            in.close();
            out.close();
        }
    }

}
