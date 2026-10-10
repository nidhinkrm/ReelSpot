package com.reelspot.app.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Long;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class SavedPlaceDao_Impl implements SavedPlaceDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<SavedPlace> __insertionAdapterOfSavedPlace;

  private final EntityDeletionOrUpdateAdapter<SavedPlace> __deletionAdapterOfSavedPlace;

  private final EntityDeletionOrUpdateAdapter<SavedPlace> __updateAdapterOfSavedPlace;

  private final SharedSQLiteStatement __preparedStmtOfDeletePlaceById;

  public SavedPlaceDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSavedPlace = new EntityInsertionAdapter<SavedPlace>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `saved_places` (`id`,`name`,`description`,`category`,`latitude`,`longitude`,`radius`,`reelUrl`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedPlace entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getCategory());
        statement.bindDouble(5, entity.getLatitude());
        statement.bindDouble(6, entity.getLongitude());
        statement.bindDouble(7, entity.getRadius());
        if (entity.getReelUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReelUrl());
        }
        statement.bindLong(9, entity.getCreatedAt());
      }
    };
    this.__deletionAdapterOfSavedPlace = new EntityDeletionOrUpdateAdapter<SavedPlace>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `saved_places` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedPlace entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfSavedPlace = new EntityDeletionOrUpdateAdapter<SavedPlace>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `saved_places` SET `id` = ?,`name` = ?,`description` = ?,`category` = ?,`latitude` = ?,`longitude` = ?,`radius` = ?,`reelUrl` = ?,`createdAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final SavedPlace entity) {
        statement.bindLong(1, entity.getId());
        statement.bindString(2, entity.getName());
        statement.bindString(3, entity.getDescription());
        statement.bindString(4, entity.getCategory());
        statement.bindDouble(5, entity.getLatitude());
        statement.bindDouble(6, entity.getLongitude());
        statement.bindDouble(7, entity.getRadius());
        if (entity.getReelUrl() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getReelUrl());
        }
        statement.bindLong(9, entity.getCreatedAt());
        statement.bindLong(10, entity.getId());
      }
    };
    this.__preparedStmtOfDeletePlaceById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM saved_places WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertPlace(final SavedPlace place, final Continuation<? super Long> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Long>() {
      @Override
      @NonNull
      public Long call() throws Exception {
        __db.beginTransaction();
        try {
          final Long _result = __insertionAdapterOfSavedPlace.insertAndReturnId(place);
          __db.setTransactionSuccessful();
          return _result;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlace(final SavedPlace place, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfSavedPlace.handle(place);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updatePlace(final SavedPlace place, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfSavedPlace.handle(place);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deletePlaceById(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeletePlaceById.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeletePlaceById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<SavedPlace>> getAllPlaces() {
    final String _sql = "SELECT * FROM saved_places ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"saved_places"}, new Callable<List<SavedPlace>>() {
      @Override
      @NonNull
      public List<SavedPlace> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "radius");
          final int _cursorIndexOfReelUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "reelUrl");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<SavedPlace> _result = new ArrayList<SavedPlace>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final SavedPlace _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadius;
            _tmpRadius = _cursor.getFloat(_cursorIndexOfRadius);
            final String _tmpReelUrl;
            if (_cursor.isNull(_cursorIndexOfReelUrl)) {
              _tmpReelUrl = null;
            } else {
              _tmpReelUrl = _cursor.getString(_cursorIndexOfReelUrl);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new SavedPlace(_tmpId,_tmpName,_tmpDescription,_tmpCategory,_tmpLatitude,_tmpLongitude,_tmpRadius,_tmpReelUrl,_tmpCreatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getPlaceById(final long id, final Continuation<? super SavedPlace> $completion) {
    final String _sql = "SELECT * FROM saved_places WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<SavedPlace>() {
      @Override
      @Nullable
      public SavedPlace call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfCategory = CursorUtil.getColumnIndexOrThrow(_cursor, "category");
          final int _cursorIndexOfLatitude = CursorUtil.getColumnIndexOrThrow(_cursor, "latitude");
          final int _cursorIndexOfLongitude = CursorUtil.getColumnIndexOrThrow(_cursor, "longitude");
          final int _cursorIndexOfRadius = CursorUtil.getColumnIndexOrThrow(_cursor, "radius");
          final int _cursorIndexOfReelUrl = CursorUtil.getColumnIndexOrThrow(_cursor, "reelUrl");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final SavedPlace _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpName;
            _tmpName = _cursor.getString(_cursorIndexOfName);
            final String _tmpDescription;
            _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            final String _tmpCategory;
            _tmpCategory = _cursor.getString(_cursorIndexOfCategory);
            final double _tmpLatitude;
            _tmpLatitude = _cursor.getDouble(_cursorIndexOfLatitude);
            final double _tmpLongitude;
            _tmpLongitude = _cursor.getDouble(_cursorIndexOfLongitude);
            final float _tmpRadius;
            _tmpRadius = _cursor.getFloat(_cursorIndexOfRadius);
            final String _tmpReelUrl;
            if (_cursor.isNull(_cursorIndexOfReelUrl)) {
              _tmpReelUrl = null;
            } else {
              _tmpReelUrl = _cursor.getString(_cursorIndexOfReelUrl);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _result = new SavedPlace(_tmpId,_tmpName,_tmpDescription,_tmpCategory,_tmpLatitude,_tmpLongitude,_tmpRadius,_tmpReelUrl,_tmpCreatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
