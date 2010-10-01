package org.neo4j.webadmin.backup;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;

import org.junit.Before;
import org.junit.Test;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.neo4j.kernel.EmbeddedGraphDatabase;
import org.neo4j.rest.WebServerFactory;
import org.neo4j.rest.domain.DatabaseBlockedException;
import org.neo4j.rest.domain.DatabaseLocator;
import org.neo4j.webadmin.TestUtil;
import org.neo4j.webadmin.domain.BackupFailedException;
import org.neo4j.webadmin.domain.NoBackupFoundationException;

public class BackupPerformerTest
{

    private final static String POPULATED_KEY = "popkey";
    private final static String POPULATED_VALUE = "This is an uncommon string.";

    @Before
    public void destroyMainDb() throws URISyntaxException
    {
        DatabaseLocator.shutdownGraphDatabase( new URI(
                WebServerFactory.getDefaultWebServer().getBaseUri() ) );
        TestUtil.deleteTestDb();
    }

    @Test
    public void doBackupFoundationTest() throws BackupFailedException,
            NoBackupFoundationException, DatabaseBlockedException
    {

        String backupPath = "target/backup";
        TestUtil.deleteFileOrDirectory( new File( backupPath ) );

        populateDb();
        BackupPerformer.doBackupFoundation( new File( backupPath ) );

        assertDbHasBeenPopulated( backupPath );

    }

    @Test
    public void doBackupTest() throws BackupFailedException,
            NoBackupFoundationException, DatabaseBlockedException
    {

        String backupPath = "target/backup";
        TestUtil.deleteFileOrDirectory( new File( backupPath ) );

        DatabaseLocator.getGraphDatabase();
        BackupPerformer.doBackupFoundation( new File( backupPath ) );

        populateDb();

        BackupPerformer.doBackup( new File( backupPath ) );

        assertDbHasBeenPopulated( backupPath );

    }

    @Test
    public void shouldFailOnBackupWithoutFoundationTest()
            throws BackupFailedException, NoBackupFoundationException,
            DatabaseBlockedException
    {

        String backupPath = "target/backup";
        TestUtil.deleteFileOrDirectory( new File( backupPath ) );

        Exception e = null;

        try
        {
            BackupPerformer.doBackup( new File( backupPath ) );

        }
        catch ( NoBackupFoundationException noBakEx )
        {
            e = noBakEx;
        }

        assertTrue(
                "Backing up without backup foundation should throw an exception.",
                e != null );
        assertTrue(
                "Backing up without backup foundation should throw NoBackupFoundationException.",
                e instanceof NoBackupFoundationException );

    }

    private void populateDb() throws DatabaseBlockedException
    {
        GraphDatabaseService db = DatabaseLocator.getGraphDatabase();

        Transaction tx = db.beginTx();
        try
        {
            Node n = db.getNodeById( 0l );
            n.setProperty( POPULATED_KEY, POPULATED_VALUE );
            tx.success();
        }
        finally
        {
            tx.finish();
        }
    }

    private void assertDbHasBeenPopulated( String path )
    {

        GraphDatabaseService db = new EmbeddedGraphDatabase( path );

        assertEquals(
                "Database should contain property added when it was populated.",
                POPULATED_VALUE,
                (String) db.getNodeById( 0l ).getProperty( POPULATED_KEY ) );

    }

}
