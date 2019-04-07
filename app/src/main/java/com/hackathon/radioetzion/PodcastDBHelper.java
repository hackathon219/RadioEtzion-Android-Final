package com.hackathon.radioetzion;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.cloudant.sync.documentstore.ConflictException;
import com.cloudant.sync.documentstore.DocumentBodyFactory;
import com.cloudant.sync.documentstore.DocumentException;
import com.cloudant.sync.documentstore.DocumentNotFoundException;
import com.cloudant.sync.documentstore.DocumentRevision;
import com.cloudant.sync.documentstore.DocumentStore;
import com.cloudant.sync.documentstore.DocumentStoreException;
import com.cloudant.sync.documentstore.DocumentStoreNotOpenedException;
import com.cloudant.sync.event.Subscribe;
import com.cloudant.sync.event.notifications.ReplicationCompleted;
import com.cloudant.sync.event.notifications.ReplicationErrored;
import com.cloudant.sync.query.QueryException;
import com.cloudant.sync.query.QueryResult;
import com.cloudant.sync.replication.Replicator;
import com.cloudant.sync.replication.ReplicatorBuilder;
import com.hackathon.radioetzion.models.PodcastModel;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class PodcastDBHelper extends AppCompatActivity {

    private static final String DOCUMENT_STORE_NAME = "podcast";
    private static final String DOCUMENT_STORE_DIR = "data";
    private static final String LOG_TAG = "PodcastDBHelper";
    private static final String TAG = "Log " + PodcastDBHelper.class.getSimpleName();

    private DocumentStore mDocumentStore;

    private Replicator mPushReplicator;
    private Replicator mPullReplicator;

    private static PodcastDBHelper podcastDbHelperInstance = null;

    private final Handler mHandler;
    private MainActivity mListener;
    private WeakReference<Context> mContext;


    private PodcastDBHelper(Context context){
        mContext = new WeakReference<>(context); //avoid static field leak


        File path = mContext.get().getApplicationContext().getDir(DOCUMENT_STORE_DIR, Context.MODE_PRIVATE);

        try {
            this.mDocumentStore = DocumentStore.getInstance(new File(path, DOCUMENT_STORE_NAME));
        } catch (DocumentStoreNotOpenedException e) {
            Log.e(LOG_TAG, "Unable to open DocumentStore", e);
        }

        Log.d(LOG_TAG, "Set up database at " + path.getAbsolutePath());

        // Set up the replicator objects from the app's settings.
        try {
            reloadPodcastReplicationSettings();
        } catch (URISyntaxException e) {
            Log.e(LOG_TAG, "Unable to construct remote URI from configuration", e);
        }


        // Allow us to switch code called by the ReplicationListener into
        // the main thread so the UI can update safely.
        this.mHandler = new Handler(Looper.getMainLooper());

        Log.d(LOG_TAG, "Models set up " + path.getAbsolutePath());


    }

    public static PodcastDBHelper getInstance(Context context){
        if (podcastDbHelperInstance == null){
            podcastDbHelperInstance = new PodcastDBHelper(context);
        }
        return podcastDbHelperInstance;
    }

    //
    // GETTERS AND SETTERS
    //

    /**
     * Sets the listener for replication callbacks as a weak reference.
     * @param listener {@link MainActivity} to receive callbacks.
     */
    public void setReplicationListener(MainActivity listener) {
        this.mListener = listener;
    }



    //
    // MANAGE REPLICATIONS
    //

    /**
     * <p>Stops running replications.</p>
     *
     * <p>The stop() methods stops the replications asynchronously, see the
     * replicator docs for more information.</p>
     */
    public void stopAllReplications() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.stop();
        }
        if (this.mPushReplicator != null) {
            this.mPushReplicator.stop();
        }
    }

    /**
     * <p>Starts the configured push replication.</p>
     */
    public void startPushReplication() {
        if (this.mPushReplicator != null) {
            this.mPushReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    /**
     * <p>Starts the configured pull replication.</p>
     */
    public void startPodcastPullReplication() {
        if (this.mPullReplicator != null) {
            this.mPullReplicator.start();
        } else {
            throw new RuntimeException("Push replication not set up correctly");
        }
    }

    public DocumentStore getmDocumentStore() {
        File path = mContext.get().getApplicationContext().getDir(DOCUMENT_STORE_DIR, Context.MODE_PRIVATE);
        DocumentStore documentStore = null;
        try {
            documentStore = DocumentStore.getInstance(new File(path, DOCUMENT_STORE_NAME));
        } catch (DocumentStoreNotOpenedException e) {
            e.printStackTrace();
        }

        return documentStore;
    }


    /**
     * <p>Stops running replications and reloads the replication settings from
     * the app's preferences.</p>
     */
    public void reloadPodcastReplicationSettings() throws URISyntaxException {

        // Stop running replications before reloading the replication
        // settings.
        // The stop() method instructs the replicator to stop ongoing
        // processes, and to stop making changes to the DocumentStore. Therefore,
        // we don't clear the listeners because their complete() methods
        // still need to be called once the replications have stopped
        // for the UI to be updated correctly with any changes made before
        // the replication was stopped.
        stopAllReplications();

        // Set up the new replicator objects
        URI uri = createServerURI();

        mPullReplicator = ReplicatorBuilder.pull().to(mDocumentStore).from(uri).build();
        mPushReplicator = ReplicatorBuilder.push().from(mDocumentStore).to(uri).build();

        mPushReplicator.getEventBus().register(this);
        mPullReplicator.getEventBus().register(this);

        Log.d(LOG_TAG, "Set up replicators for URI:" + uri.toString());
    }


    private URI createServerURI() throws URISyntaxException {


        String username = "87e906d3-cf6a-4687-be56-4e3698885873-bluemix";
        String dbName = "demo";
        String apiKey = "_e5o363wgph8XWReIgYhFc1RLWCsSydmHU6jo6RE20Bz";
        String apiSecret = "07812c2901de60f53d7f280a2c25df90f31b0e157f12d84cb224fbc6c01f921d";
        String host = username + ".cloudant.com";

        return new URI("https", apiKey + ":" + apiSecret, host, 443, "/" + dbName, null, null);
    }

    public URI createServerURI(String userName,String dbName,String apiKey, String apiSecret) throws URISyntaxException {
        //allow the user of the PodcastDBHelper class to attach the helper to any cloudant database without changing the source code of the helper class
        String host = userName + ".cloudant.com";
        return new URI("https", apiKey + ":" + apiSecret, host, 443, "/" + dbName, null, null);
    }


    //
    // REPLICATIONLISTENER IMPLEMENTATION
    //

    /**
     * Calls the MainActivity's replicationComplete method on the main thread,
     * as the complete() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void complete(ReplicationCompleted rc) {
        mHandler.post(() -> {
            Log.d(TAG, "complete: PodcastDBHelper mHandler post execute");
            if (mListener != null) {
                mListener.replicationPodcastComplete();
            }
        });
    }

    /**
     * Calls the MainActivity's replicationComplete method on the main thread,
     * as the error() callback will probably come from a replicator worker
     * thread.
     */
    @Subscribe
    public void error(ReplicationErrored re) {
        Log.d(LOG_TAG, "Replication error:", re.errorInfo);
        mHandler.post(() -> {

            if (mListener != null) {
                mListener.replicationPodcastError();
            }
        });
    }


    //
    // DOCUMENT CRUD
    //

    /**
     * Creates a task, assigning an ID.
     * @param podcastModel model to create
     * @return new revision of the document
     */

    public PodcastModel createPodcastDocument(PodcastModel podcastModel) {
        DocumentRevision rev = new DocumentRevision();
        rev.setBody(DocumentBodyFactory.create(podcastModel.asMap()));
        try {
            DocumentRevision created = this.mDocumentStore.database().create(rev);
            return PodcastModel.fromRevision(created);
        } catch (DocumentException | DocumentStoreException de) {
            return null;
        }
    }

    /**
     * Updates a Model document within the DocumentStore.
     * @param podcastModel model to update
     * @return the updated revision of the Model
     * @throws DocumentStoreException if there was an error updating the rev for this model
     */
    public PodcastModel updatePodcastDocument(PodcastModel podcastModel) throws DocumentStoreException {
        DocumentRevision rev = podcastModel.getRev();
        rev.setBody(DocumentBodyFactory.create(podcastModel.asMap()));
        try {
            DocumentRevision updated = this.mDocumentStore.database().update(rev);
            return PodcastModel.fromRevision(updated);
        } catch (DocumentException de) {
            return null;
        }
    }

    /**
     * Deletes a Task document within the DocumentStore.
     * @param podcastModel task to delete
     * @throws ConflictException if the task passed in has a rev which doesn't
     *      match the current rev in the DocumentStore.
     * @throws DocumentNotFoundException if the rev for this task does not exist
     * @throws DocumentStoreException if there was an error deleting the rev for this task
     */
    public void deletePodcastDocument(PodcastModel podcastModel) throws ConflictException, DocumentNotFoundException, DocumentStoreException {
        this.mDocumentStore.database().delete(podcastModel.getRev());
    }

    //---------
    public PodcastModel getPodcastObjByDocId(String docId) throws DocumentNotFoundException, DocumentStoreException {
        DocumentRevision revision = this.mDocumentStore.database().read(docId);
        return PodcastModel.fromRevision(revision);
    }

    public PodcastModel getPodcastObjByDocId(String docId, String rev) throws DocumentNotFoundException, DocumentStoreException {
        DocumentRevision revision = this.mDocumentStore.database().read(docId,rev);
        return PodcastModel.fromRevision(revision);
    }

    public List<PodcastModel> searchPodcast(HashMap<String,Object> query) throws QueryException {
        //query syntax:
        //https://github.com/cloudant/sync-android/blob/master/doc/query.md#querying-syntax
        QueryResult result = this.mDocumentStore.query().find(query);

        List<PodcastModel> results = new ArrayList<>();

        for (DocumentRevision revision : result) {
            results.add(PodcastModel.fromRevision(revision));
        }

        return results;
    }

    public List<PodcastModel> searchPodcast(String key, String value) throws QueryException {
        //search all documents for a document that has a field where {"key":"value"}
        //for example, to look up all people named david, call like so:
        // search("name","david");

        HashMap<String,Object> query = new HashMap<>();
        query.put(key,value);

        QueryResult result = this.mDocumentStore.query().find(query);

        List<PodcastModel> results = new ArrayList<>();

        for (DocumentRevision revision : result) {
            results.add(PodcastModel.fromRevision(revision));
        }

        return results;
    }


    /**
     * <p>Returns all {@code PodcastModel} documents in the DocumentStore.</p>
     */

    public List<PodcastModel> allPodcasts() throws DocumentStoreException {
        int nDocs = this.mDocumentStore.database().getDocumentCount();
        List<DocumentRevision> all = this.mDocumentStore.database().read(0, nDocs, true);
        List<PodcastModel> podcastModels = new ArrayList<>();

        // Filter all documents down to those of type Model.
        for(DocumentRevision rev : all) {
            PodcastModel m = PodcastModel.fromRevision(rev);
            if (m != null) {
                podcastModels.add(m);
            }
        }

        return podcastModels;
    }



}