package com.example.campusly.repositories;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import com.example.campusly.database.CachedPostEntity;
import com.example.campusly.database.CampuslyDatabase;
import com.example.campusly.database.DraftEntity;
import com.example.campusly.models.Post;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Everything stored in the on-phone Room database goes through here.
// Room doesn't allow database work on the main (UI) thread because it could
// freeze the screen, so every call runs on a background thread and results
// are handed back to the main thread through a callback.
public class LocalRepository {

    public interface Callback<T> {
        void onResult(T result);
    }

    private static final ExecutorService BACKGROUND = Executors.newSingleThreadExecutor();
    private static final Handler MAIN_THREAD = new Handler(Looper.getMainLooper());

    private final CampuslyDatabase db;

    public LocalRepository(Context context) {
        db = CampuslyDatabase.getInstance(context);
    }

    public void loadSavedFeed(Callback<List<Post>> callback) {
        BACKGROUND.execute(() -> {
            List<Post> posts = new ArrayList<>();
            for (CachedPostEntity entity : db.cachedPostDao().getAll()) {
                posts.add(entity.toPost());
            }
            MAIN_THREAD.post(() -> callback.onResult(posts));
        });
    }

    public void saveFeed(List<Post> posts) {
        List<CachedPostEntity> entities = new ArrayList<>();
        for (Post post : posts) {
            entities.add(CachedPostEntity.fromPost(post));
        }
        BACKGROUND.execute(() -> db.cachedPostDao().replaceAll(entities));
    }

    public void loadDraft(String userId, Callback<DraftEntity> callback) {
        BACKGROUND.execute(() -> {
            DraftEntity draft = db.draftDao().getDraft(userId);
            MAIN_THREAD.post(() -> callback.onResult(draft));
        });
    }

    public void saveDraft(String userId, String content, String category) {
        DraftEntity draft = new DraftEntity();
        draft.userId = userId;
        draft.content = content;
        draft.category = category;
        draft.updatedAtMillis = System.currentTimeMillis();
        BACKGROUND.execute(() -> db.draftDao().saveDraft(draft));
    }

    public void deleteDraft(String userId) {
        BACKGROUND.execute(() -> db.draftDao().deleteDraft(userId));
    }

    // Used on logout, so the next person on this phone can't see the saved
    // feed or draft of the previous user.
    public void clearAll(Runnable onDone) {
        BACKGROUND.execute(() -> {
            db.clearAllTables();
            MAIN_THREAD.post(onDone);
        });
    }
}