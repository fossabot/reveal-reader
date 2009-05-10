package com.jackcholt.reveal;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Provider to access title and category information.
 * 
 * @author jwiggins
 * 
 */
public class TitleProvider extends ContentProvider {

	private static final String mSourceURL = "http://www.thecoffeys.net/ebooks/xmlbooks.asp";
	public static final String AUTHORITY = "com.jackcholt.reveal.titles";
	public static final String TAG = "TitleProvider";
	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/titles");
	public static final String UPDATE_TYPE = "text/plain";

	/**
	 * Convenience object to encapsulate table information for a book title
	 * 
	 * @author jwiggins
	 * 
	 */
	public static final class Titles implements BaseColumns {
		public Titles() {
		}

		public static final String TABLE_NAME = "titles";
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.titles.title";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.titles.title";
		public static final String DEFAULT_SORT_ORDER = "bookname";

		public static final String _ID = "_id";
		public static final String SOURCE_ID = "id";
		public static final String FILENAME = "filename";
		public static final String BOOKNAME = "bookname";
		public static final String URL = "url";
		public static final String UPDATED = "updated";
		public static final String SIZE = "size";
		public static final String DESCRIPTION = "description";
		public static final String CATEGORY_ID = "category_id";
	}

	/**
	 * Convenience object to encapsulate table information for a category
	 * 
	 * @author jwiggins
	 * 
	 */
	public static final class Categories implements BaseColumns {
		public Categories() {
		}

		public static final String TABLE_NAME = "categories";
		public static final String CONTENT_TYPE = "vnd.android.cursor.dir/vnd.com.jackcholt.reveal.titles.category";
		public static final String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/vnd.com.jackcholt.reveal.titles.category";
		public static final String DEFAULT_SORT_ORDER = "name";

		public static final String _ID = "_id";
		public static final String NAME = "name";
		public static final String PARENT_ID = "parent_id";
	}

	/* Provider constants */
	private static final String DATABASE_NAME = "reveal_titles.db";
	private static final int DATABASE_VERSION = 2;
	/* URI constants */
	public static final int CATEGORY = 0;
	public static final int CATEGORIES = 1;
	public static final int CATEGORIES_PARENT = 2;
	public static final int TITLE = 3;
	public static final int TITLES = 4;
	public static final int TITLES_CATEGORY = 5;
	public static final int UPDATE = 6;
	public static final int UPDATE_FILE = 7;

	private static final UriMatcher sUriMatcher;
	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "titles/category/#", CATEGORY);
		sUriMatcher.addURI(AUTHORITY, "titles/category", CATEGORIES);
		sUriMatcher.addURI(AUTHORITY, "titles/categoryparent/#", CATEGORIES_PARENT);
		sUriMatcher.addURI(AUTHORITY, "titles/title/#", TITLE);
		sUriMatcher.addURI(AUTHORITY, "titles/title", TITLES);
		sUriMatcher.addURI(AUTHORITY, "titles/titlecategory/#", TITLES_CATEGORY);
		sUriMatcher.addURI(AUTHORITY, "titles/update", UPDATE);
		sUriMatcher.addURI(AUTHORITY, "titles/updatefile", UPDATE_FILE);
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {

		DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			Log.i(TAG, "Creating database " + DATABASE_NAME);

			db.execSQL("CREATE TABLE " + Titles.TABLE_NAME + " (" + Titles._ID
					+ " INTEGER PRIMARY KEY, " + Titles.SOURCE_ID + " INTEGER, " + Titles.FILENAME
					+ " TEXT DEFAULT NULL, " + Titles.BOOKNAME + " TEXT, " + Titles.URL + " TEXT, "
					+ Titles.UPDATED + " TEXT, " + Titles.SIZE + " INTEGER, " + Titles.DESCRIPTION
					+ " TEXT, " + Titles.CATEGORY_ID + " INTEGER, " + "FOREIGN KEY ("
					+ Titles.CATEGORY_ID + ") REFERENCES " + Categories.TABLE_NAME + "("
					+ Categories._ID + ")" + "); ");

			db.execSQL("CREATE INDEX " + Titles.CATEGORY_ID + " ON " + Titles.TABLE_NAME + "("
					+ Titles.CATEGORY_ID + ");");

			db.execSQL("CREATE TABLE " + Categories.TABLE_NAME + " (" + Categories._ID
					+ " INTEGER PRIMARY KEY, " + Categories.NAME + " TEXT, " + Categories.PARENT_ID
					+ " INTEGER, " + "FOREIGN KEY (" + Categories.PARENT_ID + ") REFERENCES "
					+ Categories.TABLE_NAME + "(" + Categories._ID + ")" + ");");

			db.execSQL("CREATE INDEX " + Categories.NAME + " ON " + Categories.TABLE_NAME + "("
					+ Categories.NAME + ");");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
					+ ", which will destroy all old data");

			db.execSQL("DROP TABLE IF EXISTS " + Titles.TABLE_NAME);
			db.execSQL("DROP TABLE IF EXISTS " + Categories.TABLE_NAME);

			onCreate(db);
		}
	}

	private DatabaseHelper mOpenHelper;

	/**
	 * Automatically populates the database if it is empty from a packaged xml
	 * file
	 */
	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());

		return true;
	}

	@Override
	public String getType(Uri uri) {
		Log.i(TAG, "Getting type for uri: " + uri);
		switch (sUriMatcher.match(uri)) {
		case CATEGORIES:
		case CATEGORIES_PARENT:
			return Categories.CONTENT_TYPE;

		case CATEGORY:
			return Categories.CONTENT_ITEM_TYPE;

		case TITLES:
			return Titles.CONTENT_TYPE;

		case TITLE:
			return Titles.CONTENT_ITEM_TYPE;

		case UPDATE:
			return UPDATE_TYPE;

		case UPDATE_FILE:
			return UPDATE_TYPE;

		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	/**
	 * @deprecated Deletes are not supported through this interface
	 */
	@Override
	public int delete(Uri arg0, String arg1, String[] arg2) {
		Log.i(TAG, "Deletes are unsupported by this provider");
		return 0;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) throws IllegalArgumentException, SQLException {

		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		long rowId;
		switch (sUriMatcher.match(uri)) {
		case CATEGORIES:
			if (!values.containsKey(Categories.NAME) || !values.containsKey(Categories.PARENT_ID)) {
				throw new IllegalArgumentException(
						"Missing required information while adding category: \n" + Categories.NAME
								+ ": " + values.getAsString(Categories.NAME) + ", "
								+ Categories.PARENT_ID + ": "
								+ values.getAsString(Categories.PARENT_ID));
			}

			rowId = db.insert(Categories.TABLE_NAME, Categories.NAME, values);
			if (rowId > 0) {
				Uri categoryUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(categoryUri, null);
				return categoryUri;
			}
			break;
		case TITLES:
			if (!values.containsKey(Titles.BOOKNAME) || !values.containsKey(Titles.CATEGORY_ID)
					|| !values.containsKey(Titles.URL) || !values.containsKey(Titles.SOURCE_ID)) {
				throw new IllegalArgumentException(
						"Missing required information while adding new title: \n" + Titles.BOOKNAME
								+ ": " + values.getAsString(Titles.BOOKNAME) + ", "
								+ Titles.CATEGORY_ID + ": "
								+ values.getAsString(Titles.CATEGORY_ID) + ", " + Titles.SOURCE_ID
								+ ": " + values.getAsString(Titles.SOURCE_ID) + ", " + Titles.URL
								+ ": " + values.getAsString(Titles.URL));
			}

			rowId = db.insert(Titles.TABLE_NAME, Titles.BOOKNAME, values);
			if (rowId > 0) {
				Uri titleUri = ContentUris.withAppendedId(CONTENT_URI, rowId);
				getContext().getContentResolver().notifyChange(titleUri, null);
				return titleUri;
			}
			break;
		default:
			throw new IllegalArgumentException("Unsupported URI: " + uri);
		}

		throw new SQLException("Failed to insert row into " + uri);
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
			String sortOrder) throws IllegalArgumentException {

		Log.d(TAG, "Performing query on uri: " + uri);

		String orderBy = null;
		String where = null;
		SQLiteQueryBuilder builder = new SQLiteQueryBuilder();

		switch (sUriMatcher.match(uri)) {
		case CATEGORIES:
			builder.setTables(Categories.TABLE_NAME);
			orderBy = (sortOrder == null) ? Categories.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case CATEGORY:
			builder.setTables(Categories.TABLE_NAME);
			where = Categories._ID + " = " + uri.getPathSegments().get(2);
			orderBy = (sortOrder == null) ? Categories.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case CATEGORIES_PARENT:
			builder.setTables(Categories.TABLE_NAME);
			where = Categories.PARENT_ID + " = " + uri.getPathSegments().get(2);
			Log.d(TAG, where);
			orderBy = (sortOrder == null) ? Categories.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case TITLES:
			builder.setTables(Titles.TABLE_NAME);
			orderBy = (sortOrder == null) ? Titles.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case TITLE:
			builder.setTables(Titles.TABLE_NAME);
			where = Titles._ID + " = " + uri.getPathSegments().get(2);
			orderBy = (sortOrder == null) ? Titles.DEFAULT_SORT_ORDER : sortOrder;
			break;
		case TITLES_CATEGORY:
			builder.setTables(Titles.TABLE_NAME);
			where = Titles.CATEGORY_ID + " = " + uri.getPathSegments().get(2);
			orderBy = (sortOrder == null) ? Titles.DEFAULT_SORT_ORDER : sortOrder;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
		Cursor c = builder.query(db, projection, where, null, null, null, orderBy);

		Log.d(TAG, "Query yeilded " + c.getCount() + " results");
		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		switch (sUriMatcher.match(uri)) {
		case UPDATE:
			populate(true);
			break;
		case UPDATE_FILE:
			populate(false);
			break;
		}
		return 0;
	}

	/**
	 * Used to update the website from either the embedded file or the
	 * Yanceyware books website. This will clear all titles and categories
	 * before repopulating.
	 * 
	 * @param fromWeb
	 *            Whether to populate from a web connection or the (possibly
	 *            out-dated) packaged set of titles.
	 */
	public void populate(boolean fromWeb) {
		InputSource source;

		Log.d(TAG, "Populating database from " + (fromWeb ? "web" : "file"));

		// clear the database first
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		{
			db.delete(Titles.TABLE_NAME, null, null);
			db.delete(Categories.TABLE_NAME, null, null);
			db.close();
		}

		try {
			if (fromWeb) {
				URL bookUrl = new URL(mSourceURL);
				source = new InputSource(bookUrl.openStream());
			} else {
				// source = new InputSource(getContext().getResources()
				// .openRawResource(R.raw.xmlbooks));

				// get from the internet either way...
				URL bookUrl = new URL(mSourceURL);
				source = new InputSource(bookUrl.openStream());

			}

			SAXParserFactory factory = SAXParserFactory.newInstance();
			SAXParser parser = factory.newSAXParser();

			XMLReader reader = parser.getXMLReader();

			reader.setContentHandler(new ParserHandler());

			reader.parse(source);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		}
	}

	/**
	 * XML parsing handler. This controls the sax parsing and insertion of
	 * information into the database. Currently, the xml provides any number of
	 * category tags per title which aren't really hierarchical. The best
	 * solution I could find was to simply take the top 2 and use them as parent
	 * and child categories and ignore any other categories presented.
	 * Otherwise, results are completely inconsistent.
	 * 
	 * @author jwiggins
	 * 
	 */
	private class ParserHandler extends DefaultHandler {
		private static final String mRootTag = "yanceywarebooks";
		private static final String mBookTag = "book";
		private static final String mCategoryTag = "category";

		/** We'll try not to keep creating empty objects for the table values */
		private ContentValues mCurrentValues = new ContentValues();
		private ContentValues mCategoryValues = new ContentValues();
		/** Cache the categories with their IDS as we create them */
		private HashMap<String, Integer> mCategories = new HashMap<String, Integer>();
		private String mCurrentTag;
		private int mCount;
		private String mCategory;
		private Integer mParentCategoryID;
		private Integer mCategoryID;
		private Uri mInsertUri;

		public void startElement(String namespaceURI, String tagName, String qName,
				Attributes attributes) throws SAXException {
			String lowerTag = tagName.toLowerCase();

			if (lowerTag.equals(mBookTag)) {
				mCurrentValues.clear();
			} else if (!lowerTag.equals(mRootTag)) {
				mCurrentTag = lowerTag;
			}
		}

		public void endElement(String namespaceURI, String tagName, String qName)
				throws SAXException {
			String lowerTag = tagName.toLowerCase();

			if (lowerTag.equals(mBookTag)) {
				try {
					insertNode();
				} catch (SQLException e) {
					Log.w(TAG, e.getMessage());
				} catch (IllegalArgumentException e) {
					Log.w(TAG, e.getMessage());
				}
			} else if (!lowerTag.equals(mRootTag)) {
				mCurrentTag = null;
			}
		}

		/**
		 * Only take the entries that we can place in the database
		 */
		public void characters(char inCharacters[], int start, int length) {
			if (mCurrentValues != null
					&& mCurrentTag != null
					&& (mCurrentTag.equals(Titles.BOOKNAME)
							|| mCurrentTag.equals(Titles.DESCRIPTION)
							|| mCurrentTag.equals(Titles.FILENAME)
							|| mCurrentTag.equals(Titles.SIZE)
							|| mCurrentTag.equals(Titles.UPDATED) || mCurrentTag.equals(Titles.URL)
							|| mCurrentTag.equals(Titles.SOURCE_ID)
							|| mCurrentTag.equals(mCategoryTag + "1") || mCurrentTag
							.equals(mCategoryTag + "2"))) {
				String content = mCurrentValues.getAsString(mCurrentTag);
				if (content == null) {
					content = new String(inCharacters, start, length).trim();
				} else {
					content += new String(inCharacters, start, length).trim();
				}
				content.replace("&amp;", "&");
				content.replace("&gt;", ">");
				content.replace("&lt;", "<");
				content.replace("&apos;", "'");
				mCurrentValues.put(mCurrentTag, content);
			}
		}

		/**
		 * This does all of the work of taking our parsed information and
		 * finding relationships to categories, creating missing categories and
		 * inserting titles into the database.
		 */
		private void insertNode() {
			mCount = 1;
			mCategoryValues.clear();

			mCategory = (String) mCurrentValues.get(mCategoryTag + mCount);
			mCurrentValues.remove(mCategoryTag + mCount);
			mCount++;
			/** Start at the root with id of 0 */
			mParentCategoryID = 0;
			mCategoryID = mParentCategoryID;

			while (mCount < 4) {
				if (mCategory != null && !mCategory.equals("")) {
					mCategoryValues.put(Categories.NAME, mCategory);
					mCategoryValues.put(Categories.PARENT_ID, mParentCategoryID);

					mCategoryID = mCategories.get(mCategory + mParentCategoryID);

					if (mCategoryID == null) {
						mInsertUri = insert(Uri.withAppendedPath(CONTENT_URI, "category"),
								mCategoryValues);
						mCategoryID = new Integer(mInsertUri.getPathSegments().get(1));
						mCategories.put(mCategory + mParentCategoryID, mCategoryID);
					}

					mParentCategoryID = mCategoryID;
				} else if (mCount == 3) {
					// we have some titles (standard works) that don't fit a 2
					// category setup let's try to place them in an English
					// category
					mCategoryID = mCategories.get("English" + mParentCategoryID);

					if (mCategoryID == null) {
						Log.w(TAG, "Found title without needed categories: "
								+ mCurrentValues.getAsString(Titles.BOOKNAME));
					}
				}

				// We're just cleaning out any empty or extra categories before
				// insert
				mCategory = (String) mCurrentValues.get(mCategoryTag + mCount);
				mCurrentValues.remove(mCategoryTag + mCount);
				mCount++;
			}

			mCurrentValues.put(Titles.CATEGORY_ID, mCategoryID);

			mInsertUri = insert(Uri.withAppendedPath(CONTENT_URI, "title"), mCurrentValues);
			// Log.d(TAG, "Inserted: " + mInsertUri.toString());
		}
	}
}
