package xyz.jphil.arcadedb.datahelper;

import com.arcadedb.database.Database;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.index.IndexCursor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Fluent API for upserting (update or insert) documents in ArcadeDB.
 * Provides a clean interface for common CRUD patterns.
 *
 * <p>Example usage:
 * <pre>
 * Update.use(database)
 *     .select("Person")
 *     .whereEq("email", "john@example.com")
 *     .upsert()
 *     .mapValuesWith(personMap)
 *     .saveDocument();
 * </pre>
 */
public class Update {

    private final Database database;
    private String typeName;
    private final HashMap<String, Object> lookupBy = new HashMap<>();

    public Update(Database database) {
        this.database = database;
    }

    /**
     * Static factory method to create an Update instance.
     *
     * @param database the database instance
     * @return new Update instance
     */
    public static Update use(Database database) {
        return new Update(database);
    }

    /**
     * Select the document type to operate on.
     *
     * @param typeName the document type name
     * @return this Update instance
     */
    public Update select(String typeName) {
        this.typeName = typeName;
        return this;
    }

    /**
     * Add a where condition (equality check).
     *
     * @param key the field name
     * @param value the value to match
     * @return this Update instance
     */
    public Update whereEq(String key, Object value) {
        lookupBy.put(key, value);
        return this;
    }

    /**
     * Perform an upsert operation (update if exists, insert if not).
     *
     * @return Document_Update for fluent field updates
     */
    public Document_Update upsert() {
        return upsert(null);
    }

    /**
     * Perform an upsert operation with callback to know if document already existed.
     *
     * @param alreadyExists callback receiving true if document existed, false if new
     * @return Document_Update for fluent field updates
     */
    public Document_Update upsert(Consumer<Boolean> alreadyExists) {
        var mDoc = impl(alreadyExists);
        return Document_Update.updateDocument(mDoc);
    }

    /**
     * Get a document for update only if it already exists (don't create new).
     *
     * @param alreadyExists callback receiving true if document existed, false if new
     * @return Document_Update configured not to save if document is new
     */
    public Document_Update updateNewOnly(Consumer<Boolean> alreadyExists) {
        var mDoc = impl(alreadyExists);
        return new Document_Update(mDoc, true);
    }

    /**
     * Implementation of the upsert logic.
     *
     * @param alreadyExists optional callback
     * @return MutableDocument ready for updates
     */
    public MutableDocument impl(Consumer<Boolean> alreadyExists) {
        var keys = new ArrayList<String>();
        var vals = new ArrayList<Object>();

        for (var e : lookupBy.entrySet()) {
            keys.add(e.getKey());
            vals.add(e.getValue());
        }

        IndexCursor cur;
        var sz = lookupBy.size();
        assert sz > 0;

        if (sz == 1) {
            var x = lookupBy.entrySet().iterator().next();
            cur = database.lookupByKey(typeName, x.getKey(), x.getValue());
        } else {
            cur = database.lookupByKey(typeName,
                keys.toArray(__ -> new String[__]),
                vals.toArray(__ -> new Object[__]));
        }

        var alreadyExistsUpdater = (Consumer<Boolean>) (t) -> {
            try {
                if (alreadyExists != null) {
                    alreadyExists.accept(t);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        MutableDocument mDoc;
        if (cur == null || !cur.hasNext()) {
            alreadyExistsUpdater.accept(false);
            mDoc = database.newDocument(typeName);
            return mDoc;
        } else {
            alreadyExistsUpdater.accept(true);
            mDoc = cur.next().getRecord().asDocument().modify();
            if (cur.hasNext()) {
                throw new IllegalStateException("Multiple results which one to upsert?");
            }
            return mDoc;
        }
    }
}
