package com.example.android.pets.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class PetProvider extends ContentProvider {
    public static final String LOG_TAG=PetProvider.class.getName();
    private static final int PETS = 100;
    private static final int PET_ID = 101;
    private PetDbHelper mDbHelper;
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static{
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS,PETS);
        sUriMatcher.addURI(PetContract.CONTENT_AUTHORITY,PetContract.PATH_PETS+"/#",PET_ID);
    }


    @Override
    public boolean onCreate() {
        mDbHelper=new PetDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SQLiteDatabase database = mDbHelper.getReadableDatabase();

        // This cursor will hold the result of the query
        Cursor cursor;

        // Figure out if the URI matcher can match the URI to a specific code
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                cursor=database.query(PetContract.PetEntry.TABLE_NAME,projection,selection,selectionArgs,null,null,sortOrder);
                break;
            case PET_ID:

                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                cursor = database.query(PetContract.PetEntry.TABLE_NAME, projection, selection, selectionArgs,
                        null, null, sortOrder);
                break;
            default:
                throw new IllegalArgumentException("Cannot query unknown URI " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(),uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return PetContract.PetEntry.CONTENT_LIST_TYPE;
            case PET_ID:
                return PetContract.PetEntry.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalStateException("Unknown URI " + uri + " with match " + match);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                return insertPet(uri, values);
            default:
                throw new IllegalArgumentException("Insertion is not supported for " + uri);
        }

    }
    private Uri insertPet(Uri uri, ContentValues values) {

        // TODO: Insert a new pet into the pets database table with the given ContentValues
        String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Pet requires a name");
        }
        Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
        if (gender == null || !PetContract.PetEntry.isValidGender(gender)) {
            throw new IllegalArgumentException("Pet requires valid gender");
        }
        Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
        if (weight != null && weight < 0) {
            throw new IllegalArgumentException("Pet requires valid weight");
        }

        SQLiteDatabase db=mDbHelper.getWritableDatabase();
        long id=db.insert(PetContract.PetEntry.TABLE_NAME,null,values);
        if (id == -1) {
            Log.e(LOG_TAG, "Failed to insert row for " + uri);
            return null;
        }
        getContext().getContentResolver().notifyChange(uri,null);
        return ContentUris.withAppendedId(uri, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        SQLiteDatabase database = mDbHelper.getWritableDatabase();

        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PETS:
                int Row_delete=database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                if(Row_delete!=0){
                    getContext().getContentResolver().notifyChange(uri,null);
                }
                // Delete all rows that match the selection and selection args
                return Row_delete;
            case PET_ID:
                // Delete a single row given by the ID in the URI
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };
                int row_delete=database.delete(PetContract.PetEntry.TABLE_NAME, selection, selectionArgs);
                if(row_delete!=0){
                    getContext().getContentResolver().notifyChange(uri,null);
                }

                return row_delete;
            default:
                throw new IllegalArgumentException("Deletion is not supported for " + uri);
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        final int match=sUriMatcher.match(uri);
        switch (match){
            case PETS:
                return updatePet(uri,values,selection,selectionArgs);
            case PET_ID:
                selection = PetContract.PetEntry._ID + "=?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                // This will perform a query on the pets table where the _id equals 3 to return a
                // Cursor containing that row of the table.
                return updatePet(uri,values,selection,selectionArgs);
            default:
                throw new IllegalArgumentException("Update is not supported for " + uri);
        }
    }
    private int updatePet(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs){
        if (values.size() == 0) {
            return 0;
        }
        if(values.containsKey(PetContract.PetEntry.COLUMN_PET_NAME)){
            String name = values.getAsString(PetContract.PetEntry.COLUMN_PET_NAME);
            if (name == null) {
                throw new IllegalArgumentException("Pet requires a name");
            }
        }
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_GENDER)) {
            Integer gender = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_GENDER);
            if (gender == null || !PetContract.PetEntry.isValidGender(gender)) {
                throw new IllegalArgumentException("Pet requires valid gender");
            }
        }
        if (values.containsKey(PetContract.PetEntry.COLUMN_PET_WEIGHT)) {
            // Check that the weight is greater than or equal to 0 kg
            Integer weight = values.getAsInteger(PetContract.PetEntry.COLUMN_PET_WEIGHT);
            if (weight != null && weight < 0) {
                throw new IllegalArgumentException("Pet requires valid weight");
            }
        }

        // No need to check the breed, any value is valid (including null).

        // If there are no values to update, then don't try to update the database


        SQLiteDatabase db=mDbHelper.getWritableDatabase();
        int row_update=db.update(PetContract.PetEntry.TABLE_NAME,values,selection,selectionArgs);
        if(row_update!=0){
            getContext().getContentResolver().notifyChange(uri,null);
        }
        return row_update;

    }
}
