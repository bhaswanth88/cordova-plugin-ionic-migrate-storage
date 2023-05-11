package com.migrate.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import java.lang.String;
import com.edwardstock.leveldb.*

// import com.github.hf.leveldb.LevelDB;
// import com.github.hf.leveldb.Iterator;
// import com.appunite.leveldb.Utils;
// import com.appunite.leveldb.WriteBatch;

import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;

import org.apache.cordova.CordovaWebView;
import java.io.File;

/**
 * Main class that is instantiated by cordova
 * Acts as a "bridge" between the SDK and the cordova layer
 * 
 * This plugin migrates WebSQL and localStorage from the old webview to the new webview
 * 
 * TODO
 * - Test if we can we remove old file:// keys?
 * - Properly handle exceptions? We have a catch-all at the moment that is dealt with in the `initialize` function
 * - migrating IndexedDB (may not be possible because of leveldb complexities)
 */
public class MigrateStorage extends CordovaPlugin {
    // Switch this value to enable debug mode
    private static final boolean DEBUG_MODE = false;

    private static final String TAG = "com.migrate.android";
    private static final String FILE_PROTOCOL = "file://";
    private static final String WEBSQL_FILE_DIR_NAME = "file__0";
    private static final String DEFAULT_PORT_NUMBER = "8080";
    private static final String CDV_SETTING_PORT_NUMBER = "WKPort";

    private String portNumber;


    private void logDebug(String message) {
        if(DEBUG_MODE) Log.d(TAG, message);
    }

    private String getLocalHostProtocolDirName() {
        return "http_localhost_" + this.portNumber;
    }

    private String getLocalHostProtocol() {
        return "http://localhost:" + this.portNumber;
    }

    private String getRootPath() {
        Context context = cordova.getActivity().getApplicationContext();
        return context.getFilesDir().getAbsolutePath().replaceAll("/files", "");
    }

    private String getWebViewRootPath() {
        return this.getRootPath() + "/app_webview";
    }

    private String getLocalStorageRootPath() {
        return this.getWebViewRootPath() + "/Local Storage";
    }

    private String getWebSQLDatabasesPath() {
        return this.getWebViewRootPath() + "/databases";
    }

    private String getWebSQLReferenceDbPath() {
        return this.getWebSQLDatabasesPath() + "/Databases.db";
    }

    /**
     * Migrate localStorage from `file://` to `http://localhost:{portNumber}`
     *
     * TODO Test if we can we remove old file:// keys?
     *
     * @throws Exception - Can throw LevelDBException
     */
    private void migrateLocalStorage() throws Exception {
        this.logDebug("migrateLocalStorage: Migrating localStorage..");

        String levelDbPath = this.getLocalStorageRootPath() + "/leveldb";
        this.logDebug("migrateLocalStorage: levelDbPath: " + levelDbPath);



        File levelDbDir = new File(levelDbPath);
        if(!levelDbDir.isDirectory() || !levelDbDir.exists()) {
            this.logDebug("migrateLocalStorage: '" + levelDbPath + "' is not a directory or was not found; Exiting");
            return;
        }

        //LevelDB db = new LevelDB(levelDbPath);

        LevelDB levelDB = LevelDB.open(levelDbPath, LevelDB.configure());
        // LevelDB levelDB  = AndroidLevelDB.open(context, LevelDB.configure())

        // levelDB.put("leveldb".getBytes(), "Is awesome!".getBytes());
        // String value = levelDB.get("leveldb".getBytes());

        // leveldb.put("magic".getBytes(), new byte[] { 0, 1, 2, 3, 4 });
        // byte[] magic = levelDB.getBytes("magic".getBytes());

        // levelDB.close(); // closing is a must!

        String localHostProtocol = this.getLocalHostProtocol();

        if(db.exists(Utils.stringToBytes("META:" + localHostProtocol))) {
            this.logDebug("migrateLocalStorage: Found 'META:" + localHostProtocol + "' key; Skipping migration");
            levelDB.close();
            return;
        }

        // Yes, there is a typo here; `newInterator` 😔
        Iterator iterator = levelDB.iterator();
        // LevelIterator iterator = db.newInterator();

        // To update in bulk!
        // SimpleWriteBatch batch = new SimpleWriteBatch(levelDB);
        


        // 🔃 Loop through the keys and replace `file://` with `http://localhost:{portNumber}`
        logDebug("migrateLocalStorage: Starting replacements;");

        for(iterator.seekToFirst(); iterator.isValid(); iterator.next()) {
            byte[]  key = iterator.key();
            byte[] value = iterator.value();
            logDebug("migrateLocalStorage: Go key:" + new String(key) + " Value:: " + new String(value) + "");            
        }

        // Commit batch to DB
       // db.write(batch);
        // batch.write();

        iterator.close();
        levelDB.close();

        this.logDebug("migrateLocalStorage: Successfully migrated localStorage..");
    }


    // /**
    //  * Migrate WebSQL from using `file://` to `http://localhost:{portNumber}`
    //  *
    //  */
    // private void migrateWebSQL() {
    //     this.logDebug("migrateWebSQL: Migrating WebSQL..");

    //     String databasesPath = this.getWebSQLDatabasesPath();
    //     String referenceDbPath = this.getWebSQLReferenceDbPath();
    //     String localHostDirName = this.getLocalHostProtocolDirName();

    //     if(!new File(referenceDbPath).exists()) {
    //         logDebug("migrateWebSQL: Databases.db was not found in path: '" + referenceDbPath + "'; Exiting..");
    //         return;
    //     }

    //     File originalWebSQLDir = new File(databasesPath + "/" + WEBSQL_FILE_DIR_NAME);
    //     File targetWebSQLDir = new File(databasesPath + "/" + localHostDirName);

    //     if(!originalWebSQLDir.exists()) {
    //         logDebug("migrateWebSQL: original DB does not exist at '" + originalWebSQLDir.getAbsolutePath() + "'; Exiting..");
    //         return;
    //     }

    //     if(targetWebSQLDir.exists()) {
    //         logDebug("migrateWebSQL: target DB already exists at '" + targetWebSQLDir.getAbsolutePath() + "'; Skipping..");
    //         return;
    //     }

    //     logDebug("migrateWebSQL: Databases.db path: '" + referenceDbPath + "';");

    //     SQLiteDatabase db = SQLiteDatabase.openDatabase(referenceDbPath, null, 0);

    //     // Update reference DB to point to `localhost:{portNumber}`
    //     db.execSQL("UPDATE Databases SET origin = ? WHERE origin = ?", new String[] { localHostDirName, WEBSQL_FILE_DIR_NAME });
        
    //     // rename `databases/file__0` dir to `databases/localhost_http_{portNumber}`
    //     boolean renamed = originalWebSQLDir.renameTo(targetWebSQLDir);

    //     if(!renamed) {
    //         logDebug("migrateWebSQL: Tried renaming '" + originalWebSQLDir.getAbsolutePath() + "' to '" + targetWebSQLDir.getAbsolutePath() + "' but failed; Exiting...");
    //         return;
    //     }
        
    //     db.close();

    //     this.logDebug("migrateWebSQL: Successfully migrated WebSQL..");
    // }


    /**
     * Sets up the plugin interface
     *
     * @param cordova - cdvInterface that contains cordova goodies
     * @param webView - the webview that we're running
     */
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        try {
            super.initialize(cordova, webView);

            this.portNumber = this.preferences.getString(CDV_SETTING_PORT_NUMBER, "");
            if(this.portNumber.isEmpty() || this.portNumber == null) this.portNumber = DEFAULT_PORT_NUMBER;

            logDebug("Starting migration;");

            this.migrateLocalStorage();
            // this.migrateWebSQL();

            logDebug("Migration completed;");
        } catch (Exception ex) {
            logDebug("Migration filed due to error: " + ex.getMessage());
        }
    }
}
